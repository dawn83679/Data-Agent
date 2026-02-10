import React from 'react';
import { useTranslation } from 'react-i18next';
// React needed for JSX
import { User } from 'lucide-react';
import { parseMentionSegments } from '../mentionTypes';
import type { Message } from './types';

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

export interface UserBubbleProps {
  message: Message;
}

export function UserBubble({ message }: UserBubbleProps) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center space-x-2 mb-1.5 opacity-60">
        <span className="text-[10px] font-medium theme-text-secondary">
          {t('ai.you')}
        </span>
        <User className="w-3 h-3 shrink-0" />
      </div>
      <div
        className="w-full px-3 py-2 rounded-lg text-xs border"
        style={{
          backgroundColor: 'var(--user-bubble-bg)',
          color: 'hsl(var(--user-bubble-text))',
          borderColor: 'hsl(var(--user-bubble-border))',
        }}
      >
        <p className="mb-0 leading-relaxed whitespace-pre-wrap">
          {renderContentWithMentions(message.content)}
        </p>
      </div>
    </div>
  );
}
