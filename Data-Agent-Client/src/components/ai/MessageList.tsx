import React from 'react';
import ReactMarkdown from 'react-markdown';
import { useTranslation } from 'react-i18next';
import { Bot, User } from 'lucide-react';
import { cn } from '../../lib/utils';
import { MonacoEditor } from '../editor/MonacoEditor';

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface MessageListProps {
  messages: Message[];
  messagesEndRef: React.RefObject<HTMLDivElement>;
}

export function MessageList({ messages, messagesEndRef }: MessageListProps) {
  const { t } = useTranslation();
  return (
    <div className="flex-1 overflow-y-auto p-3 space-y-4 no-scrollbar theme-bg-main">
      {messages.map((msg) => (
        <div 
          key={msg.id} 
          className={cn(
            "flex flex-col max-w-[90%]",
            msg.role === 'user' ? "ml-auto items-end" : "mr-auto items-start"
          )}
        >
          <div className="flex items-center space-x-2 mb-1 opacity-50">
            {msg.role === 'assistant' && <Bot className="w-3 h-3" />}
            <span className="text-[10px] font-medium">
              {msg.role === 'assistant' ? t('ai.bot_name') : t('ai.you')}
            </span>
            {msg.role === 'user' && <User className="w-3 h-3" />}
          </div>
          <div 
            className={cn(
              "px-3 py-2 rounded-lg text-xs shadow-sm",
              msg.role === 'user' 
                ? "bg-primary text-primary-foreground rounded-tr-none" 
                : "theme-bg-panel theme-text-primary border theme-border rounded-tl-none"
            )}
          >
            <ReactMarkdown
              components={{
                code({ node, className, children, ...props }) {
                  const match = /language-(\w+)/.exec(className || '');
                  const language = match ? match[1] : '';
                  const isInline = !match;
                  
                  if (!isInline && language === 'sql') {
                    return (
                      <div className="my-2 border theme-border rounded overflow-hidden h-[150px]">
                        <MonacoEditor
                          value={String(children).replace(/\n$/, '')}
                          language="sql"
                          readOnly={true}
                          theme="jetbrains-dark"
                        />
                      </div>
                    );
                  }
                  return (
                    <code className={cn("bg-accent/50 px-1 rounded text-[11px] font-mono", className)} {...props}>
                      {children}
                    </code>
                  );
                },
                p: ({ children }) => <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>,
                ul: ({ children }) => <ul className="list-disc pl-4 mb-2">{children}</ul>,
                ol: ({ children }) => <ol className="list-decimal pl-4 mb-2">{children}</ol>,
              }}
            >
              {msg.content}
            </ReactMarkdown>
          </div>
        </div>
      ))}
      <div ref={messagesEndRef} />
    </div>
  );
}
