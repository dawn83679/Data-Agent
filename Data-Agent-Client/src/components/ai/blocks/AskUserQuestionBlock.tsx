import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AskUserAnswered } from './AskUserAnswered';
import { AskUserUnanswered } from './AskUserUnanswered';
import type { AskUserQuestionPayload } from './askUserQuestionTypes';
import { normalizeToQuestions } from './askUserQuestionTypes';
import { I18N_KEYS } from '../../../constants/i18nKeys';

export interface AskUserQuestionBlockProps {
  payload: AskUserQuestionPayload;
  onSubmit: (answer: string) => void;
  /** When true, disables submit and inputs to prevent repeated submission. */
  disabled?: boolean;
  /** When set, show question/options in read-only style and display this as the submitted answer (e.g. history). */
  submittedAnswer?: string;
}

/**
 * Renders an AskUserQuestion block.
 * Dispatches to answered/unanswered subcomponents based on state.
 */
export function AskUserQuestionBlock({
  payload,
  onSubmit,
  disabled = false,
  submittedAnswer,
}: AskUserQuestionBlockProps) {
  const { t } = useTranslation();
  const [localSubmittedAnswer, setLocalSubmittedAnswer] = useState<string | null>(null);

  const isAnswered =
    (submittedAnswer != null && submittedAnswer.trim() !== '') || localSubmittedAnswer != null;
  const displayAnswer = submittedAnswer ?? localSubmittedAnswer ?? '';

  const handleSubmit = (answer: string) => {
    setLocalSubmittedAnswer(answer);
    onSubmit(answer);
  };

  // Normalize payload to questions array
  const questions = normalizeToQuestions(payload);

  return (
    <div
      className="mt-1 rounded-lg border theme-border overflow-hidden theme-bg-panel"
      aria-label={t(I18N_KEYS.AI.ASK_USER_QUESTION.LABEL)}
    >
      {isAnswered ? (
        <AskUserAnswered questions={questions} answer={displayAnswer} />
      ) : (
        <AskUserUnanswered questions={questions} onSubmit={handleSubmit} disabled={disabled} />
      )}
    </div>
  );
}
