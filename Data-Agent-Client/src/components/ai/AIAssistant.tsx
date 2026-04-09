import { useState, useRef, useEffect, useCallback, useMemo, type Dispatch, type SetStateAction } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { AgentType } from './agentTypes';
import { AIAssistantProvider } from './AIAssistantContext';
import { ChatInput } from './ChatInput';
import { AIAssistantHeader } from './AIAssistantHeader';
import { AIAssistantContent } from './AIAssistantContent';
import { PlanListPanel } from './PlanListPanel';
import { ConversationHistoryPanel } from './ConversationHistoryPanel';
import { cn } from '../../lib/utils';
import { useConversationRuntime } from '../../hooks/useConversationRuntime';
import { useAuthStore } from '../../store/authStore';
import { conversationService } from '../../services/conversation.service';
import { aiService } from '../../services/ai.service';
import { ChatPaths } from '../../constants/apiPaths';
import { DEFAULT_MODEL, FALLBACK_MODELS } from '../../constants/models';
import { ROUTES } from '../../constants/routes';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { SLASH_COMMAND_IDS } from './slashCommands';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { ChatContext, ChatMessage, ChatUserMention } from '../../types/chat';
import { MessageBlockType, MessageRole } from '../../types/chat';
import type { ModelOption } from '../../types/ai';
import { findMentionTokens } from './mentionTypes';
import { chatMessagesToMessages } from './MessageList';
import { extractPlansFromMessages } from './blocks/exitPlanModeTypes';
import { planTabId } from './blocks/ToolRunBlock';
import type { Conversation } from '../../types/conversation';

const DEFAULT_AGENT: AgentType = 'Agent';
const HISTORY_SIDEBAR_LS_KEY = 'aiAssistant.historySidebar';

function filterMentionsByTokens(mentions: ChatUserMention[], tokens: string[]): ChatUserMention[] {
  if (mentions.length === 0 || tokens.length === 0) {
    return [];
  }

  const tokenSet = new Set(tokens);
  return mentions.filter((mention) => tokenSet.has(mention.token));
}

function resolveMentionsInText(text: string, mentions: ChatUserMention[]): ChatUserMention[] {
  if (text === '' || mentions.length === 0) {
    return [];
  }

  const matchedTokens = findMentionTokens(text, mentions.map((mention) => mention.token));
  if (matchedTokens.length === 0) {
    return [];
  }

  const mentionMap = new Map(mentions.map((mention) => [mention.token, mention]));
  return matchedTokens
    .map((token) => mentionMap.get(token))
    .filter((mention): mention is ChatUserMention => mention != null);
}

function stripLocalCompactMessages(list: ChatMessage[]): ChatMessage[] {
  return list.filter((message) => !message.localKind?.startsWith('compact-'));
}

export interface AIAssistantProps {
  onClosePanel?: () => void;
  /** Organization COMMON full-page AI home only; personal / org admin keep header history popover. */
  historyAsLeftSidebar?: boolean;
}

