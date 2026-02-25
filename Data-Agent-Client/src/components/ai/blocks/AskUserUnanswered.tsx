import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Check } from 'lucide-react';
import type { SingleQuestion } from './askUserQuestionTypes';
import { I18N_KEYS } from '../../../constants/i18nKeys';

export interface AskUserUnansweredProps {
  questions: SingleQuestion[];
  onSubmit: (answer: string) => void;
  onReject?: () => void;
  disabled: boolean;
}

interface AnswerValue {
  selectedOptions: string[];  // Selected from options list
  customText: string;          // User's custom input
}

/**
 * Renders unanswered AskUserQuestion(s) with interactive inputs.
 * Multiple questions are displayed as tabs.
 */
export function AskUserUnanswered({ questions, onSubmit, onReject, disabled }: AskUserUnansweredProps) {
  const { t } = useTranslation();

  // Track answers for all questions: Map<questionIndex, AnswerValue>
  const [answers, setAnswers] = useState<Map<number, AnswerValue>>(() => {
    const initial = new Map<number, AnswerValue>();
    questions.forEach((_, idx) => {
      initial.set(idx, { selectedOptions: [], customText: '' });
    });
    return initial;
  });

  // Track active tab for multi-question view
  const [activeTab, setActiveTab] = useState(0);

  // Handle ESC key to reject answering
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !disabled && onReject) {
        e.preventDefault();
        onReject();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [disabled, onReject]);

  const handleOptionClick = (questionIdx: number, option: string, allowMultiSelect: boolean) => {
    if (disabled) return;

    setAnswers((prev) => {
      const newAnswers = new Map(prev);
      const current = newAnswers.get(questionIdx) || { selectedOptions: [], customText: '' };

      if (allowMultiSelect) {
        // Checkbox: toggle selection
        const isSelected = current.selectedOptions.includes(option);
        const newSelected = isSelected
          ? current.selectedOptions.filter((o) => o !== option)
          : [...current.selectedOptions, option];
        newAnswers.set(questionIdx, { ...current, selectedOptions: newSelected });
      } else {
        // Radio: single selection
        newAnswers.set(questionIdx, { ...current, selectedOptions: [option] });
      }

      return newAnswers;
    });
  };

  const handleCustomTextChange = (questionIdx: number, text: string) => {
    setAnswers((prev) => {
      const newAnswers = new Map(prev);
      const current = newAnswers.get(questionIdx) || { selectedOptions: [], customText: '' };
      newAnswers.set(questionIdx, { ...current, customText: text });
      return newAnswers;
    });
  };

  const handleSubmit = () => {
    if (disabled) return;

    // Format answer based on single or multiple questions
    if (questions.length === 1) {
      // Single question: simple answer format (backward compatibility)
      const answer = answers.get(0);
      if (!answer) return;

      const selectedParts = answer.selectedOptions;
      const customPart = answer.customText.trim();

      let answerStr = '';
      if (selectedParts.length > 0 && customPart) {
        // Both selected options and custom input
        answerStr = `${selectedParts.join(', ')} | ${t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_LABEL)}: ${customPart}`;
      } else if (selectedParts.length > 0) {
        // Only selected options
        answerStr = selectedParts.join(', ');
      } else if (customPart) {
        // Only custom input
        answerStr = `${t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_LABEL)}: ${customPart}`;
      }

      if (answerStr) {
        onSubmit(answerStr);
      }
    } else {
      // Multiple questions: formatted as "question"="answer" pairs
      const pairs = questions.map((q, idx) => {
        const answer = answers.get(idx);
        if (!answer) return `"${q.question}"=""`;

        const selectedParts = answer.selectedOptions;
        const customPart = answer.customText.trim();

        let answerStr = '';
        if (selectedParts.length > 0 && customPart) {
          // Both selected options and custom input
          answerStr = `${selectedParts.join(', ')} | ${t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_LABEL)}: ${customPart}`;
        } else if (selectedParts.length > 0) {
          // Only selected options
          answerStr = selectedParts.join(', ');
        } else if (customPart) {
          // Only custom input
          answerStr = `${t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_LABEL)}: ${customPart}`;
        }

        return `"${q.question}"="${answerStr}"`;
      });

      const formattedAnswer =
        t(I18N_KEYS.AI.ASK_USER_QUESTION.MULTI_ANSWER_PREFIX) +
        pairs.join(', ') +
        '. ' +
        t(I18N_KEYS.AI.ASK_USER_QUESTION.CONTINUE_SUFFIX);

      onSubmit(formattedAnswer);
    }
  };

  // Check if all questions have at least one answer
  const canSubmit = !disabled && questions.every((_, idx) => {
    const answer = answers.get(idx);
    return answer && (answer.selectedOptions.length > 0 || answer.customText.trim() !== '');
  });

  const truncateText = (text: string, maxLength: number): string => {
    if (text.length <= maxLength) return text;
    return text.slice(0, maxLength) + '...';
  };

  const renderQuestionContent = (question: SingleQuestion, qIdx: number) => {
    const answer = answers.get(qIdx) || { selectedOptions: [], customText: '' };
    const allowMultiSelect = true; // Always allow multi-select (checkbox)

    return (
      <div>
        {/* Full question text */}
        <p className="theme-text-primary text-[13px] mb-3 whitespace-pre-wrap">
          {question.question}
        </p>

        {/* Options: radio or checkbox based on allowMultiSelect */}
        {question.options && question.options.length > 0 && (
          <div className="flex flex-col gap-2 mb-3">
            {question.options.map((opt, optIdx) => {
              const isSelected = answer.selectedOptions.includes(opt);

              return (
                <button
                  key={optIdx}
                  type="button"
                  disabled={disabled}
                  onClick={() => handleOptionClick(qIdx, opt, allowMultiSelect)}
                  className={`w-full text-left px-3 py-2 rounded-md text-[12px] transition-colors border theme-text-primary disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2 ${
                    isSelected
                      ? 'theme-bg-selected theme-border-accent border'
                      : 'theme-border theme-bg-hover'
                  }`}
                >
                  {/* Checkbox or Radio indicator */}
                  <div
                    className={`shrink-0 w-4 h-4 border theme-border flex items-center justify-center ${
                      allowMultiSelect ? 'rounded' : 'rounded-full'
                    } ${
                      isSelected
                        ? 'bg-[var(--accent-blue)] border-[var(--accent-blue)]'
                        : 'bg-transparent'
                    }`}
                  >
                    {isSelected && (
                      allowMultiSelect ? (
                        <Check className="w-3 h-3 text-white" />
                      ) : (
                        <div className="w-2 h-2 rounded-full bg-white" />
                      )
                    )}
                  </div>
                  <span>{opt}</span>
                </button>
              );
            })}
          </div>
        )}

        {/* Free text input - ALWAYS displayed */}
        <div className="mb-3">
          <input
            type="text"
            value={answer.customText}
            onChange={(e) => handleCustomTextChange(qIdx, e.target.value)}
            placeholder={
              question.freeTextHint || t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_INPUT_PLACEHOLDER)
            }
            disabled={disabled}
            className="w-full px-2.5 py-1.5 rounded border theme-border theme-bg-panel theme-text-primary text-[12px] placeholder:theme-text-secondary focus:outline-none focus:ring-2 focus:ring-[var(--accent-blue)] focus:border-[var(--accent-blue)] disabled:opacity-60 disabled:cursor-not-allowed"
            aria-label={t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_INPUT_PLACEHOLDER)}
          />
        </div>
      </div>
    );
  };

  return (
    <div className="px-3 pt-2.5 pb-3">
      {/* Tab headers for multiple questions */}
      {questions.length > 1 && (
        <div className="flex gap-1 mb-3 border-b theme-border">
          {questions.map((q, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => setActiveTab(idx)}
              className={`px-3 py-1.5 text-[11px] font-medium transition-colors border-b-2 ${
                activeTab === idx
                  ? 'border-[var(--accent-blue)] theme-text-primary'
                  : 'border-transparent theme-text-secondary hover:theme-text-primary'
              }`}
            >
              {truncateText(q.question, 30)}
            </button>
          ))}
        </div>
      )}

      {/* Question content */}
      {questions.length === 1 ? (
        // Single question: show directly
        renderQuestionContent(questions[0], 0)
      ) : (
        // Multiple questions: show active tab
        renderQuestionContent(questions[activeTab], activeTab)
      )}

      {/* Submit and Reject buttons */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={handleSubmit}
          disabled={!canSubmit}
          className="flex-1 px-3 py-1.5 rounded-md text-[12px] font-medium bg-primary text-primary-foreground border border-primary hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
        >
          {t(I18N_KEYS.AI.ASK_USER_QUESTION.SUBMIT_ANSWER)}
        </button>
        {onReject && (
          <button
            type="button"
            onClick={onReject}
            disabled={disabled}
            className="px-3 py-1.5 rounded-md text-[12px] font-medium theme-bg-panel theme-text-secondary border theme-border hover:theme-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {t(I18N_KEYS.AI.ASK_USER_QUESTION.REJECT)}
          </button>
        )}
      </div>
    </div>
  );
}
