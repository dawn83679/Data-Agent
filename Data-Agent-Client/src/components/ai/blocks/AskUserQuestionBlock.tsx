import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MessageCircle, ChevronDown, ChevronRight } from 'lucide-react';
import type { AskUserQuestionPayload } from './askUserQuestionTypes';

export interface AskUserQuestionBlockProps {
  payload: AskUserQuestionPayload;
  onSubmit: (answer: string) => void;
  /** When true, disables submit and inputs to prevent repeated submission. */
  disabled?: boolean;
  /** When set, show question/options in read-only style and display this as the submitted answer (e.g. history). */
  submittedAnswer?: string;
}

export function AskUserQuestionBlock({
  payload,
  onSubmit,
  disabled = false,
  submittedAnswer,
}: AskUserQuestionBlockProps) {
  const { t } = useTranslation();
  const [selectedOption, setSelectedOption] = useState<string | null>(null);
  const [freeText, setFreeText] = useState('');
  const [localSubmittedAnswer, setLocalSubmittedAnswer] = useState<string | null>(null);

  const hasOptions = payload.options != null && payload.options.length > 0;
  const hasFreeText = payload.freeTextHint != null && payload.freeTextHint !== '';
  const isAnswered =
    (submittedAnswer != null && submittedAnswer.trim() !== '') || localSubmittedAnswer != null;
  const displayAnswer = submittedAnswer ?? localSubmittedAnswer ?? '';
  const [answeredBlockCollapsed, setAnsweredBlockCollapsed] = useState(true);

  const handleSubmit = () => {
    if (disabled) return;
    const answer = freeText.trim() || selectedOption || '';
    if (answer) {
      setLocalSubmittedAnswer(answer);
      onSubmit(answer);
    }
  };

  const canSubmit = (selectedOption != null || freeText.trim() !== '') && !disabled && !isAnswered;

  const questionPreview = (payload.question || '—').replace(/\s+/g, ' ').slice(0, 40);
  const summary = isAnswered ? `${questionPreview}${questionPreview.length >= 40 ? '…' : ''} — ${displayAnswer}` : '';

  return (
    <div
      className="mt-1 rounded-lg border theme-border overflow-hidden theme-bg-panel"
      aria-label={t('ai.askUserQuestion.label')}
    >
      {isAnswered ? (
        <>
          <button
            type="button"
            onClick={() => setAnsweredBlockCollapsed((c) => !c)}
            className="w-full px-3 py-2.5 flex items-center gap-2 text-left rounded-lg transition-colors theme-text-primary hover:bg-black/5 dark:hover:bg-white/5"
            aria-expanded={!answeredBlockCollapsed}
          >
            <MessageCircle className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
            <span className="text-[10px] font-semibold tracking-wide theme-text-secondary shrink-0">
              {t('ai.askUserQuestion.label')}
            </span>
            <span className="min-w-0 flex-1 truncate text-[12px] theme-text-secondary">
              {summary}
            </span>
            <span className="shrink-0 opacity-60" aria-hidden>
              {answeredBlockCollapsed ? (
                <ChevronRight className="w-3.5 h-3.5" />
              ) : (
                <ChevronDown className="w-3.5 h-3.5" />
              )}
            </span>
          </button>
          {!answeredBlockCollapsed && (
            <div className="px-3 pt-0 pb-3 border-t theme-border">
              <p className="theme-text-primary text-[13px] mt-3 mb-3 whitespace-pre-wrap">
                {payload.question || '—'}
              </p>
              {hasOptions && (() => {
                const answerIsOption = payload.options!.includes(displayAnswer);
                const firstMatchIndex = answerIsOption
                  ? payload.options!.findIndex((opt) => opt === displayAnswer)
                  : -1;
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
              {(!hasOptions || !payload.options!.includes(displayAnswer)) && (
                <div className="py-2 px-2.5 rounded bg-[var(--accent-blue)] text-[12px] font-medium text-white">
                  {displayAnswer}
                </div>
              )}
            </div>
          )}
        </>
      ) : (
        <div className="px-3 pt-2.5 pb-3">
          <div className="flex items-center gap-2 mb-2">
            <MessageCircle className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
            <span className="text-[10px] font-semibold tracking-wide theme-text-secondary">
              {t('ai.askUserQuestion.label')}
            </span>
          </div>
          <p className="theme-text-primary text-[13px] mb-3 whitespace-pre-wrap">
            {payload.question || '—'}
          </p>

          {hasOptions && (
            <div className="flex flex-col gap-2 mb-3">
              {payload.options!.map((opt, i) => (
                <button
                  key={i}
                  type="button"
                  disabled={disabled}
                  onClick={() => setSelectedOption(opt)}
                  className={`w-full text-left px-3 py-2 rounded-md text-[12px] transition-colors border theme-text-primary disabled:opacity-60 disabled:cursor-not-allowed ${
                    selectedOption === opt
                      ? 'theme-bg-selected theme-border-accent border'
                      : 'theme-border theme-bg-hover'
                  }`}
                >
                  {opt}
                </button>
              ))}
            </div>
          )}

          <div className="mb-3">
            <input
              type="text"
              value={freeText}
              onChange={(e) => setFreeText(e.target.value)}
              placeholder={hasFreeText ? (payload.freeTextHint ?? '') : t('ai.askUserQuestion.inputPlaceholder')}
              disabled={disabled}
              className="w-full px-2.5 py-1.5 rounded border theme-border theme-bg-panel theme-text-primary text-[12px] placeholder:theme-text-secondary focus:outline-none focus:ring-2 focus:ring-[var(--accent-blue)] focus:border-[var(--accent-blue)] disabled:opacity-60 disabled:cursor-not-allowed"
              aria-label={t('ai.askUserQuestion.inputPlaceholder')}
            />
          </div>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={!canSubmit}
            className="px-3 py-1.5 rounded-md text-[12px] font-medium bg-primary text-primary-foreground border border-primary hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
          >
            {t('ai.askUserQuestion.submitAnswer')}
          </button>
        </div>
      )}
    </div>
  );
}
