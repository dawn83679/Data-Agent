import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MessageCircle } from 'lucide-react';
import type { AskUserQuestionPayload } from './askUserQuestionTypes';

export interface AskUserUnansweredProps {
  payload: AskUserQuestionPayload;
  onSubmit: (answer: string) => void;
  disabled: boolean;
}

/**
 * Renders an unanswered AskUserQuestion with interactive inputs.
 */
export function AskUserUnanswered({ payload, onSubmit, disabled }: AskUserUnansweredProps) {
  const { t } = useTranslation();
  const [selectedOption, setSelectedOption] = useState<string | null>(null);
  const [freeText, setFreeText] = useState('');

  const hasOptions = payload.options != null && payload.options.length > 0;
  const hasFreeText = payload.freeTextHint != null && payload.freeTextHint !== '';

  const handleSubmit = () => {
    if (disabled) return;
    const answer = freeText.trim() || selectedOption || '';
    if (answer) {
      onSubmit(answer);
    }
  };

  const canSubmit = (selectedOption != null || freeText.trim() !== '') && !disabled;

  return (
    <div className="px-3 pt-2.5 pb-3">
      <div className="flex items-center gap-2 mb-2">
        <MessageCircle className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
        <span className="text-[10px] font-semibold tracking-wide theme-text-secondary">
          {t('ai.askUserQuestion.label')}
        </span>
      </div>

      <p className="theme-text-primary text-[13px] mb-3 whitespace-pre-wrap">{payload.question || '—'}</p>

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
  );
}
