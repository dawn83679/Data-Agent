import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AskUserAnswered } from './AskUserAnswered';
import { AskUserUnanswered } from './AskUserUnanswered';
import type { AskUserQuestionPayload } from './askUserQuestionTypes';

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

  return (
    <div
      className="mt-1 rounded-lg border theme-border overflow-hidden theme-bg-panel"
      aria-label={t('ai.askUserQuestion.label')}
    >
      {isAnswered ? (
        <AskUserAnswered payload={payload} answer={displayAnswer} />
      ) : (
        <AskUserUnanswered payload={payload} onSubmit={handleSubmit} disabled={disabled} />
      )}
    </div>
  );
}
