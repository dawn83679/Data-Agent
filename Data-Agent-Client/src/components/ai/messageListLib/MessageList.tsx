import React from 'react';
import { MessageRole } from '../../../types/chat';
import { mergeAssistantToolPairs } from './mergeMessages';
import { blocksToSegments } from './blocksToSegments';
import { MessageBubble } from './MessageBubble';
import type { Message } from './types';

export type { Message } from './types';

export interface MessageListProps {
  messages: Message[];
  messagesEndRef: React.Ref<HTMLDivElement>;
  isLoading?: boolean;
}

export function MessageList({
  messages,
  messagesEndRef,
  isLoading = false,
}: MessageListProps) {
  const displayMessages = mergeAssistantToolPairs(messages);

  return (
    <div className="flex-1 overflow-y-auto p-3 space-y-4 no-scrollbar theme-bg-main">
      {displayMessages.map((msg, msgIndex) => {
        const isLastMessage = msgIndex === displayMessages.length - 1;
        const isLastAssistantStreaming =
          isLastMessage && msg.role === MessageRole.ASSISTANT && isLoading;
        const segments =
          msg.blocks && msg.blocks.length > 0
            ? blocksToSegments(msg.blocks)
            : [];
        return (
          <div key={msg.id} className="flex flex-col w-full">
            <MessageBubble
              message={msg}
              segments={segments}
              isLastAssistantStreaming={isLastAssistantStreaming}
            />
          </div>
        );
      })}
      <div ref={messagesEndRef} />
    </div>
  );
}
