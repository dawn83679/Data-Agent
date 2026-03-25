import { MessageRole } from '../../../types/chat';
import { UserBubble } from './UserBubble';
import { AssistantBubble } from './AssistantBubble';
import { CompactSummaryBubble } from './CompactSummaryBubble';
import { isCompactSummaryMessage } from './compactSummary';
import type { TodoBoxSpec } from './types';
import type { Message, Segment } from './types';

export interface MessageBubbleProps {
  message: Message;
  segments: Segment[];
  isLastAssistantStreaming: boolean;
  isWaiting: boolean;
  /** When true, do not render raw todo segments (we show boxes from overrideTodoBoxes where applicable). */
  hideTodoInThisMessage?: boolean;
  /** Todo boxes to show in this message (one per todoId that first appeared here). */
  overrideTodoBoxes?: TodoBoxSpec[];
  showElapsedTextForSubAgent?: boolean;
}

export function MessageBubble({
  message,
  segments,
  isLastAssistantStreaming,
  isWaiting,
  hideTodoInThisMessage = false,
  overrideTodoBoxes = [],
  showElapsedTextForSubAgent = true,
}: MessageBubbleProps) {
  if (isCompactSummaryMessage(message)) {
    return <CompactSummaryBubble message={message} />;
  }
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
      isWaiting={isWaiting}
      showElapsedTextForSubAgent={showElapsedTextForSubAgent}
    />
  );
}
