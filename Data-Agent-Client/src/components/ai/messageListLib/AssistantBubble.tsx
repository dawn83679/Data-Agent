import { useTranslation } from 'react-i18next';
import { Bot } from 'lucide-react';
import { SegmentList } from './SegmentList';
import type { Message, Segment, TodoBoxSpec } from './types';
import type { WaitingPromptMode } from '../../../types/chat';
import { I18N_KEYS } from '../../../constants/i18nKeys';

export interface AssistantBubbleProps {
  message: Message;
  segments: Segment[];
  hideTodoSegments: boolean;
  overrideTodoBoxes: TodoBoxSpec[];
  isLastAssistantStreaming: boolean;
  isWaiting: boolean;
  waitingPromptMode: WaitingPromptMode;
  showAssistantHeader?: boolean;
  showElapsedTextForSubAgent?: boolean;
}

export function AssistantBubble({
  message,
  segments,
  hideTodoSegments,
  overrideTodoBoxes,
  isLastAssistantStreaming,
  isWaiting,
  waitingPromptMode,
  showAssistantHeader = true,
  showElapsedTextForSubAgent = true,
}: AssistantBubbleProps) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col w-full">
      {showAssistantHeader && (
        <div className="flex items-center gap-1.5 opacity-60 leading-4">
          <Bot className="w-3 h-3 shrink-0" />
          <span className="text-[10px] font-medium theme-text-secondary">
            {t(I18N_KEYS.AI.BOT_NAME)}
          </span>
        </div>
      )}
      <div className="text-xs theme-text-primary">
        <SegmentList
          segments={segments}
          fallbackContent={message.content ?? ''}
          hideTodoSegments={hideTodoSegments}
          overrideTodoBoxes={overrideTodoBoxes}
          isLastAssistantStreaming={isLastAssistantStreaming}
          isWaiting={isWaiting}
          waitingPromptMode={waitingPromptMode}
          showElapsedTextForSubAgent={showElapsedTextForSubAgent}
        />
      </div>
    </div>
  );
}
