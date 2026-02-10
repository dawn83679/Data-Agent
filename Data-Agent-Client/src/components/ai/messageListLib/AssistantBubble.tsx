import { useTranslation } from 'react-i18next';
import { Bot } from 'lucide-react';
import { SegmentList } from './SegmentList';
import type { Message, Segment, TodoBoxSpec } from './types';

export interface AssistantBubbleProps {
  message: Message;
  segments: Segment[];
  hideTodoSegments: boolean;
  overrideTodoBoxes: TodoBoxSpec[];
  isLastAssistantStreaming: boolean;
  showPlanningIndicator?: boolean;
}

export function AssistantBubble({
  message,
  segments,
  hideTodoSegments,
  overrideTodoBoxes,
  isLastAssistantStreaming,
  showPlanningIndicator = false,
}: AssistantBubbleProps) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center space-x-2 mb-1.5 opacity-60">
        <Bot className="w-3 h-3 shrink-0" />
        <span className="text-[10px] font-medium theme-text-secondary">
          {t('ai.bot_name')}
        </span>
      </div>
      <div className="text-xs theme-text-primary">
        <SegmentList
          segments={segments}
          fallbackContent={message.content ?? ''}
          hideTodoSegments={hideTodoSegments}
          overrideTodoBoxes={overrideTodoBoxes}
          isLastAssistantStreaming={isLastAssistantStreaming}
          showPlanningIndicator={showPlanningIndicator}
        />
      </div>
    </div>
  );
}
