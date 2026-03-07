import { useState, useRef, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { AgentType } from './agentTypes';
import { AIAssistantProvider } from './AIAssistantContext';
import { ChatInput } from './ChatInput';
import { AIAssistantHeader } from './AIAssistantHeader';
import { AIAssistantContent } from './AIAssistantContent';
import { MemoryCandidateDock } from './MemoryCandidateDock';
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

export function AIAssistant() {
  const { t, i18n } = useTranslation();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [agent, setAgent] = useState<AgentType>('Agent');
  const [modelOptions, setModelOptions] = useState<ModelOption[]>(FALLBACK_MODELS);
  const [model, setModel] = useState<string>(DEFAULT_MODEL);
  const [chatContext, setChatContext] = useState<ChatContext>({});
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
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
    onResponse: (response) => {
      const headerConversationId = response.headers.get('X-Conversation-Id');
      if (!headerConversationId) return;
      const parsed = Number(headerConversationId);
      if (Number.isFinite(parsed) && parsed > 0) {
        setActiveConversation(parsed);
      }
    },
    onError: (err) => {
      console.error('Stream error:', err);
    },
  });

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
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
    const messageText = input.trim();
    setInput('');
    submitMessage(messageText);
  }, [input, setInput, submitMessage]);

  const chatMessages = chatMessagesToMessages(messages);
  // Refresh memory candidates only after the full assistant response is done
  // (isLoading=false). While streaming, freeze the key so intermediate block
  // completions don't trigger premature fetches.
  const completedAssistantCount = messages.reduce((count, message) => {
    if (message.role !== 'assistant') return count;
    const hasDoneBlock = message.blocks?.some((block) => block.done) ?? false;
    return hasDoneBlock ? count + 1 : count;
  }, 0);
  const candidateRefreshKey = `${activeConversationId ?? 'none'}:${isLoading ? 'loading' : completedAssistantCount}`;

  const contextValue = {
    input,
    setInput,
    onSend: handleSend,
    onStop: stop,
    submitMessage,
    enqueueMessage: submitMessage, // Queue is now handled internally
    isLoading,
    conversationId: activeConversationId,
    modelState: { model, setModel, modelOptions },
    agentState: { agent, setAgent },
    chatContextState: { chatContext, setChatContext },
    messages,
    onCommand: (id: string) => {
      if (id === SLASH_COMMAND_IDS.NEW) {
        setActiveConversation(null);
        loadMessages(null, []);
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
          currentConversationId={activeConversationId}
          onSelectConversation={async (id) => {
            setActiveConversation(id);
            try {
              const list = await conversationService.getMessages(id);
              loadMessages(id, list);
            } catch {
              loadMessages(id, []);
            }
          }}
          onNewChat={() => {
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
          conversationId={activeConversationId}
          refreshKey={candidateRefreshKey}
        />

        <ChatInput />
      </div>
    </AIAssistantProvider>
  );
}
