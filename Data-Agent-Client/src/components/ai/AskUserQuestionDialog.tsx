import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '../ui/Dialog';
import { useAskQuestionModalStore } from '../../store/askQuestionModalStore';
import { AskUserUnanswered } from './blocks/AskUserUnanswered';

interface AskUserQuestionDialogProps {
  /** Current conversation ID - used to auto-close when switching conversations */
  currentConversationId: number | null;
}

/**
 * Global modal for AskUserQuestion tool.
 * Automatically closes when switching to a different conversation.
 */
export function AskUserQuestionDialog({ currentConversationId }: AskUserQuestionDialogProps) {
  const { t } = useTranslation();
  const { isOpen, conversationId, questions, onSubmit, close } = useAskQuestionModalStore();

  // Auto-close when conversation changes
  useEffect(() => {
    if (isOpen && conversationId !== currentConversationId) {
      close();
    }
  }, [currentConversationId, isOpen, conversationId, close]);

  const handleSubmit = (answer: string) => {
    if (onSubmit) {
      onSubmit(answer);
    }
    close();
  };

  // Only show if open AND conversation matches
  const shouldShow = isOpen && conversationId === currentConversationId;

  return (
    <Dialog open={shouldShow} onOpenChange={(open) => !open && close()}>
      <DialogContent className="sm:max-w-[500px] max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{t('ai.askUserQuestion.label')}</DialogTitle>
        </DialogHeader>
        <div className="mt-2">
          <AskUserUnanswered
            questions={questions}
            onSubmit={handleSubmit}
            disabled={false}
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}
