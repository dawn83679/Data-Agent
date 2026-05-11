import { MessageList, type Message } from './MessageList';
import { MessageQueuePanel } from './MessageQueuePanel';
import { ChatErrorStrip } from './ChatErrorStrip';
import type { WaitingPromptMode } from '../../types/chat';

export interface AIAssistantContentProps {
  error?: Error;
  messages: Message[];
  messagesEndRef: React.Ref<HTMLDivElement>;
  isLoading: boolean;
  isWaiting: boolean;
  waitingPromptMode: WaitingPromptMode;
  queue: string[];
  onRemoveFromQueue: (index: number) => void;
}

export function AIAssistantContent({
  error,
  messages,
  messagesEndRef,
  isLoading,
  isWaiting,
  waitingPromptMode,
  queue,
  onRemoveFromQueue,
}: AIAssistantContentProps) {
  return (
    <div className="flex-1 min-h-0 flex flex-col bg-transparent">
      {error && <ChatErrorStrip error={error} />}
      <MessageList
        messages={messages}
        messagesEndRef={messagesEndRef}
        isLoading={isLoading}
        isWaiting={isWaiting}
        waitingPromptMode={waitingPromptMode}
      />
      <MessageQueuePanel
        queue={queue}
        onRemove={onRemoveFromQueue}
      />
    </div>
  );
}
