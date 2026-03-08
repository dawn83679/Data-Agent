import { useState, useRef, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { AgentType } from './agentTypes';
import { AIAssistantProvider } from './AIAssistantContext';
import { ChatInput } from './ChatInput';
import { AIAssistantHeader } from './AIAssistantHeader';
import { AIAssistantContent } from './AIAssistantContent';
import { MemoryCandidateDock } from './MemoryCandidateDock';
import { useChat } from '../../hooks/useChat';
import { useMessageQueue } from '../../hooks/useMessageQueue';
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

export function AIAssistant() {
  const { t, i18n } = useTranslation();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [agent, setAgent] = useState<AgentType>('Agent');
  const [modelOptions, setModelOptions] = useState<ModelOption[]>(FALLBACK_MODELS);
  const [model, setModel] = useState<string>(DEFAULT_MODEL);
  const [chatContext, setChatContext] = useState<ChatContext>({});
  const [currentConversationId, setCurrentConversationId] = useState<number | null>(null);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  const messageQueue = useMessageQueue();

  const {
    messages,
    setMessages,
    input,
    setInput,
    isLoading,
    isWaiting,
    stop,
    error,
    handleSubmit,
    submitMessage,
  } = useChat({
    api: ChatPaths.STREAM,
    body: {
      model,
      language: i18n.resolvedLanguage ?? i18n.language ?? 'en',
      agentType: agent,
      ...(currentConversationId != null && { conversationId: currentConversationId }),
      ...(chatContext.connectionId != null && { connectionId: chatContext.connectionId }),
      ...(chatContext.databaseName != null && chatContext.databaseName !== '' && { databaseName: chatContext.databaseName }),
      ...(chatContext.schemaName != null && chatContext.schemaName !== '' && { schemaName: chatContext.schemaName }),
    },
    onResponse: (response) => {
      const headerConversationId = response.headers.get('X-Conversation-Id');
      if (!headerConversationId) return;
      const parsed = Number(headerConversationId);
      if (Number.isFinite(parsed) && parsed > 0) {
        setCurrentConversationId(parsed);
      }
    },
    onConversationId: (id) => setCurrentConversationId(id),
    onFinish: messageQueue.drainOnFinish,
    onError: (err) => {
      console.error('Stream error:', err);
    },
  });

  useEffect(() => {
    messageQueue.setSubmitMessage(submitMessage);
  }, [messageQueue, submitMessage]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Only auto-scroll when user is near the bottom (hasn't manually scrolled up).
  // During streaming, messages update frequently; without this check, every update
  // would force scroll-to-bottom and prevent the user from reading earlier content.
  const SCROLL_FOLLOW_THRESHOLD = 80; // px from bottom to consider "following"
  useEffect(() => {
    const el = messagesEndRef.current;
    if (!el) return;
    const scrollParent = el.parentElement;
    if (!scrollParent || scrollParent.scrollHeight <= scrollParent.clientHeight) return;
    const { scrollTop, clientHeight, scrollHeight } = scrollParent;
    const isNearBottom = scrollTop + clientHeight >= scrollHeight - SCROLL_FOLLOW_THRESHOLD;
    if (isNearBottom) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  useEffect(() => {
    aiService.getModels().then((list) => {
      if (list.length > 0) {
        setModelOptions(list);
        setModel((current) => {
          const exists = list.some((m) => m.modelName === current);
          return exists ? current : list[0].modelName;
        });
      }
    });
  }, []);

  const handleSend = useCallback(() => {
    if (!input.trim()) return;
    if (isLoading) {
      messageQueue.addToQueue(input);
      setInput('');
      return;
    }
    handleSubmit({ preventDefault: () => {} } as unknown as React.FormEvent);
  }, [input, isLoading, handleSubmit, setInput, messageQueue.addToQueue]);

  const chatMessages = chatMessagesToMessages(messages);
  // Refresh memory candidates only after the full assistant response is done
  // (isLoading=false). While streaming, freeze the key so intermediate block
  // completions don't trigger premature fetches.
  const completedAssistantCount = messages.reduce((count, message) => {
    if (message.role !== 'assistant') return count;
    const hasDoneBlock = message.blocks?.some((block) => block.done) ?? false;
    return hasDoneBlock ? count + 1 : count;
  }, 0);
  const candidateRefreshKey = `${currentConversationId ?? 'none'}:${isLoading ? 'loading' : completedAssistantCount}`;

  const contextValue = {
    input,
    setInput,
    onSend: handleSend,
    onStop: stop,
    submitMessage,
    enqueueMessage: messageQueue.addToQueue,
    isLoading,
    conversationId: currentConversationId,
    modelState: { model, setModel, modelOptions },
    agentState: { agent, setAgent },
    chatContextState: { chatContext, setChatContext },
    messages,
    onCommand: (id: string) => {
      if (id === SLASH_COMMAND_IDS.NEW) {
        setCurrentConversationId(null);
        setMessages([]);
      } else if (id === SLASH_COMMAND_IDS.HISTORY) {
        setIsHistoryOpen(true);
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
          currentConversationId={currentConversationId}
          onSelectConversation={async (id) => {
            setCurrentConversationId(id);
            try {
              const list = await conversationService.getMessages(id);
              setMessages(list);
            } catch {
              setMessages([]);
            }
          }}
          onNewChat={() => {
            setCurrentConversationId(null);
            setMessages([]);
          }}
        />

        <AIAssistantContent
          error={error}
          messages={chatMessages}
          messagesEndRef={messagesEndRef}
          isLoading={isLoading}
          isWaiting={isWaiting}
          queue={messageQueue.queue}
          onRemoveFromQueue={messageQueue.removeFromQueue}
        />

        <MemoryCandidateDock
          conversationId={currentConversationId}
          refreshKey={candidateRefreshKey}
        />

        <ChatInput />
      </div>
    </AIAssistantProvider>
  );
}
