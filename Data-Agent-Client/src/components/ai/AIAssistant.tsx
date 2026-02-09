import { useState, useRef, useEffect, useCallback } from 'react';
import {
  Brain,
  History,
  Settings as SettingsIcon,
  X,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { MessageList, Message } from './MessageList';
import { ChatInput, AgentType } from './ChatInput';
import { AISettings } from './AISettings';
import { ConversationHistoryPanel } from './ConversationHistoryPanel';
import { useChat } from '../../hooks/useChat';
import { useAuthStore } from '../../store/authStore';
import { conversationService } from '../../services/conversation.service';
import type { ChatMessage, ChatContext } from '../../types/chat';

export function AIAssistant() {
  const { t } = useTranslation();
  const accessToken = useAuthStore((s) => s.accessToken);

  const [agent, setAgent] = useState<AgentType>('Agent');
  const [model, setModel] = useState('Gemini 3 Pro');
  const [chatContext, setChatContext] = useState<ChatContext>({});
  const [currentConversationId, setCurrentConversationId] = useState<number | null>(null);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  const {
    messages,
    setMessages,
    input,
    setInput,
    isLoading,
    error,
    handleSubmit,
  } = useChat({
    api: '/api/chat/stream',
    body: {
      ...(currentConversationId != null && { conversationId: currentConversationId }),
      ...(chatContext.connectionId != null && { connectionId: chatContext.connectionId }),
      ...(chatContext.databaseName != null && chatContext.databaseName !== '' && { databaseName: chatContext.databaseName }),
      ...(chatContext.schemaName != null && chatContext.schemaName !== '' && { schemaName: chatContext.schemaName }),
    },
    onConversationId: (id) => setCurrentConversationId(id),
    onFinish: (message) => {
      console.log('Stream finished:', message);
    },
    onError: (err) => {
      console.error('Stream error:', err);
    },
  });

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = useCallback(() => {
    if (!input.trim() || isLoading) return;
    handleSubmit({ preventDefault: () => {} } as unknown as React.FormEvent);
  }, [input, isLoading, handleSubmit]);

  // Convert ChatMessage to Message
  const chatMessages: Message[] = messages.map((msg: ChatMessage) => ({
    id: msg.id,
    role: msg.role,
    content: msg.content,
    timestamp: msg.createdAt ?? new Date(),
    blocks: msg.blocks,
  }));

  return (
    <div className="flex flex-col h-full theme-bg-panel overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 theme-bg-panel border-b theme-border shrink-0">
        <div className="flex items-center space-x-2">
          <Brain className="w-4 h-4 text-purple-400" />
          <span className="theme-text-primary text-xs font-bold">{t('ai.title')}</span>
        </div>
        <div className="flex items-center space-x-2">
          {accessToken && (
            <div className="relative">
              <History
                className="w-3.5 h-3.5 theme-text-secondary hover:theme-text-primary cursor-pointer transition-colors"
                onClick={(e) => {
                  e.stopPropagation();
                  setIsHistoryOpen((v) => !v);
                }}
                aria-label={t('ai.history')}
              />
              {isHistoryOpen && (
                <ConversationHistoryPanel
                  open={isHistoryOpen}
                  onClose={() => setIsHistoryOpen(false)}
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
                  currentConversationId={currentConversationId}
                />
              )}
            </div>
          )}
          <div className="relative">
            <SettingsIcon
              className="w-3.5 h-3.5 theme-text-secondary hover:theme-text-primary cursor-pointer transition-colors"
              onClick={() => setIsSettingsOpen(!isSettingsOpen)}
            />
            {isSettingsOpen && (
              <AISettings onClose={() => setIsSettingsOpen(false)} />
            )}
          </div>
          <X className="w-3.5 h-3.5 theme-text-secondary hover:theme-text-primary cursor-pointer transition-colors" />
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="px-3 py-2 bg-red-500/10 border-b border-red-500/20">
          <p className="text-xs text-red-500">Error: {error.message}</p>
        </div>
      )}

      {/* Messages Area */}
      <MessageList messages={chatMessages} messagesEndRef={messagesEndRef} />

      {/* Input Area */}
      <ChatInput
        input={input}
        setInput={setInput}
        onSend={handleSend}
        agent={agent}
        setAgent={setAgent}
        model={model}
        setModel={setModel}
        chatContext={chatContext}
        setChatContext={setChatContext}
      />
    </div>
  );
}
