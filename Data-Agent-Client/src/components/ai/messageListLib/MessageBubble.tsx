import React from 'react';
import ReactMarkdown from 'react-markdown';
import { useTranslation } from 'react-i18next';
import { Bot, User } from 'lucide-react';
import { parseMentionSegments } from '../mentionTypes';
import { markdownComponents } from '../blocks';
import { renderSegment } from './segmentRenderer';
import type { Message, Segment } from './types';
import { SegmentKind } from './types';
import { MessageRole } from '../../../types/chat';

const MENTION_COLOR_CLASS = 'text-cyan-400 font-medium';

function renderContentWithMentions(content: string): React.ReactNode[] {
  const segments = parseMentionSegments(content);
  return segments.map((seg, i) =>
    seg.type === 'mention' ? (
      <span key={i} className={MENTION_COLOR_CLASS}>
        {seg.text}
      </span>
    ) : (
      <React.Fragment key={i}>{seg.text}</React.Fragment>
    )
  );
}

export interface MessageBubbleProps {
  message: Message;
  segments: Segment[];
  isLastAssistantStreaming: boolean;
}

export function MessageBubble({
  message,
  segments,
  isLastAssistantStreaming,
}: MessageBubbleProps) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center space-x-2 mb-1.5 opacity-60">
        {message.role === MessageRole.ASSISTANT && <Bot className="w-3 h-3 shrink-0" />}
        <span className="text-[10px] font-medium theme-text-secondary">
          {message.role === MessageRole.ASSISTANT ? t('ai.bot_name') : t('ai.you')}
        </span>
        {message.role === MessageRole.USER && <User className="w-3 h-3 shrink-0" />}
      </div>
      {message.role === MessageRole.USER ? (
        <div className="px-3 py-2 rounded-lg text-xs bg-primary/90 text-primary-foreground w-fit max-w-full">
          <p className="mb-0 leading-relaxed whitespace-pre-wrap">
            {renderContentWithMentions(message.content)}
          </p>
        </div>
      ) : (
        <div className="text-xs theme-text-primary">
          {segments.length > 0 ? (
            <div className="space-y-0">
              {segments.map((seg, i) =>
                renderSegment(
                  seg,
                  i,
                  isLastAssistantStreaming &&
                    i === segments.length - 1 &&
                    seg.kind === SegmentKind.THOUGHT
                )
              )}
            </div>
          ) : (
            <ReactMarkdown components={markdownComponents}>
              {message.content || ''}
            </ReactMarkdown>
          )}
        </div>
      )}
    </div>
  );
}
