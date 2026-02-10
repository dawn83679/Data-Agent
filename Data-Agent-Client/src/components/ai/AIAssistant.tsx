import { useState, useRef, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { AgentType } from './agentTypes';
import { AIAssistantProvider } from './AIAssistantContext';
import { ChatInput } from './ChatInput';
import { AIAssistantHeader } from './AIAssistantHeader';
import { AIAssistantContent } from './AIAssistantContent';
import { useChat } from '../../hooks/useChat';
import { useMessageQueue } from '../../hooks/useMessageQueue';
import { useAuthStore } from '../../store/authStore';
import { conversationService } from '../../services/conversation.service';
import { aiService } from '../../services/ai.service';
import type { ChatContext } from '../../types/chat';
import type { ModelOption } from '../../types/ai';
import { chatMessagesToMessages } from './MessageList';

const FALLBACK_MODELS: ModelOption[] = [
  { modelName: 'qwen3-max', supportThinking: false },
  { modelName: 'qwen3-max-thinking', supportThinking: true },
];

export function AIAssistant() {
  const { t } = useTranslation();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [agent, setAgent] = useState<AgentType>('Agent');
  const [modelOptions, setModelOptions] = useState<ModelOption[]>(FALLBACK_MODELS);
  const [model, setModel] = useState('qwen3-max');
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
    stop,
    error,
    handleSubmit,
    submitMessage,
    submitToolAnswer,
  } = useChat({
    api: '/api/chat/stream',
    body: {
      model,
      ...(currentConversationId != null && { conversationId: currentConversationId }),
      ...(chatContext.connectionId != null && { connectionId: chatContext.connectionId }),
      ...(chatContext.databaseName != null && chatContext.databaseName !== '' && { databaseName: chatContext.databaseName }),
      ...(chatContext.schemaName != null && chatContext.schemaName !== '' && { schemaName: chatContext.schemaName }),
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
    if (isLoading) {
      messageQueue.addToQueue(input);
      setInput('');
      return;
    }
    handleSubmit({ preventDefault: () => {} } as unknown as React.FormEvent);
  }, [input, isLoading, handleSubmit, setInput, messageQueue.addToQueue]);

  const chatMessages = chatMessagesToMessages(messages);

  const contextValue = {
    input,
    setInput,
    onSend: handleSend,
    onStop: stop,
    submitMessage,
    submitToolAnswer,
    isLoading,
    modelState: { model, setModel, modelOptions },
    agentState: { agent, setAgent },
    chatContextState: { chatContext, setChatContext },
    onCommand: (id: string) => {
      if (id === 'new') {
        setCurrentConversationId(null);
        setMessages([]);
      }
    },
  };

  return (
    <AIAssistantProvider value={contextValue}>
      <div className="flex flex-col h-full theme-bg-panel overflow-hidden">
        <AIAssistantHeader
          title={t('ai.title')}
          historyAriaLabel={t('ai.history')}
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
          queue={messageQueue.queue}
          onRemoveFromQueue={messageQueue.removeFromQueue}
        />

        <ChatInput />
      </div>
    </AIAssistantProvider>
  );
}