export function AIAssistant({ onClosePanel, historyAsLeftSidebar = false }: AIAssistantProps) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [agent, setAgentState] = useState<AgentType>(DEFAULT_AGENT);
  const [modelOptions, setModelOptions] = useState<ModelOption[]>(FALLBACK_MODELS);
  const [model, setModelState] = useState<string>(DEFAULT_MODEL);
  const [chatContext, setChatContext] = useState<ChatContext>({});
  const [userMentions, setUserMentionsState] = useState<ChatUserMention[]>([]);
  const userMentionsRef = useRef<ChatUserMention[]>([]);
  const [historySidebarOpen, setHistorySidebarOpen] = useState(() => {
    if (!historyAsLeftSidebar) return false;
    if (typeof localStorage === 'undefined') return true;
    try {
      return localStorage.getItem(HISTORY_SIDEBAR_LS_KEY) !== '0';
    } catch {
      return true;
    }
  });
  const [isHistoryPopoverOpen, setIsHistoryPopoverOpen] = useState(false);
  const [isPlanListOpen, setIsPlanListOpen] = useState(false);
  const [isCompacting, setIsCompacting] = useState(false);
  const [input, setInput] = useState('');

  const setUserMentions = useCallback<Dispatch<SetStateAction<ChatUserMention[]>>>((value) => {
    const next =
      typeof value === 'function'
        ? (value as (prev: ChatUserMention[]) => ChatUserMention[])(userMentionsRef.current)
        : value;
    userMentionsRef.current = next;
    setUserMentionsState(next);
  }, []);

  const {
    messages,
    activeConversationTokenCount,
    isLoading,
    isWaiting,
    queue,
    submitMessage,
    stop,
    removeFromQueue,
    setActiveConversation,
    loadMessages,
    setConversationTokenCount,
    appendLocalAssistantMessage,
    activeConversationId,
    conversationTabs,
    closeConversationTab,
    setConversationTabTitle,
    getActivePrefs,
    setActivePrefs,
  } = useConversationRuntime({
    api: ChatPaths.STREAM,
    body: {
      model,
      language: i18n.resolvedLanguage ?? i18n.language ?? 'en',
      agentType: agent,
      ...(chatContext.connectionId != null && { connectionId: chatContext.connectionId }),
      ...(chatContext.catalogName != null && chatContext.catalogName !== '' && { catalogName: chatContext.catalogName }),
      ...(chatContext.schemaName != null && chatContext.schemaName !== '' && { schemaName: chatContext.schemaName }),
    },
    onError: (err) => {
      console.error('Stream error:', err);
    },
  });

  useEffect(() => {
    const activeTokens = findMentionTokens(input, userMentionsRef.current.map((mention) => mention.token));
    setUserMentions((prev) => filterMentionsByTokens(prev, activeTokens));
  }, [input, setUserMentions]);

  useEffect(() => {
    if (!historyAsLeftSidebar) return;
    try {
      localStorage.setItem(HISTORY_SIDEBAR_LS_KEY, historySidebarOpen ? '1' : '0');
    } catch {
      /* ignore */
    }
  }, [historyAsLeftSidebar, historySidebarOpen]);

  // Restore mode/model when active conversation changes
  const prevActiveConversationIdRef = useRef<number | null | undefined>(undefined);
  useEffect(() => {
    if (prevActiveConversationIdRef.current === activeConversationId) return;
    prevActiveConversationIdRef.current = activeConversationId;

    const prefs = getActivePrefs();
    if (prefs) {
      setAgentState(prefs.agent as AgentType);
      setModelState(prefs.model);
    } else {
      setAgentState(DEFAULT_AGENT);
      setModelState(DEFAULT_MODEL);
    }
  }, [activeConversationId, getActivePrefs]);

  // Setters that also persist the choice into the active runtime
  const setAgent = useCallback((value: AgentType) => {
    setAgentState(value);
    const current = getActivePrefs() ?? { agent: DEFAULT_AGENT, model: DEFAULT_MODEL };
    setActivePrefs({ ...current, agent: value });
  }, [getActivePrefs, setActivePrefs]);

  const setModel = useCallback((value: string) => {
    setModelState(value);
    const current = getActivePrefs() ?? { agent: DEFAULT_AGENT, model: DEFAULT_MODEL };
    setActivePrefs({ ...current, model: value });
  }, [getActivePrefs, setActivePrefs]);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const chatInputAnchorRef = useRef<HTMLDivElement>(null);
  const userHasScrolledUpRef = useRef(false);
  const SCROLL_NEAR_BOTTOM_PX = 100;

  useEffect(() => {
    const anchor = messagesEndRef.current;
    const scrollEl = anchor?.parentElement;
    if (!scrollEl) return;
    const checkNearBottom = () => {
      const { scrollTop, clientHeight, scrollHeight } = scrollEl;
      const nearBottom = scrollTop + clientHeight >= scrollHeight - SCROLL_NEAR_BOTTOM_PX;
      userHasScrolledUpRef.current = !nearBottom;
    };
    scrollEl.addEventListener('scroll', checkNearBottom, { passive: true });
    return () => scrollEl.removeEventListener('scroll', checkNearBottom);
  }, []);

  useEffect(() => {
    if (userHasScrolledUpRef.current) return;
    const el = messagesEndRef.current;
    if (!el) return;
    el.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    aiService.getModels().then((list) => {
      if (list.length > 0) {
        setModelOptions(list);
        setModelState((current) => {
          const exists = list.some((m) => m.modelName === current);
          return exists ? current : list[0].modelName;
        });
      }
    });
  }, []);

  const handleSend = useCallback(() => {
    if (!input.trim()) return;
    const messageText = input.trim();
    const activeMentions = resolveMentionsInText(messageText, userMentionsRef.current);
    userHasScrolledUpRef.current = false;
    submitMessage(messageText, activeMentions.length > 0 ? { userMentions: activeMentions } : undefined);
    setInput('');
    setUserMentions([]);
  }, [input, setInput, submitMessage, setUserMentions]);

  const chatMessages = chatMessagesToMessages(messages);
  const persistedConversationId =
    activeConversationId != null && activeConversationId > 0 ? activeConversationId : null;

  const conversationPlans = useMemo(() => extractPlansFromMessages(messages), [messages]);
  const latestPlanTabId_ = useMemo(() => {
    if (conversationPlans.length === 0) return null;
    return planTabId(conversationPlans[conversationPlans.length - 1].title);
  }, [conversationPlans]);

  const resetToDefaults = useCallback(() => {
    setAgentState(DEFAULT_AGENT);
    setModelState(DEFAULT_MODEL);
  }, []);

  const handleNewChat = useCallback(() => {
    resetToDefaults();
    setActiveConversation(null);
    loadMessages(null, []);
    setConversationTokenCount(null, null);
  }, [loadMessages, resetToDefaults, setActiveConversation, setConversationTokenCount]);

  const handleSelectConversationFromHistory = useCallback(
    async (c: Conversation) => {
      setActiveConversation(c.id);
      setConversationTabTitle(c.id, c.title);
      setConversationTokenCount(c.id, c.tokenCount ?? null);
      try {
        const list = await conversationService.getMessages(c.id);
        loadMessages(c.id, list);
      } catch {
        loadMessages(c.id, []);
      }
    },
    [loadMessages, setActiveConversation, setConversationTabTitle, setConversationTokenCount]
  );

  const buildLocalAssistantMessage = useCallback((content: string): ChatMessage => ({
    id: crypto.randomUUID(),
    role: MessageRole.ASSISTANT,
    content,
    createdAt: new Date(),
  }), []);

  const buildCompactCommandMessage = useCallback((): ChatMessage => ({
    id: crypto.randomUUID(),
    role: MessageRole.USER,
    content: '/compact',
    localKind: 'compact-command',
    createdAt: new Date(),
  }), []);

  const buildCompactStatusMessage = useCallback((): ChatMessage => ({
    id: crypto.randomUUID(),
    role: MessageRole.ASSISTANT,
    content: '',
    blocks: [
      {
        type: MessageBlockType.STATUS,
        data: 'compacting',
        done: false,
      },
    ],
    localKind: 'compact-status',
    createdAt: new Date(),
  }), []);

  const buildCompactSummaryMessage = useCallback((summary: string): ChatMessage => ({
    id: crypto.randomUUID(),
    role: MessageRole.USER,
    content: summary,
    localKind: 'compact-summary',
    createdAt: new Date(),
  }), []);

  const buildCompactResultMessage = useCallback((content: string): ChatMessage => ({
    id: crypto.randomUUID(),
    role: MessageRole.ASSISTANT,
    content,
    localKind: 'compact-result',
    createdAt: new Date(),
  }), []);

  const contextValue = {
    input,
    setInput,
    onSend: handleSend,
    onStop: stop,
    submitMessage,
    enqueueMessage: submitMessage,
    isLoading,
    conversationId: persistedConversationId,
    modelState: { model, setModel, modelOptions },
    agentState: { agent, setAgent },
    chatContextState: { chatContext, setChatContext },
    mentionState: { userMentions, setUserMentions },
    messages,
    conversationTokenCount: activeConversationTokenCount,
    latestPlanTabId: latestPlanTabId_,
    onCommand: (id: string) => {
      if (id === SLASH_COMMAND_IDS.NEW) {
        resetToDefaults();
        setActiveConversation(null);
        loadMessages(null, []);
        setConversationTokenCount(null, null);
      } else if (id === SLASH_COMMAND_IDS.HISTORY) {
        if (historyAsLeftSidebar) {
          setHistorySidebarOpen(true);
        } else {
          setIsHistoryPopoverOpen(true);
        }
      } else if (id === SLASH_COMMAND_IDS.MEMORY) {
        if (historyAsLeftSidebar) setHistorySidebarOpen(false);
        setIsHistoryPopoverOpen(false);
        navigate('/memories');
      } else if (id === SLASH_COMMAND_IDS.COMPACT) {
        void (async () => {
          const targetConversationId = persistedConversationId;
          const baseMessages = stripLocalCompactMessages(messages);
          if (isLoading || isCompacting) {
            appendLocalAssistantMessage(buildLocalAssistantMessage(
              t(I18N_KEYS.AI.COMPACT.IN_PROGRESS)
            ));
            return;
          }
          if (targetConversationId == null) {
            appendLocalAssistantMessage(buildLocalAssistantMessage(
              t(I18N_KEYS.AI.COMPACT.NO_CONVERSATION)
            ));
            return;
          }

          userHasScrolledUpRef.current = false;
          loadMessages(targetConversationId, [
            ...baseMessages,
            buildCompactCommandMessage(),
            buildCompactStatusMessage(),
          ]);
          setIsCompacting(true);

          try {
            const result = await conversationService.compact(targetConversationId, { model });
            if (!result.compressed) {
              loadMessages(targetConversationId, [
                ...baseMessages,
                buildCompactCommandMessage(),
                buildCompactResultMessage(t(I18N_KEYS.AI.COMPACT.NOOP)),
              ]);
              return;
            }

            setConversationTokenCount(targetConversationId, result.tokenCountAfter ?? null);
            loadMessages(targetConversationId, [
              ...baseMessages,
              buildCompactCommandMessage(),
              ...(result.summary && result.summary.trim() !== ''
                ? [buildCompactSummaryMessage(result.summary)]
                : []),
            ]);
          } catch (error) {
            console.error('Compact request failed:', error);
            const fallback = t(I18N_KEYS.AI.COMPACT.FAILED);
            const reason = resolveErrorMessage(error, fallback);
            loadMessages(targetConversationId, [
              ...baseMessages,
              buildCompactCommandMessage(),
              buildCompactResultMessage(
                reason === fallback
                  ? fallback
                  : t(I18N_KEYS.AI.COMPACT.FAILED_WITH_REASON, { reason })
              ),
            ]);
          } finally {
            setIsCompacting(false);
          }
        })();
      } else if (id === SLASH_COMMAND_IDS.PLAN) {
        setIsPlanListOpen(true);
      } else if (id === SLASH_COMMAND_IDS.PERMISSION) {
        if (activeConversationId != null && activeConversationId > 0) {
          navigate(`${ROUTES.PERMISSIONS}?conversationId=${activeConversationId}`);
          return;
        }
        navigate(ROUTES.PERMISSIONS);
      }
    },
  };

  const headerShared = {
    title: t(I18N_KEYS.AI.TITLE),
    historyAriaLabel: t(I18N_KEYS.AI.HISTORY),
    accessToken: !!accessToken,
    currentConversationId: activeConversationId,
    conversationTabs,
    onSelectTab: async (id: number | null) => {
      if (id == null) {
        setActiveConversation(null);
        loadMessages(null, []);
        return;
      }
      setActiveConversation(id);
      if (id <= 0) return;

      const tab = conversationTabs.find((t) => t.id === id);
      if (tab && tab.messageCount > 0) return;

      try {
        const list = await conversationService.getMessages(id);
        loadMessages(id, list);
        const conversation = await conversationService.getById(id);
        setConversationTokenCount(id, conversation.tokenCount ?? null);
      } catch {
        loadMessages(id, []);
        setConversationTokenCount(id, null);
      }
    },
    onCloseTab: (id: number | null) => {
      closeConversationTab(id);
    },
    onNewChat: handleNewChat,
    onClosePanel: () => {
      setIsHistoryPopoverOpen(false);
      if (historyAsLeftSidebar) setHistorySidebarOpen(false);
      onClosePanel?.();
    },
  };

  const mainColumn = (
    <>
      {historyAsLeftSidebar ? (
        <AIAssistantHeader
          {...headerShared}
          historyMode="sidebar"
          onToggleHistorySidebar={() => setHistorySidebarOpen((v) => !v)}
        />
      ) : (
        <AIAssistantHeader
          {...headerShared}
          historyMode="popover"
          isHistoryPopoverOpen={isHistoryPopoverOpen}
          setIsHistoryPopoverOpen={setIsHistoryPopoverOpen}
          onSelectConversation={handleSelectConversationFromHistory}
        />
      )}

      <AIAssistantContent
        error={undefined}
        messages={chatMessages}
        messagesEndRef={messagesEndRef}
        isLoading={isLoading}
        isWaiting={isWaiting}
        queue={queue}
        onRemoveFromQueue={removeFromQueue}
      />

      <div ref={chatInputAnchorRef}>
        <ChatInput />
      </div>
      <PlanListPanel
        open={isPlanListOpen}
        onClose={() => setIsPlanListOpen(false)}
        plans={conversationPlans}
        anchorRef={chatInputAnchorRef}
      />
    </>
  );

  return (
    <AIAssistantProvider value={contextValue}>
      {historyAsLeftSidebar ? (
        <div className="flex h-full min-h-0 flex-row overflow-hidden bg-transparent">
          {accessToken ? (
            <aside
              className={cn(
                'flex shrink-0 flex-col overflow-hidden border-r theme-border transition-[width] duration-200 ease-out',
                historySidebarOpen ? 'w-[280px]' : 'w-0 border-transparent'
              )}
            >
              {historySidebarOpen ? (
                <ConversationHistoryPanel
                  variant="sidebar"
                  open
                  onClose={() => setHistorySidebarOpen(false)}
                  onSelectConversation={handleSelectConversationFromHistory}
                  onNewChat={handleNewChat}
                  currentConversationId={activeConversationId}
                />
              ) : null}
            </aside>
          ) : null}

          <div className="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">{mainColumn}</div>
        </div>
      ) : (
        <div className="flex h-full flex-col overflow-hidden bg-transparent">{mainColumn}</div>
      )}
    </AIAssistantProvider>
  );
}
