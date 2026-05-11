import { MessageBubble } from './MessageBubble';
import { TodoDetailsPrompt } from './TodoDetailsPrompt';
import type { Message, Segment, TodoBoxSpec } from './types';
import type { TodoItem } from '../blocks';
import type { WaitingPromptMode } from '../../../types/chat';

export interface MessageListItemProps {
  msg: Message;
  msgIndex: number;
  totalCount: number;
  isLoading: boolean;
  isWaiting: boolean;
  waitingPromptMode: WaitingPromptMode;
  showAssistantHeader: boolean;
  segments: Segment[];
  overrideTodoBoxes: TodoBoxSpec[];
  hideTodoInThisMessage: boolean;
  showAllCompletedPrompt: boolean;
  latestTodoItemsForPrompt: TodoItem[] | null;
  isLastAssistantStreaming: boolean;
  showElapsedTextForSubAgent?: boolean;
}

export function MessageListItem({
  msg,
  msgIndex: _msgIndex,
  totalCount: _totalCount,
  isLoading: _isLoading,
  isWaiting,
  waitingPromptMode,
  showAssistantHeader,
  segments,
  overrideTodoBoxes,
  hideTodoInThisMessage,
  showAllCompletedPrompt,
  latestTodoItemsForPrompt,
  isLastAssistantStreaming,
  showElapsedTextForSubAgent = true,
}: MessageListItemProps) {
  return (
    <>
      <div className="flex flex-col w-full">
        <MessageBubble
          message={msg}
          segments={segments}
          isLastAssistantStreaming={isLastAssistantStreaming}
          isWaiting={isWaiting}
          waitingPromptMode={waitingPromptMode}
          showAssistantHeader={showAssistantHeader}
          hideTodoInThisMessage={hideTodoInThisMessage}
          overrideTodoBoxes={overrideTodoBoxes}
          showElapsedTextForSubAgent={showElapsedTextForSubAgent}
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
