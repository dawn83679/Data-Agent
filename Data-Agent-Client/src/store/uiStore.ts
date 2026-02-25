import { create } from 'zustand';

interface UiState {
  isSettingsModalOpen: boolean;
  setSettingsModalOpen: (open: boolean) => void;
}

export const useUiStore = create<UiState>((set) => ({
  isSettingsModalOpen: false,
  setSettingsModalOpen: (open) => set({ isSettingsModalOpen: open }),
}));
