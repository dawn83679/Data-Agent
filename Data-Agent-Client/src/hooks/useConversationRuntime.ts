import { useRef, useState, useCallback, useMemo } from 'react';
import { useAuthStore } from '../store/authStore';
import { parseSSEResponse } from '../lib/sse';
import { ensureValidAccessToken } from '../lib/authToken';
import type { ChatRequest, ChatMessage, ChatResponseBlock } from '../types/chat';
import { isContentBlockType, MessageRole } from '../types/chat';
import type { TokenPairResponse } from '../types/auth';
import {
  CHAT_STREAM_API,
  NOT_AUTHENTICATED,
  SESSION_EXPIRED_MESSAGE,
} from '../constants/chat';

interface ConversationRuntime {
  conversationId: number | null;
  messages: ChatMessage[];
  queue: string[];
  isLoading: boolean;
  isWaiting: boolean;
  submitting: boolean;
  abortController: AbortController | null;
  lastStreamEventAt: number;
  waitingTimer: ReturnType<typeof setTimeout> | null;
}

interface UseConversationRuntimeOptions {
  api?: string;
  body?: Record<string, unknown>;
  onResponse?: (response: Response) => void;
  onError?: (error: Error) => void;
}

interface UseConversationRuntimeReturn {
  // Current active conversation state
  messages: ChatMessage[];
  isLoading: boolean;
  isWaiting: boolean;
  queue: string[];
  
  // Actions
  submitMessage: (text: string) => Promise<void>;
  stop: () => void;
  removeFromQueue: (index: number) => void;
  setActiveConversation: (id: number | null) => void;
  loadMessages: (id: number | null, messages: ChatMessage[]) => void;
  
  // Current conversation ID
  activeConversationId: number | null;
}

