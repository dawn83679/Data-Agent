import React, { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { User, Copy, Check } from 'lucide-react';
import { COPY_FEEDBACK_SHORT_MS } from '../../../constants/timing';
import { parseMentionSegments } from '../mentionTypes';
import type { Message } from './types';

const MENTION_COLOR_CLASS = 'text-violet-400 font-medium';

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
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    const text = message.content ?? '';
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), COPY_FEEDBACK_SHORT_MS);
    } catch {
      // ignore
    }
  }, [message.content]);

  return (
    <div className="flex flex-col w-full group/bubble">
      <div className="flex items-center space-x-2 mb-1.5 opacity-60">
        <span className="text-[10px] font-medium theme-text-secondary">
          {t('ai.you')}
        </span>
        <User className="w-3 h-3 shrink-0" />
      </div>
      <div
        className="relative w-full px-3 py-2 pr-9 rounded-lg text-xs border"
        style={{
          backgroundColor: 'var(--user-bubble-bg)',
          color: 'hsl(var(--user-bubble-text))',
          borderColor: 'hsl(var(--user-bubble-border))',
        }}
      >
        <p className="mb-0 leading-relaxed whitespace-pre-wrap">
          {renderContentWithMentions(message.content)}
        </p>
        <button
          type="button"
          onClick={handleCopy}
          aria-label={t('ai.copy')}
          className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded opacity-0 group-hover/bubble:opacity-100 hover:opacity-100 transition-opacity theme-text-secondary hover:theme-text-primary hover:bg-black/10 dark:hover:bg-white/10"
        >
          {copied ? (
            <Check className="w-3.5 h-3.5 text-green-500" aria-hidden />
          ) : (
            <Copy className="w-3.5 h-3.5" aria-hidden />
          )}
        </button>
      </div>
    </div>
  );
}
