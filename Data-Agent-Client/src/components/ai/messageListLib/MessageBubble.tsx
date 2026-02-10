import { MessageRole } from '../../../types/chat';
import { UserBubble } from './UserBubble';
import { AssistantBubble } from './AssistantBubble';
import type { TodoBoxSpec } from './types';
import type { Message, Segment } from './types';

export interface MessageBubbleProps {
  message: Message;
  segments: Segment[];
  isLastAssistantStreaming: boolean;
  /** When true, do not render raw todo segments (we show boxes from overrideTodoBoxes where applicable). */
  hideTodoInThisMessage?: boolean;
  /** Todo boxes to show in this message (one per todoId that first appeared here). */
  overrideTodoBoxes?: TodoBoxSpec[];
}

export function MessageBubble({
  message,
  segments,
  isLastAssistantStreaming,
  hideTodoInThisMessage = false,
  overrideTodoBoxes = [],
}: MessageBubbleProps) {
  if (message.role === MessageRole.USER) {
    return <UserBubble message={message} />;
  }
  return (
    <AssistantBubble
      message={message}
      segments={segments}
      hideTodoSegments={hideTodoInThisMessage}
      overrideTodoBoxes={overrideTodoBoxes}
      isLastAssistantStreaming={isLastAssistantStreaming}
    />
  );
}
