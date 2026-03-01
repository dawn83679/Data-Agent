import { create } from 'zustand';
import type { WriteConfirmPayload } from '../components/ai/blocks/writeConfirmTypes';

interface WriteConfirmModalState {
  isOpen: boolean;
  conversationId: number | null;
  payload: WriteConfirmPayload | null;
  onConfirm: (() => void) | null;
  onCancel: (() => void) | null;

  open: (
    conversationId: number,
    payload: WriteConfirmPayload,
    onConfirm: () => void,
    onCancel: () => void
  ) => void;
  close: () => void;
}

export const useWriteConfirmModalStore = create<WriteConfirmModalState>((set) => ({
  isOpen: false,
  conversationId: null,
  payload: null,
  onConfirm: null,
  onCancel: null,

  open: (conversationId, payload, onConfirm, onCancel) =>
    set({ isOpen: true, conversationId, payload, onConfirm, onCancel }),

  close: () =>
    set({ isOpen: false, conversationId: null, payload: null, onConfirm: null, onCancel: null }),
}));
