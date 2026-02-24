import { create } from 'zustand';
import type { SingleQuestion } from '../components/ai/blocks/askUserQuestionTypes';

interface AskQuestionModalState {
  isOpen: boolean;
  conversationId: number | null;
  questions: SingleQuestion[];
  onSubmit: ((answer: string) => void) | null;

  open: (conversationId: number, questions: SingleQuestion[], onSubmit: (answer: string) => void) => void;
  close: () => void;
}

/**
 * Global store for AskUserQuestion modal.
 * Modal is bound to a specific conversation ID and auto-closes when switching conversations.
 */
export const useAskQuestionModalStore = create<AskQuestionModalState>((set) => ({
  isOpen: false,
  conversationId: null,
  questions: [],
  onSubmit: null,

  open: (conversationId, questions, onSubmit) => {
    console.log('[askQuestionModalStore] open() called with:', {
      conversationId,
      questions,
      hasOnSubmit: !!onSubmit,
    });
    set({
      isOpen: true,
      conversationId,
      questions,
      onSubmit,
    });
    console.log('[askQuestionModalStore] State updated to open');
  },

  close: () => {
    console.log('[askQuestionModalStore] close() called');
    set({
      isOpen: false,
      conversationId: null,
      questions: [],
      onSubmit: null,
    });
  },
}));
