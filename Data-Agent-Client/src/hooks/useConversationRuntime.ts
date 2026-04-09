import { useRef, useState, useCallback, useMemo, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { useWorkspaceStore } from '../store/workspaceStore';
import { parseSSEResponse } from '../lib/sse';
import { ensureValidAccessToken } from '../lib/authToken';
import type { ChatRequest, ChatMessage, ChatResponseBlock, DoneMetadata } from '../types/chat';
import { isContentBlockType, MessageRole } from '../types/chat';
import type { TokenPairResponse } from '../types/auth';
import {
    CHAT_STREAM_API,
    NOT_AUTHENTICATED,
    SESSION_EXPIRED_MESSAGE,
} from '../constants/chat';
import { buildChatStreamFetchHeaders } from '../lib/chatStreamHeaders';

interface QueuedMessage {
    text: string;
    bodyOverrides?: Partial<ChatRequest>;
}

export interface ConversationPrefs {
    agent: string;
    model: string;
}

const PREFS_STORAGE_KEY = 'conversation_prefs';

function loadAllPrefsFromStorage(): Record<string, ConversationPrefs> {
    try {
        const raw = localStorage.getItem(PREFS_STORAGE_KEY);
        return raw ? (JSON.parse(raw) as Record<string, ConversationPrefs>) : {};
    } catch {
        return {};
    }
}

function savePrefsToStorage(conversationId: number, prefs: ConversationPrefs): void {
    try {
        const all = loadAllPrefsFromStorage();
        all[String(conversationId)] = prefs;
        localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(all));
    } catch {
        // localStorage unavailable, silently ignore
    }
}

function readPrefsFromStorage(conversationId: number): ConversationPrefs | null {
    const all = loadAllPrefsFromStorage();
    return all[String(conversationId)] ?? null;
}

interface ConversationRuntime {
    conversationId: number | null;
    tokenCount: number | null;
    messages: ChatMessage[];
    queue: QueuedMessage[];
    isLoading: boolean;
    isWaiting: boolean;
    submitting: boolean;
    abortController: AbortController | null;
    lastStreamEventAt: number;
    waitingTimer: ReturnType<typeof setTimeout> | null;
    createdAt: number;
    lastTouchedAt: number;
    titleOverride: string | null;
    /** Persisted agent/model selection for this conversation. */
    prefs: ConversationPrefs | null;
}

interface UseConversationRuntimeOptions {
    api?: string;
    body?: Record<string, unknown>;
    onResponse?: (response: Response) => void;
    onError?: (error: Error) => void;
}

export interface ConversationTabSummary {
    /** null represents the in-progress "new chat" runtime. */
    id: number | null;
    /** First non-empty user message content (raw), used as tab title candidate. */
    titleCandidate: string | null;
    /** Optional title override (e.g. renamed conversation title from history). */
    titleOverride: string | null;
    /** Total message count in this conversation runtime. */
    messageCount: number;
    /** When this runtime was created (ms). */
    createdAt: number;
    /** Last time this runtime was interacted with (ms). */
    lastTouchedAt: number;
}

interface UseConversationRuntimeReturn {
    // Current active conversation state
    messages: ChatMessage[];
    isLoading: boolean;
    isWaiting: boolean;
    queue: string[];

    // Actions
    submitMessage: (text: string, bodyOverrides?: Partial<ChatRequest>) => Promise<void>;
    stop: () => void;
    removeFromQueue: (index: number) => void;
    setActiveConversation: (id: number | null) => void;
    loadMessages: (id: number | null, messages: ChatMessage[]) => void;
    setConversationTokenCount: (id: number | null, tokenCount: number | null) => void;
    appendLocalAssistantMessage: (message: ChatMessage, id?: number | null) => void;
    closeConversationTab: (id: number | null) => void;
    setConversationTabTitle: (id: number, title: string | null) => void;
    /** Read the prefs of the currently active conversation. */
    getActivePrefs: () => ConversationPrefs | null;
    /** Write prefs for the currently active conversation. */
    setActivePrefs: (prefs: ConversationPrefs) => void;

    // Current conversation ID
    activeConversationId: number | null;
    activeConversationTokenCount: number | null;

    // Conversation tabs (all in-memory runtimes)
    conversationTabs: ConversationTabSummary[];
}

const GAP_THRESHOLD_MS = 800;

