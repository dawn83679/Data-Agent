import { useState, useRef, useEffect } from 'react';
import { 
  Brain, 
  Settings as SettingsIcon, 
  X,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { MessageList, Message } from './MessageList';
import { ChatInput, AgentType } from './ChatInput';
import { AISettings } from './AISettings';

export function AIAssistant() {
  const { t } = useTranslation();
  
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [agent, setAgent] = useState<AgentType>('Agent');
  const [model, setModel] = useState('Gemini 3 Pro');
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = () => {
    if (!input.trim()) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInput('');
  };

  return (
    <div className="flex flex-col h-full theme-bg-panel overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 theme-bg-panel border-b theme-border shrink-0">
        <div className="flex items-center space-x-2">
          <Brain className="w-4 h-4 text-purple-400" />
          <span className="theme-text-primary text-xs font-bold">{t('ai.title')}</span>
        </div>
        <div className="flex items-center space-x-2">
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

      {/* Messages Area */}
      <MessageList messages={messages} messagesEndRef={messagesEndRef} />

      {/* Input Area */}
      <ChatInput 
        input={input}
        setInput={setInput}
        onSend={handleSend}
        agent={agent}
        setAgent={setAgent}
        model={model}
        setModel={setModel}
      />
    </div>
  );
}

