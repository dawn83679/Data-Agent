import { SegmentList } from './SegmentList';
import { ChatStatsFooter } from '../blocks/ChatStatsFooter';
import type { Message, Segment, TodoBoxSpec } from './types';

export interface AssistantBubbleProps {
  message: Message;
  segments: Segment[];
  hideTodoSegments: boolean;
  overrideTodoBoxes: TodoBoxSpec[];
  isLastAssistantStreaming: boolean;
  isWaiting: boolean;
  showElapsedTextForSubAgent?: boolean;
}

export function AssistantBubble({
  message,
  segments,
  hideTodoSegments,
  overrideTodoBoxes,
  isLastAssistantStreaming,
  isWaiting,
  showElapsedTextForSubAgent = true,
}: AssistantBubbleProps) {
  return (
    <div className="flex flex-col w-full">
      <div className="text-xs theme-text-primary">
        <SegmentList
          segments={segments}
          fallbackContent={message.content ?? ''}
          hideTodoSegments={hideTodoSegments}
          overrideTodoBoxes={overrideTodoBoxes}
          isLastAssistantStreaming={isLastAssistantStreaming}
          isWaiting={isWaiting}
          showElapsedTextForSubAgent={showElapsedTextForSubAgent}
        />
        {!isLastAssistantStreaming && message.doneMetadata && (
          <ChatStatsFooter metadata={message.doneMetadata} />
        )}
      </div>
    </div>
  );
}