function safeMessageId(): string {
    const c = globalThis.crypto as Crypto | undefined;

    if (c && typeof c.randomUUID === 'function') {
        return c.randomUUID();
    }

    if (c && typeof c.getRandomValues === 'function') {
        const bytes = new Uint8Array(16);
        c.getRandomValues(bytes);

        bytes[6] = (bytes[6] & 0x0f) | 0x40;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;

        const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0'));
        return [
            hex.slice(0, 4).join(''),
            hex.slice(4, 6).join(''),
            hex.slice(6, 8).join(''),
            hex.slice(8, 10).join(''),
            hex.slice(10, 16).join(''),
        ].join('-');
    }

    return `msg-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function isPersistedConversationId(id: number | null): id is number {
    return Number.isFinite(id) && (id as number) > 0;
}

async function refreshAccessToken(): Promise<TokenPairResponse | null> {
    const { refreshToken } = useAuthStore.getState();
    if (!refreshToken) return null;

    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        });

        if (!response.ok) return null;
        const data = await response.json();
        return data.data || data;
    } catch {
        return null;
    }
}

async function fetchWithAuthRetry(
    url: string,
    body: object,
    signal: AbortSignal,
    retryCount = 0
): Promise<Response> {
    const token = await ensureValidAccessToken();
    if (!token) throw new Error(NOT_AUTHENTICATED);

    const response = await fetch(url, {
        method: 'POST',
        headers: buildChatStreamFetchHeaders(token),
        body: JSON.stringify(body),
        signal,
    });

    if (response.status === 401 && retryCount === 0) {
        const tokens = await refreshAccessToken();
        if (tokens) {
            const { user, setAuth } = useAuthStore.getState();
            setAuth(user, tokens.accessToken, tokens.refreshToken);
            return fetchWithAuthRetry(url, body, signal, 1);
        }
        const { setAuth, openLoginModal } = useAuthStore.getState();
        setAuth(null, null, null);
        openLoginModal();
        throw new Error(SESSION_EXPIRED_MESSAGE);
    }

    return response;
}

export function useConversationRuntime(
    options: UseConversationRuntimeOptions = {}
): UseConversationRuntimeReturn {
    const { api = CHAT_STREAM_API, body: baseBody = {}, onResponse, onError } = options;

    // Map of conversation ID to runtime state
    const runtimesRef = useRef<Map<string, ConversationRuntime>>(new Map());

    // Current active conversation ID
    const [activeConversationId, setActiveConversationId] = useState<number | null>(null);
    const activeConversationIdRef = useRef<number | null>(null);
    const previousActiveConversationIdRef = useRef<number | null>(null);
    const tempConversationIdRef = useRef(-1);

    useEffect(() => {
        activeConversationIdRef.current = activeConversationId;
    }, [activeConversationId]);

    // Force re-render when active conversation changes
    const [renderTick, setRenderTick] = useState(0);
    const forceUpdate = useCallback(() => setRenderTick((t) => t + 1), []);

    const closeSubAgentTabsForConversation = useCallback((conversationId: number | null) => {
        const workspace = useWorkspaceStore.getState();
        const relatedSubAgentTabs = workspace.tabs
            .filter((tab) =>
                tab.type === 'subagent-console'
                && (tab.metadata as import('../types/tab').SubAgentConsoleTabMetadata | undefined)?.conversationId === conversationId
            )
            .map((tab) => tab.id);

        relatedSubAgentTabs.forEach((tabId) => {
            workspace.closeTab(tabId);
        });
    }, []);

    const getRuntimeKey = useCallback((id: number | null): string => {
        return id === null ? '__new__' : String(id);
    }, []);

    const allocateTempConversationId = useCallback((): number => {
        const next = tempConversationIdRef.current;
        tempConversationIdRef.current -= 1;
        return next;
    }, []);

    const getOrCreateRuntime = useCallback((id: number | null): ConversationRuntime => {
        const key = getRuntimeKey(id);
        let runtime = runtimesRef.current.get(key);

        if (!runtime) {
            const now = Date.now();
            runtime = {
                conversationId: id,
                tokenCount: null,
                messages: [],
                queue: [],
                isLoading: false,
                isWaiting: false,
                submitting: false,
                abortController: null,
                lastStreamEventAt: 0,
                waitingTimer: null,
                createdAt: now,
                lastTouchedAt: now,
                titleOverride: null,
                prefs: null,
            };
            runtimesRef.current.set(key, runtime);
        }

        return runtime;
    }, [getRuntimeKey]);

    const touchRuntime = useCallback((runtime: ConversationRuntime) => {
        runtime.lastTouchedAt = Date.now();
    }, []);

    const getFirstValidUserMessage = useCallback((messages: ChatMessage[]): string | null => {
        for (const msg of messages) {
            if (msg.role !== MessageRole.USER) continue;
            const content = (msg.content ?? '').trim();
            if (content !== '') return content;
        }
        return null;
    }, []);

    const scheduleWaiting = useCallback((runtime: ConversationRuntime) => {
        if (runtime.waitingTimer) clearTimeout(runtime.waitingTimer);
        runtime.waitingTimer = setTimeout(() => {
            runtime.isWaiting = true;
            forceUpdate();
        }, GAP_THRESHOLD_MS);
    }, [forceUpdate]);

    const cancelWaiting = useCallback((runtime: ConversationRuntime) => {
        if (runtime.waitingTimer) clearTimeout(runtime.waitingTimer);
        runtime.waitingTimer = null;
        runtime.isWaiting = false;
    }, []);

    const updateRuntimeMessages = useCallback((runtime: ConversationRuntime, messages: ChatMessage[]) => {
        runtime.messages = messages;
        // Only trigger re-render if this is the active conversation.
        // Use ref so "select conversation" + "loadMessages" in the same tick updates immediately.
        if (runtime.conversationId === activeConversationIdRef.current) {
            forceUpdate();
        }
    }, [forceUpdate]);

    const consumeStreamIntoRuntime = useCallback(
        async (
            response: Response,
            runtime: ConversationRuntime,
            onConversationIdReceived?: (id: number) => void
        ): Promise<void> => {
            let accumulatedContent = '';
            const accumulatedBlocks: ChatResponseBlock[] = [];
            let doneMetadata: DoneMetadata | undefined;
            let updateCounter = 0;
            const UPDATE_THROTTLE = 5; // 增加节流阈值，减少更新频率
            
            // 使用 RAF 批量更新，避免阻塞输入
            let rafId: number | null = null;
            let pendingUpdate = false;
            
            const scheduleUpdate = () => {
                if (pendingUpdate) return;
                pendingUpdate = true;
                
                if (rafId) cancelAnimationFrame(rafId);
                rafId = requestAnimationFrame(() => {
                    const lastMessage = runtime.messages[runtime.messages.length - 1];
                    if (lastMessage?.role === MessageRole.ASSISTANT) {
                        const updatedMessages = [...runtime.messages];
                        updatedMessages[updatedMessages.length - 1] = {
                            ...lastMessage,
                            content: accumulatedContent,
                            blocks: [...accumulatedBlocks],
                        };
                        updateRuntimeMessages(runtime, updatedMessages);
                    }
                    pendingUpdate = false;
                    rafId = null;
                });
            };

            try {
                for await (const block of parseSSEResponse(response)) {
                    const lastMessage = runtime.messages[runtime.messages.length - 1];
                    if (lastMessage?.role !== MessageRole.ASSISTANT) continue;

                    if (block.conversationId != null) {
                        onConversationIdReceived?.(block.conversationId);
                    }

                    if (isContentBlockType(block.type)) {
                        accumulatedContent += block.data ?? '';
                    }
                    // Don't add done blocks to the block list — metadata is stored in doneMetadata
                    if (!block.done) {
                        accumulatedBlocks.push(block);
                    }

                    updateCounter++;

                    // 重要 block 立即更新，普通文本使用 RAF 批量更新
                    const isImportantBlock =
                        block.done ||
                        block.type === 'THOUGHT' ||
                        block.type === 'TOOL_CALL' ||
                        block.type === 'TOOL_RESULT';
                    
                    const shouldUpdate = isImportantBlock || updateCounter >= UPDATE_THROTTLE;

                    // Parse doneBlock metadata (tool stats, token usage)
                    if (block.done && block.data) {
                        try {
                            doneMetadata = JSON.parse(block.data) as DoneMetadata;
                            if (doneMetadata.totalTokens != null && doneMetadata.totalTokens > 0) {
                                runtime.tokenCount = doneMetadata.totalTokens;
                            }
                        } catch {
                            console.warn("[SSE] doneMetadata parse failed", block.data);
                        }
                    }

                    if (shouldUpdate) {
                        updateCounter = 0;

                        if (isImportantBlock) {
                            // 重要 block 立即更新
                            if (rafId) {
                                cancelAnimationFrame(rafId);
                                rafId = null;
                            }
                            const updatedMessages = [...runtime.messages];
                            updatedMessages[updatedMessages.length - 1] = {
                                ...lastMessage,
                                content: accumulatedContent,
                                blocks: [...accumulatedBlocks],
                                ...(doneMetadata && { doneMetadata }),
                            };
                            updateRuntimeMessages(runtime, updatedMessages);
                            pendingUpdate = false;
                        } else {
                            // 普通文本使用 RAF 批量更新
                            scheduleUpdate();
                        }
                    }

                    if (block.done) {
                        break;
                    }

                    cancelWaiting(runtime);
                    scheduleWaiting(runtime);
                    runtime.lastStreamEventAt = Date.now();
                }
            } finally {
                // 清理 RAF
                if (rafId) {
                    cancelAnimationFrame(rafId);
                }
            }
        },
        [updateRuntimeMessages, cancelWaiting, scheduleWaiting]
    );

    const submitMessageToRuntime = useCallback(
        async (runtime: ConversationRuntime, text: string, bodyOverrides?: Partial<ChatRequest>) => {
            const trimmed = text.trim();
            if (!trimmed || runtime.submitting) return;

            if (runtime.conversationId === null) {
                const tempConversationId = allocateTempConversationId();
                const oldKey = getRuntimeKey(null);
                const newKey = getRuntimeKey(tempConversationId);
                if (runtimesRef.current.get(oldKey) === runtime) {
                    runtimesRef.current.delete(oldKey);
                }
                runtime.conversationId = tempConversationId;
                runtimesRef.current.set(newKey, runtime);
                touchRuntime(runtime);
                if (activeConversationIdRef.current === null) {
                    activeConversationIdRef.current = tempConversationId;
                    setActiveConversationId(tempConversationId);
                }
            }

            runtime.submitting = true;
            runtime.isLoading = true;
            runtime.isWaiting = true;

            // Add user message
            const userMessage: ChatMessage = {
                id: safeMessageId(),
                role: MessageRole.USER,
                content: trimmed,
                userMentions: bodyOverrides?.userMentions,
                createdAt: new Date(),
            };
            touchRuntime(runtime);
            updateRuntimeMessages(runtime, [...runtime.messages, userMessage]);

            // Workspace in body: ensures org context even if fetch custom headers are dropped by proxy/CDN.
            const auth = useAuthStore.getState();
            const clientWorkspace =
                auth.workspaceType === 'ORGANIZATION' && auth.workspaceOrgId != null
                    ? { clientWorkspaceType: 'ORGANIZATION' as const, clientOrgId: auth.workspaceOrgId }
                    : { clientWorkspaceType: 'PERSONAL' as const };

            // Prepare request (clientWorkspace last so stream always carries current org context)
            const request: ChatRequest = {
                message: trimmed,
                ...baseBody,
                ...bodyOverrides,
                ...clientWorkspace,
                ...(isPersistedConversationId(runtime.conversationId) && { conversationId: runtime.conversationId }),
            };

            runtime.abortController = new AbortController();

            try {
                const response = await fetchWithAuthRetry(
                    api,
                    request,
                    runtime.abortController.signal
                );

                if (!response.ok) {
                    const err = new Error(`Stream request failed: ${response.status} ${response.statusText}`);
                    onError?.(err);
                    return;
                }

                // If backend already assigned a conversationId in headers, adopt it ASAP to avoid UI flicker.
                const headerConversationId = response.headers.get('X-Conversation-Id');
                if (!isPersistedConversationId(runtime.conversationId) && headerConversationId) {
                    const parsed = Number(headerConversationId);
                    if (Number.isFinite(parsed) && parsed > 0) {
                        const previousConversationId = runtime.conversationId;
                        const oldKey = getRuntimeKey(previousConversationId);
                        const newKey = getRuntimeKey(parsed);
                        if (oldKey !== newKey) {
                            runtimesRef.current.delete(oldKey);
                        }
                        runtime.conversationId = parsed;
                        runtimesRef.current.set(newKey, runtime);
                        touchRuntime(runtime);
                        // Persist prefs that were set before the real ID was known
                        if (runtime.prefs) savePrefsToStorage(parsed, runtime.prefs);

                        // Only auto-switch if user is still on this runtime.
                        if (activeConversationIdRef.current === previousConversationId) {
                            activeConversationIdRef.current = parsed;
                            setActiveConversationId(parsed);
                        }
                    }
                }

                onResponse?.(response);

                // Add assistant message placeholder
                const assistantMessage: ChatMessage = {
                    id: safeMessageId(),
                    role: MessageRole.ASSISTANT,
                    content: '',
                    blocks: [],
                    createdAt: new Date(),
                };
                touchRuntime(runtime);
                updateRuntimeMessages(runtime, [...runtime.messages, assistantMessage]);
                runtime.lastStreamEventAt = Date.now();

                await consumeStreamIntoRuntime(response, runtime, (newConversationId) => {
                    if (!Number.isFinite(newConversationId) || newConversationId <= 0) {
                        return;
                    }

                    // Update runtime's conversation ID
                    if (!isPersistedConversationId(runtime.conversationId)) {
                        const previousConversationId = runtime.conversationId;
                        const oldKey = getRuntimeKey(previousConversationId);
                        const newKey = getRuntimeKey(newConversationId);

                        // Migrate runtime to new key
                        if (oldKey !== newKey) {
                            runtimesRef.current.delete(oldKey);
                        }
                        runtime.conversationId = newConversationId;
                        runtimesRef.current.set(newKey, runtime);
                        touchRuntime(runtime);
                        // Persist prefs that were set before the real ID was known
                        if (runtime.prefs) savePrefsToStorage(newConversationId, runtime.prefs);

                        // Update active conversation ID if this runtime is still active
                        if (activeConversationIdRef.current === previousConversationId) {
                            activeConversationIdRef.current = newConversationId;
                            setActiveConversationId(newConversationId);
                        }
                    }
                });

            } catch (err) {
                if (err instanceof Error && err.name !== 'AbortError') {
                    onError?.(err);
                }
            } finally {
                runtime.submitting = false;
                runtime.isLoading = false;
                cancelWaiting(runtime);
                forceUpdate();

                // After stream completes, drain queue
                if (runtime.queue.length > 0) {
                    const [first, ...rest] = runtime.queue;
                    runtime.queue = rest;
                    forceUpdate();

                    // Small delay to ensure UI updates
                    setTimeout(() => {
                        submitMessageToRuntime(runtime, first.text, first.bodyOverrides);
                    }, 0);
                }
            }
        },
        [api, baseBody, onResponse, onError, updateRuntimeMessages, consumeStreamIntoRuntime,
            cancelWaiting, getRuntimeKey, touchRuntime, forceUpdate, allocateTempConversationId]
    );

    const submitMessage = useCallback(
        async (text: string, bodyOverrides?: Partial<ChatRequest>) => {
            const runtime = getOrCreateRuntime(activeConversationId);

            // If already submitting, add to queue
            if (runtime.submitting) {
                runtime.queue = [...runtime.queue, { text: text.trim(), bodyOverrides }];
                forceUpdate();
                return;
            }

            await submitMessageToRuntime(runtime, text, bodyOverrides);
        },
        [activeConversationId, getOrCreateRuntime, submitMessageToRuntime, forceUpdate]
    );

    const stop = useCallback(() => {
        const runtime = getOrCreateRuntime(activeConversationId);
        runtime.abortController?.abort();
        runtime.submitting = false;
        runtime.isLoading = false;
        touchRuntime(runtime);
        cancelWaiting(runtime);
        forceUpdate();
    }, [activeConversationId, getOrCreateRuntime, cancelWaiting, touchRuntime, forceUpdate]);

    const removeFromQueue = useCallback((index: number) => {
        const runtime = getOrCreateRuntime(activeConversationId);
        runtime.queue = runtime.queue.filter((_, i) => i !== index);
        forceUpdate();
    }, [activeConversationId, getOrCreateRuntime, forceUpdate]);

    const loadMessages = useCallback(
        (id: number | null, messages: ChatMessage[]) => {
            const runtime = getOrCreateRuntime(id);
            touchRuntime(runtime);
            updateRuntimeMessages(runtime, messages);
        },
        [getOrCreateRuntime, updateRuntimeMessages, touchRuntime]
    );

    const setConversationTokenCount = useCallback((id: number | null, tokenCount: number | null) => {
        const runtime = getOrCreateRuntime(id);
        runtime.tokenCount = tokenCount;
        touchRuntime(runtime);
        if (runtime.conversationId === activeConversationIdRef.current) {
            forceUpdate();
        }
    }, [forceUpdate, getOrCreateRuntime, touchRuntime]);

    const appendLocalAssistantMessage = useCallback((message: ChatMessage, id?: number | null) => {
        const targetId = id === undefined ? activeConversationIdRef.current : id;
        const runtime = getOrCreateRuntime(targetId ?? null);
        touchRuntime(runtime);
        updateRuntimeMessages(runtime, [...runtime.messages, message]);
    }, [getOrCreateRuntime, touchRuntime, updateRuntimeMessages]);

    const setActiveConversation = useCallback((id: number | null) => {
        const current = activeConversationIdRef.current;
        if (current !== id) {
            previousActiveConversationIdRef.current = current;
            closeSubAgentTabsForConversation(current);
        }
        activeConversationIdRef.current = id;
        setActiveConversationId(id);
        const runtime = getOrCreateRuntime(id);
        touchRuntime(runtime);
        forceUpdate();
    }, [closeSubAgentTabsForConversation, getOrCreateRuntime, touchRuntime, forceUpdate]);

    const closeConversationTab = useCallback((id: number | null) => {
        const key = getRuntimeKey(id);
        const wasActive = activeConversationIdRef.current === id;

        closeSubAgentTabsForConversation(id);

        runtimesRef.current.delete(key);

        if (wasActive) {
            let nextId: number | null = null;

            // Special case: closing "new chat" should go back to previous conversation
            if (id === null) {
                const prev = previousActiveConversationIdRef.current;
                if (prev != null && runtimesRef.current.has(getRuntimeKey(prev))) {
                    nextId = prev;
                }
            }

            // Fallback: pick latest by createdAt (or null)
            if (nextId == null) {
                const remaining = Array.from(runtimesRef.current.values())
                    .map((rt) => ({ id: rt.conversationId, createdAt: rt.createdAt }))
                    .sort((a, b) => a.createdAt - b.createdAt);
                nextId = remaining.length > 0 ? remaining[remaining.length - 1]!.id : null;
            }

            activeConversationIdRef.current = nextId;
            setActiveConversationId(nextId);
        }

        forceUpdate();
    }, [closeSubAgentTabsForConversation, forceUpdate, getRuntimeKey]);

    const setConversationTabTitle = useCallback((id: number, title: string | null) => {
        const runtime = getOrCreateRuntime(id);
        runtime.titleOverride = title;
        touchRuntime(runtime);
        forceUpdate();
    }, [forceUpdate, getOrCreateRuntime, touchRuntime]);

    // Get current active runtime state
    const activeRuntime = useMemo(() => {
        return getOrCreateRuntime(activeConversationId);
    }, [activeConversationId, getOrCreateRuntime]);

    const conversationTabs = useMemo<ConversationTabSummary[]>(() => {
        const list = Array.from(runtimesRef.current.values()).map((rt) => ({
            id: rt.conversationId,
            titleCandidate: getFirstValidUserMessage(rt.messages),
            titleOverride: rt.titleOverride,
            messageCount: rt.messages.length,
            createdAt: rt.createdAt,
            lastTouchedAt: rt.lastTouchedAt,
        }));
        // Keep stable left-to-right order by "opened time"
        list.sort((a, b) => a.createdAt - b.createdAt);
        return list;
    }, [renderTick, getFirstValidUserMessage]);

    const getActivePrefs = useCallback((): ConversationPrefs | null => {
        const id = activeConversationIdRef.current;
        const runtime = getOrCreateRuntime(id);
        // Return from runtime cache first; fall back to localStorage for persisted conversations
        if (runtime.prefs) return runtime.prefs;
        if (isPersistedConversationId(id)) {
            const stored = readPrefsFromStorage(id);
            if (stored) runtime.prefs = stored; // back-fill cache
            return stored;
        }
        return null;
    }, [getOrCreateRuntime]);

    const setActivePrefs = useCallback((prefs: ConversationPrefs) => {
        const id = activeConversationIdRef.current;
        getOrCreateRuntime(id).prefs = prefs;
        // Only persist for real server-assigned conversation IDs
        if (isPersistedConversationId(id)) {
            savePrefsToStorage(id, prefs);
        }
    }, [getOrCreateRuntime]);

    return {
        messages: activeRuntime.messages,
        activeConversationTokenCount: activeRuntime.tokenCount,
        isLoading: activeRuntime.isLoading,
        isWaiting: activeRuntime.isWaiting,
        queue: activeRuntime.queue.map((item) => item.text),
        submitMessage,
        stop,
        removeFromQueue,
        setActiveConversation,
        loadMessages,
        setConversationTokenCount,
        appendLocalAssistantMessage,
        closeConversationTab,
        setConversationTabTitle,
        getActivePrefs,
        setActivePrefs,
        activeConversationId,
        conversationTabs,
    };
}
