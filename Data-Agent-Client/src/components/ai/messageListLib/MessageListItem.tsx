import { MessageBubble } from './MessageBubble';
import { TodoDetailsPrompt } from './TodoDetailsPrompt';
import type { Message, Segment, TodoBoxSpec } from './types';
import type { TodoItem } from '../blocks';

export interface MessageListItemProps {
  msg: Message;
  msgIndex: number;
  totalCount: number;
  isLoading: boolean;
  isWaiting: boolean;
  segments: Segment[];
  overrideTodoBoxes: TodoBoxSpec[];
  hideTodoInThisMessage: boolean;
  showAllCompletedPrompt: boolean;
  latestTodoItemsForPrompt: TodoItem[] | null;
  isLastAssistantStreaming: boolean;
}

export function MessageListItem({
  msg,
  msgIndex: _msgIndex,
  totalCount: _totalCount,
  isLoading: _isLoading,
  isWaiting,
  segments,
  overrideTodoBoxes,
  hideTodoInThisMessage,
  showAllCompletedPrompt,
  latestTodoItemsForPrompt,
  isLastAssistantStreaming,
}: MessageListItemProps) {
  return (
    <>
      <div className="flex flex-col w-full">
        <MessageBubble
          message={msg}
          segments={segments}
          isLastAssistantStreaming={isLastAssistantStreaming}
          isWaiting={isWaiting}
          hideTodoInThisMessage={hideTodoInThisMessage}
          overrideTodoBoxes={overrideTodoBoxes}
        />
      </div>
      {showAllCompletedPrompt && latestTodoItemsForPrompt != null && (
        <div className="flex flex-col items-start w-full">
          <TodoDetailsPrompt items={latestTodoItemsForPrompt} />
        </div>
      )}
    </>
  );
}
