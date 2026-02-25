import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { DEFAULT_PREFERENCES, type PreferenceState } from '../constants/workspacePreferences';
import { STORAGE_KEYS } from '../constants/storageKeys';

interface PreferenceStoreState extends PreferenceState {
  updatePreferences: (prefs: Partial<PreferenceState>) => void;
  resetPreferences: () => void;
}

export const usePreferenceStore = create<PreferenceStoreState>()(
  persist(
    (set) => ({
      ...DEFAULT_PREFERENCES,

      updatePreferences: (prefs) =>
        set((state) => ({ ...state, ...prefs })),

      resetPreferences: () =>
        set((state) => ({ ...state, ...DEFAULT_PREFERENCES })),
    }),
    {
      name: STORAGE_KEYS.WORKSPACE,
      partialize: (state) => ({
        resultBehavior: state.resultBehavior,
        tableDblClickMode: state.tableDblClickMode,
        tableDblClickConsoleTarget: state.tableDblClickConsoleTarget,
        aiAutoSelect: state.aiAutoSelect,
        aiAutoWrite: state.aiAutoWrite,
        aiWriteTransaction: state.aiWriteTransaction,
        aiMaxRetries: state.aiMaxRetries,
      }),
    }
  )
);
