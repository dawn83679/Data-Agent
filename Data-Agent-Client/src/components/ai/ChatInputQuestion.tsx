import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useAskQuestionModalStore } from '../../store/askQuestionModalStore';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { AskUserUnanswered } from './blocks/AskUserUnanswered';

interface ChatInputQuestionProps {
  conversationId: number | null;
}

/** Shows question form when there's a pending question for current conversation */
export function ChatInputQuestion({ conversationId }: ChatInputQuestionProps) {
  const { t } = useTranslation();
  const { isOpen, conversationId: questionConversationId, questions, onSubmit, close } = useAskQuestionModalStore();

  const hasQuestion = isOpen && conversationId !== null && questionConversationId === conversationId;

  const handleQuestionSubmit = useCallback(
    (answer: string) => {
      if (onSubmit) {
        onSubmit(answer);
      }
      close();
    },
    [onSubmit, close]
  );

  const handleQuestionReject = useCallback(() => {
    if (onSubmit) {
      onSubmit(t(I18N_KEYS.AI.ASK_USER_QUESTION.REJECT_MESSAGE));
    }
    close();
  }, [onSubmit, close, t]);

  if (!hasQuestion) {
    return null;
  }

  return (
    <div className="p-2 theme-bg-panel border-t theme-border shrink-0">
      <div className="rounded-lg border theme-border theme-bg-main p-4">
        <AskUserUnanswered
          questions={questions}
          onSubmit={handleQuestionSubmit}
          onReject={handleQuestionReject}
          disabled={false}
        />
      </div>
    </div>
  );
}
