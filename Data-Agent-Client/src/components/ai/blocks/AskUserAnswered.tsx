import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MessageCircle, ChevronDown, ChevronRight } from 'lucide-react';
import type { AskUserQuestionPayload } from './askUserQuestionTypes';

export interface AskUserAnsweredProps {
  payload: AskUserQuestionPayload;
  answer: string;
}

/**
 * Renders an answered AskUserQuestion in collapsed/expandable format.
 */
export function AskUserAnswered({ payload, answer }: AskUserAnsweredProps) {
  const { t } = useTranslation();
  const [collapsed, setCollapsed] = useState(true);

  const hasOptions = payload.options != null && payload.options.length > 0;
  const questionPreview = (payload.question || '—').replace(/\s+/g, ' ').slice(0, 40);
  const summary = `${questionPreview}${questionPreview.length >= 40 ? '…' : ''} — ${answer}`;

  return (
    <>
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className="w-full px-3 py-2.5 flex items-center gap-2 text-left rounded-lg transition-colors theme-text-primary hover:bg-black/5 dark:hover:bg-white/5"
        aria-expanded={!collapsed}
      >
        <MessageCircle className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
        <span className="text-[10px] font-semibold tracking-wide theme-text-secondary shrink-0">
          {t('ai.askUserQuestion.label')}
        </span>
        <span className="min-w-0 flex-1 truncate text-[12px] theme-text-secondary">
          {summary}
        </span>
        <span className="shrink-0 opacity-60" aria-hidden>
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <div className="px-3 pt-0 pb-3 border-t theme-border">
          <p className="theme-text-primary text-[13px] mt-3 mb-3 whitespace-pre-wrap">
            {payload.question || '—'}
          </p>

          {hasOptions &&
            (() => {
              const answerIsOption = payload.options!.includes(answer);
              const firstMatchIndex = answerIsOption ? payload.options!.findIndex((opt) => opt === answer) : -1;
              return (
                <div className="flex flex-col gap-2 mb-3">
                  {payload.options!.map((opt, i) => (
                    <div
                      key={i}
                      className={
                        i === firstMatchIndex
                          ? 'w-full text-left px-3 py-2 rounded-md text-[12px] font-medium bg-[var(--accent-blue)] text-white'
                          : 'w-full text-left px-3 py-2 rounded-md text-[12px] border theme-border opacity-60 theme-text-primary'
                      }
                    >
                      {opt}
                    </div>
                  ))}
                </div>
              );
            })()}

          {(!hasOptions || !payload.options!.includes(answer)) && (
            <div className="py-2 px-2.5 rounded bg-[var(--accent-blue)] text-[12px] font-medium text-white">
              {answer}
            </div>
          )}
        </div>
      )}
    </>
  );
}