const GAP_THRESHOLD_MS = 800;

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
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
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
  
  // Force re-render when active conversation changes
  const [, forceUpdate] = useState({});
  
  const getRuntimeKey = useCallback((id: number | null): string => {
    return id === null ? '__new__' : String(id);
  }, []);
  
  const getOrCreateRuntime = useCallback((id: number | null): ConversationRuntime => {
    const key = getRuntimeKey(id);
    let runtime = runtimesRef.current.get(key);
    
    if (!runtime) {
      runtime = {
        conversationId: id,
        messages: [],
        queue: [],
        isLoading: false,
        isWaiting: false,
        submitting: false,
        abortController: null,
        lastStreamEventAt: 0,
        waitingTimer: null,
      };
      runtimesRef.current.set(key, runtime);
    }
    
    return runtime;
  }, [getRuntimeKey]);
  
  const scheduleWaiting = useCallback((runtime: ConversationRuntime) => {
    if (runtime.waitingTimer) clearTimeout(runtime.waitingTimer);
    runtime.waitingTimer = setTimeout(() => {
      runtime.isWaiting = true;
      forceUpdate({});
    }, GAP_THRESHOLD_MS);
  }, []);
  
  const cancelWaiting = useCallback((runtime: ConversationRuntime) => {
    if (runtime.waitingTimer) clearTimeout(runtime.waitingTimer);
    runtime.waitingTimer = null;
    runtime.isWaiting = false;
  }, []);
  
  const updateRuntimeMessages = useCallback((runtime: ConversationRuntime, messages: ChatMessage[]) => {
    runtime.messages = messages;
    // Only trigger re-render if this is the active conversation
    if (runtime.conversationId === activeConversationId) {
      forceUpdate({});
    }
  }, [activeConversationId]);
  
  const consumeStreamIntoRuntime = useCallback(
    async (
      response: Response,
      runtime: ConversationRuntime,
      onConversationIdReceived?: (id: number) => void
    ): Promise<void> => {
      let accumulatedContent = '';
      const accumulatedBlocks: ChatResponseBlock[] = [];
      let updateCounter = 0;
      const UPDATE_THROTTLE = 3;

      for await (const block of parseSSEResponse(response)) {
        const lastMessage = runtime.messages[runtime.messages.length - 1];
        if (lastMessage?.role !== MessageRole.ASSISTANT) continue;

        if (block.conversationId != null) {
          onConversationIdReceived?.(block.conversationId);
        }

        if (isContentBlockType(block.type)) {
          accumulatedContent += block.data ?? '';
        }
        accumulatedBlocks.push(block);

        updateCounter++;
        
        const shouldUpdate = 
          block.done || 
          block.type === 'TOOL_CALL' || 
          block.type === 'TOOL_RESULT' ||
          updateCounter >= UPDATE_THROTTLE;

        if (shouldUpdate) {
          updateCounter = 0;
          const updatedMessages = [...runtime.messages];
          updatedMessages[updatedMessages.length - 1] = {
            ...lastMessage,
            content: accumulatedContent,
            blocks: [...accumulatedBlocks],
          };
          updateRuntimeMessages(runtime, updatedMessages);
        }

        if (block.done) {
          break;
        }
        
        cancelWaiting(runtime);
        scheduleWaiting(runtime);
        runtime.lastStreamEventAt = Date.now();
      }
    },
    [updateRuntimeMessages, cancelWaiting, scheduleWaiting]
  );
  
  const submitMessageToRuntime = useCallback(
    async (runtime: ConversationRuntime, text: string) => {
      const trimmed = text.trim();
      if (!trimmed || runtime.submitting) return;
      
      runtime.submitting = true;
      runtime.isLoading = true;
      runtime.isWaiting = true;
      
      // Add user message
      const userMessage: ChatMessage = {
        id: crypto.randomUUID(),
        role: MessageRole.USER,
        content: trimmed,
        createdAt: new Date(),
      };
      updateRuntimeMessages(runtime, [...runtime.messages, userMessage]);
      
      // Prepare request
      const request: ChatRequest = {
        message: trimmed,
        ...baseBody,
        ...(runtime.conversationId != null && { conversationId: runtime.conversationId }),
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

        onResponse?.(response);

        // Add assistant message placeholder
        const assistantMessage: ChatMessage = {
          id: crypto.randomUUID(),
          role: MessageRole.ASSISTANT,
          content: '',
          blocks: [],
          createdAt: new Date(),
        };
        updateRuntimeMessages(runtime, [...runtime.messages, assistantMessage]);
        runtime.lastStreamEventAt = Date.now();

        await consumeStreamIntoRuntime(response, runtime, (newConversationId) => {
          // Update runtime's conversation ID
          if (runtime.conversationId === null && newConversationId) {
            const oldKey = getRuntimeKey(null);
            const newKey = getRuntimeKey(newConversationId);
            
            // Migrate runtime to new key
            runtimesRef.current.delete(oldKey);
            runtime.conversationId = newConversationId;
            runtimesRef.current.set(newKey, runtime);
            
            // Update active conversation ID if this was the active one
            if (activeConversationId === null) {
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
        forceUpdate({});
        
        // After stream completes, drain queue
        if (runtime.queue.length > 0) {
          const [first, ...rest] = runtime.queue;
          runtime.queue = rest;
          forceUpdate({});
          
          // Small delay to ensure UI updates
          setTimeout(() => {
            submitMessageToRuntime(runtime, first);
          }, 0);
        }
      }
    },
    [api, baseBody, onResponse, onError, updateRuntimeMessages, consumeStreamIntoRuntime, 
     cancelWaiting, getRuntimeKey, activeConversationId]
  );
  
  const submitMessage = useCallback(
    async (text: string) => {
      const runtime = getOrCreateRuntime(activeConversationId);
      
      // If already submitting, add to queue
      if (runtime.submitting) {
        runtime.queue = [...runtime.queue, text.trim()];
        forceUpdate({});
        return;
      }
      
      await submitMessageToRuntime(runtime, text);
    },
    [activeConversationId, getOrCreateRuntime, submitMessageToRuntime]
  );
  
  const stop = useCallback(() => {
    const runtime = getOrCreateRuntime(activeConversationId);
    runtime.abortController?.abort();
    runtime.submitting = false;
    runtime.isLoading = false;
    cancelWaiting(runtime);
    forceUpdate({});
  }, [activeConversationId, getOrCreateRuntime, cancelWaiting]);
  
  const removeFromQueue = useCallback((index: number) => {
    const runtime = getOrCreateRuntime(activeConversationId);
    runtime.queue = runtime.queue.filter((_, i) => i !== index);
    forceUpdate({});
  }, [activeConversationId, getOrCreateRuntime]);
  
  const loadMessages = useCallback(
    (id: number | null, messages: ChatMessage[]) => {
      const runtime = getOrCreateRuntime(id);
      updateRuntimeMessages(runtime, messages);
    },
    [getOrCreateRuntime, updateRuntimeMessages]
  );
  
  // Get current active runtime state
  const activeRuntime = useMemo(() => {
    return getOrCreateRuntime(activeConversationId);
  }, [activeConversationId, getOrCreateRuntime]);
  
  return {
    messages: activeRuntime.messages,
    isLoading: activeRuntime.isLoading,
    isWaiting: activeRuntime.isWaiting,
    queue: activeRuntime.queue,
    submitMessage,
    stop,
    removeFromQueue,
    setActiveConversation: setActiveConversationId,
    loadMessages,
    activeConversationId,
  };
}
