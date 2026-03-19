import { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { AgentType } from './agentTypes';
import { AIAssistantProvider } from './AIAssistantContext';
import { ChatInput } from './ChatInput';
import { AIAssistantHeader } from './AIAssistantHeader';
import { AIAssistantContent } from './AIAssistantContent';
import { MemoryCandidateDock } from './MemoryCandidateDock';
import { PlanListPanel } from './PlanListPanel';
import { PermissionRuleDialog } from './permissions/PermissionRuleDialog';
import { useConversationRuntime } from '../../hooks/useConversationRuntime';
import { useAuthStore } from '../../store/authStore';
import { conversationService } from '../../services/conversation.service';
import { aiService } from '../../services/ai.service';
import { ChatPaths } from '../../constants/apiPaths';
import { DEFAULT_MODEL, FALLBACK_MODELS } from '../../constants/models';
import { SLASH_COMMAND_IDS } from './slashCommands';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { ChatContext } from '../../types/chat';
import type { ModelOption } from '../../types/ai';
import { chatMessagesToMessages } from './MessageList';
import { extractPlansFromMessages } from './blocks/exitPlanModeTypes';
import { planTabId } from './blocks/ToolRunBlock';

const DEFAULT_AGENT: AgentType = 'Agent';

export function AIAssistant() {
  const { t, i18n } = useTranslation();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [agent, setAgentState] = useState<AgentType>(DEFAULT_AGENT);
  const [modelOptions, setModelOptions] = useState<ModelOption[]>(FALLBACK_MODELS);
  const [model, setModelState] = useState<string>(DEFAULT_MODEL);
  const [chatContext, setChatContext] = useState<ChatContext>({});
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isPlanListOpen, setIsPlanListOpen] = useState(false);
  const [isPermissionOpen, setIsPermissionOpen] = useState(false);
  const [input, setInput] = useState('');

  const {
    messages,
    isLoading,
    isWaiting,
    queue,
    submitMessage,
    stop,
    removeFromQueue,
    setActiveConversation,
    loadMessages,
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
      ...(chatContext.databaseName != null && chatContext.databaseName !== '' && { databaseName: chatContext.databaseName }),
      ...(chatContext.schemaName != null && chatContext.schemaName !== '' && { schemaName: chatContext.schemaName }),
    },
    onError: (err) => {
      console.error('Stream error:', err);
    },
  });

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
    setInput('');
    userHasScrolledUpRef.current = false;
    submitMessage(messageText);
  }, [input, setInput, submitMessage]);

  const chatMessages = chatMessagesToMessages(messages);
  const persistedConversationId =
    activeConversationId != null && activeConversationId > 0 ? activeConversationId : null;

  const completedAssistantCount = messages.reduce((count, message) => {
    if (message.role !== 'assistant') return count;
    const hasDoneBlock = message.blocks?.some((block) => block.done) ?? false;
    return hasDoneBlock ? count + 1 : count;
  }, 0);
  const candidateRefreshKey = `${persistedConversationId ?? 'none'}:${isLoading ? 'loading' : completedAssistantCount}`;

  const conversationPlans = useMemo(() => extractPlansFromMessages(messages), [messages]);
  const latestPlanTabId_ = useMemo(() => {
    if (conversationPlans.length === 0) return null;
    return planTabId(conversationPlans[conversationPlans.length - 1].title);
  }, [conversationPlans]);

  const resetToDefaults = useCallback(() => {
    setAgentState(DEFAULT_AGENT);
    setModelState(DEFAULT_MODEL);
  }, []);

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
    messages,
    latestPlanTabId: latestPlanTabId_,
    onCommand: (id: string) => {
      if (id === SLASH_COMMAND_IDS.NEW) {
        resetToDefaults();
        setActiveConversation(null);
        loadMessages(null, []);
      } else if (id === SLASH_COMMAND_IDS.HISTORY) {
        setIsHistoryOpen(true);
      } else if (id === SLASH_COMMAND_IDS.PLAN) {
        setIsPlanListOpen(true);
      } else if (id === SLASH_COMMAND_IDS.PERMISSION) {
        setIsPermissionOpen(true);
      }
    },
  };

  return (
    <AIAssistantProvider value={contextValue}>
      <div className="flex flex-col h-full theme-bg-panel overflow-hidden">
        <AIAssistantHeader
          title={t(I18N_KEYS.AI.TITLE)}
          historyAriaLabel={t(I18N_KEYS.AI.HISTORY)}
          accessToken={!!accessToken}
          isHistoryOpen={isHistoryOpen}
          setIsHistoryOpen={setIsHistoryOpen}
          isSettingsOpen={isSettingsOpen}
          setIsSettingsOpen={setIsSettingsOpen}
          currentConversationId={activeConversationId}
          conversationTabs={conversationTabs}
          onSelectTab={async (id) => {
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
            } catch {
              loadMessages(id, []);
            }
          }}
          onCloseTab={(id) => {
            closeConversationTab(id);
          }}
          onSelectConversation={async (c) => {
            setActiveConversation(c.id);
            setConversationTabTitle(c.id, c.title);
            try {
              const list = await conversationService.getMessages(c.id);
              loadMessages(c.id, list);
            } catch {
              loadMessages(c.id, []);
            }
          }}
          onNewChat={() => {
            resetToDefaults();
            setActiveConversation(null);
            loadMessages(null, []);
          }}
        />

        <AIAssistantContent
          error={undefined}
          messages={chatMessages}
          messagesEndRef={messagesEndRef}
          isLoading={isLoading}
          isWaiting={isWaiting}
          queue={queue}
          onRemoveFromQueue={removeFromQueue}
        />

        <MemoryCandidateDock
          conversationId={persistedConversationId}
          refreshKey={candidateRefreshKey}
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
        <PermissionRuleDialog
          open={isPermissionOpen}
          onClose={() => setIsPermissionOpen(false)}
          conversationId={activeConversationId}
        />
      </div>
    </AIAssistantProvider>
  );
}
