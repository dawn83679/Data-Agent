import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Bot } from 'lucide-react';
import { MessageRole } from '../../../types/chat';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { mergeAssistantToolPairs } from './mergeMessages';
import { MessageAccumulator } from './MessageAccumulator';
import { MessageListItem } from './MessageListItem';
import { segmentsHaveTodo } from './segmentTodoUtils';
import { useTodoInMessages } from './useTodoInMessages';
import { PlanningIndicator } from '../blocks';
import { getToolType, ToolType } from '../blocks/toolTypes';
import type { Message, Segment, TodoBoxSpec } from './types';
import { isTodoCompleted } from '../blocks/todoTypes';
import { ExecuteNonSelectToolStatus, parseExecuteNonSelectToolResult } from '../blocks/executeNonSelectTypes';
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
  const displayMessages = useMemo(() => mergeAssistantToolPairs(messages), [messages]);
  const {
    lastAssistantMessageIndexWithTodo,
    latestTodoItems,
    allTodoCompleted,
  } = useTodoInMessages(displayMessages);

  const lastMsg = displayMessages[displayMessages.length - 1];
  // Phase A: isWaiting=true and no assistant message yet (last is user or list empty)
  const showPlanningRow = isWaiting && (!lastMsg || lastMsg.role !== MessageRole.ASSISTANT);

  // 优化: 只渲染最近的消息，避免长对话时 DOM 节点过多
  const VISIBLE_MESSAGE_COUNT = 50;
  const visibleMessages = useMemo(() => 
    displayMessages.length > VISIBLE_MESSAGE_COUNT 
      ? displayMessages.slice(-VISIBLE_MESSAGE_COUNT)
      : displayMessages,
    [displayMessages]
  );
  const hiddenCount = displayMessages.length - visibleMessages.length;

  return (
    <div className="flex-1 min-h-0 overflow-y-auto p-4 space-y-6 bg-[color:var(--bg-panel)]">
      {hiddenCount > 0 && (
        <div className="text-center text-xs theme-text-secondary py-2 opacity-60">
          {hiddenCount} {t('ai.messages_hidden', { defaultValue: 'earlier messages hidden' })}
        </div>
      )}
      {visibleMessages.map((msg, msgIndex) => {
        const actualIndex = hiddenCount + msgIndex;
        const isLastMessage = actualIndex === displayMessages.length - 1;
        const isLastAssistantStreaming =
          isLastMessage && msg.role === MessageRole.ASSISTANT && isLoading;
        let segments =
          msg.blocks && msg.blocks.length > 0
            ? (() => {
                const acc = new MessageAccumulator();
                msg.blocks!.forEach((b) => acc.pushBlock(b));
                return acc.getSegments();
              })()
            : msg.role === MessageRole.ASSISTANT && (msg.content ?? '').trim() !== ''
              ? [{ kind: SegmentKind.TEXT as const, data: msg.content ?? '' }]
              : [];

        // Hide interactive tool cards completely in historical messages
        if (!isLastMessage) {
          segments = segments.filter((seg) => {
            if (seg.kind === SegmentKind.TOOL_RUN) {
              const toolType = getToolType(seg.toolName);
              if (toolType === ToolType.ASK_USER) {
                return false;
              }
              if (seg.toolName === 'executeNonSelectSql') {
                const executeNonSelectPayload = parseExecuteNonSelectToolResult(seg.responseData);
                if (executeNonSelectPayload?.status === ExecuteNonSelectToolStatus.REQUIRES_CONFIRMATION) {
                  return false;
                }
              }
            }
            return true;
          });
        }

        const hasTodoSegments =
          msg.role === MessageRole.ASSISTANT && segmentsHaveTodo(segments);
        // Don't use overrideTodoBoxes since todos are shown above input
        const overrideTodoBoxes: TodoBoxSpec[] = [];
        const showAllCompletedPrompt =
          actualIndex === lastAssistantMessageIndexWithTodo &&
          allTodoCompleted &&
          latestTodoItems != null;

        // Always hide todos in messages since they're shown above input
        const hideTodoInMessage = hasTodoSegments;

        // Skip rendering entirely if the message ended up completely empty after filtering
        if (!isLastMessage && segments.length === 0 && (msg.content ?? '').trim() === '') {
          return null;
        }

        return (
          <MemoizedMessageListItem
            key={msg.id}
            msg={msg}
            msgIndex={actualIndex}
            totalCount={displayMessages.length}
            isLoading={isLoading}
            isWaiting={isLastAssistantStreaming ? isWaiting : false}
            segments={segments}
            overrideTodoBoxes={overrideTodoBoxes}
            hideTodoInThisMessage={hideTodoInMessage}
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

function getBlockSignature(msg: Message): string {
  if (!msg.blocks?.length) return '';
  return msg.blocks
    .map((block) => `${block.type ?? ''}:${block.done ? '1' : '0'}:${block.data ?? ''}`)
    .join('|');
}

function getSegmentSignature(segments: Segment[]): string {
  return segments
    .map((seg) => {
      switch (seg.kind) {
        case SegmentKind.TEXT:
        case SegmentKind.THOUGHT:
          return `${seg.kind}:${seg.data}`;
        case SegmentKind.STATUS:
          return `${seg.kind}:${seg.statusKey}`;
        case SegmentKind.TOOL_RUN:
          return [
            seg.kind,
            seg.toolName,
            seg.parametersData,
            seg.responseData,
            seg.responseError ? '1' : '0',
            seg.pending ? '1' : '0',
            seg.executionState ?? '',
            seg.toolCallId ?? '',
          ].join(':');
      }
    })
    .join('|');
}

function getTodoBoxesSignature(todoBoxes: TodoBoxSpec[]): string {
  return todoBoxes
    .map((box) => `${box.todoId}:${box.items.map((item) => `${item.title}:${item.status ?? ''}:${isTodoCompleted(item.status) ? '1' : '0'}`).join(',')}`)
    .join('|');
}

function getTodoItemsSignature(items: NonNullable<React.ComponentProps<typeof MessageListItem>['latestTodoItemsForPrompt']>): string {
  return items
    .map((item) => `${item.title}:${item.status ?? ''}:${isTodoCompleted(item.status) ? '1' : '0'}`)
    .join('|');
}

// 优化: 使用 React.memo 避免历史消息不必要的重新渲染
const MemoizedMessageListItem = React.memo(
  MessageListItem,
  (prevProps, nextProps) => {
    if (prevProps.msg.id !== nextProps.msg.id) {
      return false;
    }

    if (prevProps.isLastAssistantStreaming || nextProps.isLastAssistantStreaming) {
      return false;
    }

    if (
      prevProps.isWaiting !== nextProps.isWaiting ||
      prevProps.hideTodoInThisMessage !== nextProps.hideTodoInThisMessage ||
      prevProps.showAllCompletedPrompt !== nextProps.showAllCompletedPrompt
    ) {
      return false;
    }

    if (prevProps.msg.content !== nextProps.msg.content) {
      return false;
    }

    if (getBlockSignature(prevProps.msg) !== getBlockSignature(nextProps.msg)) {
      return false;
    }

    if (getSegmentSignature(prevProps.segments) !== getSegmentSignature(nextProps.segments)) {
      return false;
    }

    if (
      getTodoBoxesSignature(prevProps.overrideTodoBoxes) !==
      getTodoBoxesSignature(nextProps.overrideTodoBoxes)
    ) {
      return false;
    }

    const prevTodoItems = prevProps.latestTodoItemsForPrompt;
    const nextTodoItems = nextProps.latestTodoItemsForPrompt;
    if ((prevTodoItems == null) !== (nextTodoItems == null)) {
      return false;
    }
    if (
      prevTodoItems != null &&
      nextTodoItems != null &&
      getTodoItemsSignature(prevTodoItems) !== getTodoItemsSignature(nextTodoItems)
    ) {
      return false;
    }

    return true;
  }
);
