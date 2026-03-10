import React, { useMemo } from 'react';
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
import { getToolType, ToolType } from '../blocks/toolTypes';
import { projectSubAgentBlocks } from './subAgentProjection';
import type { Message, TodoBoxSpec } from './types';
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
    <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-4 theme-bg-main">
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
        const subAgentProjection =
          msg.blocks && msg.blocks.length > 0
            ? projectSubAgentBlocks(msg.blocks)
            : { anchors: [], anchorByToolCallId: new Map() };
        let segments =
          msg.blocks && msg.blocks.length > 0
            ? blocksToSegments(msg.blocks, subAgentProjection.anchorByToolCallId)
            : msg.role === MessageRole.ASSISTANT && (msg.content ?? '').trim() !== ''
              ? [{ kind: SegmentKind.TEXT as const, data: msg.content ?? '' }]
              : [];

        // Hide interactive tool cards completely in historical messages
        if (!isLastMessage) {
          segments = segments.filter((seg) => {
            if (seg.kind === SegmentKind.TOOL_RUN) {
              const toolType = getToolType(seg.toolName);
              if (toolType === ToolType.ASK_USER || toolType === ToolType.WRITE_CONFIRM) {
                return false;
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

// 优化: 使用 React.memo 避免历史消息不必要的重新渲染
const MemoizedMessageListItem = React.memo(
  MessageListItem,
  (prevProps, nextProps) => {
    // 如果不是最后一条消息且内容没变，跳过重新渲染
    if (!nextProps.isLastAssistantStreaming && prevProps.msg.id === nextProps.msg.id) {
      // 检查关键属性是否变化
      const contentSame = prevProps.msg.content === nextProps.msg.content;
      const blocksSame = prevProps.msg.blocks?.length === nextProps.msg.blocks?.length;
      const segmentsSame = prevProps.segments.length === nextProps.segments.length;
      
      if (contentSame && blocksSame && segmentsSame) {
        return true; // 返回 true 表示不重新渲染
      }
    }
    
    // 最后一条流式消息需要实时更新
    return false;
  }
);
