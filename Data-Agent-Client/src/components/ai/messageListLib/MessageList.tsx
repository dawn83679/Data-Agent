import React from 'react';
import { useTranslation } from 'react-i18next';
import { Bot } from 'lucide-react';
import { MessageRole } from '../../../types/chat';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { mergeAssistantToolPairs } from './mergeMessages';
import { blocksToSegments } from './blocksToSegments';
import { MessageListItem } from './MessageListItem';
import { segmentsHaveTodo } from './segmentTodoUtils';
import { useTodoInMessages } from './useTodoInMessages';
import { PlanningIndicator } from '../blocks';
import type { Message } from './types';
import { SegmentKind } from './types';

export type { Message } from './types';

export interface MessageListProps {
  messages: Message[];
  messagesEndRef: React.Ref<HTMLDivElement>;
  isLoading?: boolean;
  isWaiting?: boolean;
}

export function MessageList({
  messages,
  messagesEndRef,
  isLoading = false,
  isWaiting = false,
}: MessageListProps) {
  const { t } = useTranslation();
  const displayMessages = mergeAssistantToolPairs(messages);
  const {
    lastAssistantMessageIndexWithTodo,
    latestTodoItems,
    allTodoCompleted,
    todoBoxesByMessageIndex,
  } = useTodoInMessages(displayMessages);

  const lastMsg = displayMessages[displayMessages.length - 1];
  // Phase A: isWaiting=true and no assistant message yet (last is user or list empty)
  const showPlanningRow = isWaiting && (!lastMsg || lastMsg.role !== MessageRole.ASSISTANT);

  return (
    <div className="flex-1 overflow-y-auto p-3 space-y-4 no-scrollbar theme-bg-main">
      {displayMessages.map((msg, msgIndex) => {
        const isLastMessage = msgIndex === displayMessages.length - 1;
        const isLastAssistantStreaming =
          isLastMessage && msg.role === MessageRole.ASSISTANT && isLoading;
        // Filter out askUserQuestion when not streaming (history messages)
        const segments =
          msg.blocks && msg.blocks.length > 0
            ? blocksToSegments(msg.blocks, !isLastAssistantStreaming)
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
            isWaiting={isLastAssistantStreaming ? isWaiting : false}
            segments={segments}
            overrideTodoBoxes={overrideTodoBoxes}
            hideTodoInThisMessage={hasTodoSegments}
            showAllCompletedPrompt={showAllCompletedPrompt}
            latestTodoItemsForPrompt={latestTodoItems}
            isLastAssistantStreaming={isLastAssistantStreaming}
          />
        );
      })}
      {showPlanningRow && (
        <div className="flex flex-col w-full">
          <div className="flex items-center space-x-2 mb-1.5 opacity-60">
            <Bot className="w-3 h-3 shrink-0" />
            <span className="text-[10px] font-medium theme-text-secondary">
              {t(I18N_KEYS.AI.BOT_NAME)}
            </span>
          </div>
          <div className="text-xs theme-text-primary">
            <PlanningIndicator />
          </div>
        </div>
      )}
      <div ref={messagesEndRef} />
    </div>
  );
}
