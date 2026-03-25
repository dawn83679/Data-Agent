import { useTranslation } from 'react-i18next';
import { Archive } from 'lucide-react';
import type { Message } from './types';
import { stripCompactSummaryPrefix } from './compactSummary';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { TextBlock } from '../blocks/TextBlock';

export interface CompactSummaryBubbleProps {
  message: Message;
}

export function CompactSummaryBubble({ message }: CompactSummaryBubbleProps) {
  const { t } = useTranslation();
  const summary = stripCompactSummaryPrefix(message.content ?? '');

  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center space-x-2 mb-1.5 opacity-70">
        <Archive className="w-3 h-3 shrink-0" />
        <span className="text-[10px] font-medium theme-text-secondary">
          {t(I18N_KEYS.AI.COMPACT.SUMMARY_LABEL)}
        </span>
      </div>
      <div className="min-w-0 overflow-hidden rounded-lg border border-[color:var(--border-color)] bg-[color:var(--bg-panel)] px-3 py-2 text-xs theme-text-primary">
        <div className="min-w-0 break-words">
          <TextBlock data={summary} />
        </div>
      </div>
    </div>
  );
}
