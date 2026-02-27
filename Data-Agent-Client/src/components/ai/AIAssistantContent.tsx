import { MessageList, type Message } from './MessageList';
import { MessageQueuePanel } from './MessageQueuePanel';
import { ChatErrorStrip } from './ChatErrorStrip';

export interface AIAssistantContentProps {
  error?: Error;
  messages: Message[];
  messagesEndRef: React.Ref<HTMLDivElement>;
  isLoading: boolean;
  isWaiting: boolean;
  queue: string[];
  onRemoveFromQueue: (index: number) => void;
}

export function AIAssistantContent({
  error,
  messages,
  messagesEndRef,
  isLoading,
  isWaiting,
  queue,
  onRemoveFromQueue,
}: AIAssistantContentProps) {
  return (
    <>
      {error && <ChatErrorStrip error={error} />}
      <MessageList
        messages={messages}
        messagesEndRef={messagesEndRef}
        isLoading={isLoading}
        isWaiting={isWaiting}
      />
      <MessageQueuePanel
        queue={queue}
        onRemove={onRemoveFromQueue}
      />
    </>
  );
}
