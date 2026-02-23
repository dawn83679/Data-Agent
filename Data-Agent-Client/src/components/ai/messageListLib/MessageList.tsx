import React from 'react';
import { MessageRole } from '../../../types/chat';
import { mergeAssistantToolPairs } from './mergeMessages';
import { blocksToSegments } from './blocksToSegments';
import { MessageListItem } from './MessageListItem';
import { segmentsHaveTodo } from './segmentTodoUtils';
import { useTodoInMessages } from './useTodoInMessages';
import type { Message } from './types';
import { SegmentKind } from './types';

export type { Message } from './types';

export interface MessageListProps {
  messages: Message[];
  messagesEndRef: React.Ref<HTMLDivElement>;
  isLoading?: boolean;
  showPlanning?: boolean;
}

export function MessageList({
  messages,
  messagesEndRef,
  isLoading = false,
  showPlanning = false,
}: MessageListProps) {
  const displayMessages = mergeAssistantToolPairs(messages);
  const {
    lastAssistantMessageIndexWithTodo,
    latestTodoItems,
    allTodoCompleted,
    todoBoxesByMessageIndex,
  } = useTodoInMessages(displayMessages);

  return (
    <div className="flex-1 overflow-y-auto p-3 space-y-4 no-scrollbar theme-bg-main">
      {displayMessages.map((msg, msgIndex) => {
        const isLastMessage = msgIndex === displayMessages.length - 1;
        const isLastAssistantStreaming =
          isLastMessage && msg.role === MessageRole.ASSISTANT && isLoading;
        const showPlanningIndicator = isLoading && showPlanning;
        const segments =
          msg.blocks && msg.blocks.length > 0
            ? blocksToSegments(msg.blocks)
            : msg.role === MessageRole.ASSISTANT && (msg.content ?? '').trim() !== ''
              ? [{ kind: SegmentKind.TEXT as const, data: msg.content ?? '' }]
              : [];
        const hasTodoSegments =
          msg.role === MessageRole.ASSISTANT && segmentsHaveTodo(segments);
        const overrideTodoBoxes = todoBoxesByMessageIndex[msgIndex] ?? [];
        const showAllCompletedPrompt =
          msgIndex === lastAssistantMessageIndexWithTodo &&
          allTodoCompleted &&
          latestTodoItems != null;
        return (
          <MessageListItem
            key={msg.id}
            msg={msg}
            msgIndex={msgIndex}
            totalCount={displayMessages.length}
            isLoading={isLoading}
            segments={segments}
            overrideTodoBoxes={overrideTodoBoxes}
            hideTodoInThisMessage={hasTodoSegments}
            showAllCompletedPrompt={showAllCompletedPrompt}
            latestTodoItemsForPrompt={latestTodoItems}
            isLastAssistantStreaming={isLastAssistantStreaming}
            showPlanningIndicator={showPlanningIndicator}
          />
        );
      })}
      <div ref={messagesEndRef} />
    </div>
  );
}
