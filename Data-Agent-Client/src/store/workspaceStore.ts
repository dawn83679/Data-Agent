/**
 * WorkspaceStore Facade
 *
 * Combines multiple focused stores into a single unified API for backward compatibility.
 * Internally delegates to: tabStore, preferenceStore, dbTypeStore, uiStore
 *
 * This facade pattern allows us to:
 * - Keep existing code unchanged (no breaking changes)
 * - Internally use smaller, focused stores
 * - Maintain a single point of access for workspace state
 */

import { create } from 'zustand';
import type { DbTypeOption } from '../types/dbType';
import type { ConsoleTabMetadata } from '../types/tab';
import type { PreferenceState } from '../constants/workspacePreferences';
import { useTabStore, type Tab } from './tabStore';
import { usePreferenceStore } from './preferenceStore';
import { useDbTypeStore } from './dbTypeStore';
import { useUiStore } from './uiStore';

/**
 * WorkspaceState is the unified interface combining all sub-stores
 */
export interface WorkspaceState extends PreferenceState {
  // Tab state
  tabs: Tab[];
  activeTabId: string | null;

  // UI state
  isSettingsModalOpen: boolean;

  // Database types state
  supportedDbTypes: DbTypeOption[];
  supportedDbTypesLoading: boolean;

  // Tab actions
  openTab: (tab: Omit<Tab, 'active'>) => void;
  closeTab: (id: string) => void;
  switchTab: (id: string) => void;
  updateTabContent: (id: string, content: string) => void;
  updateTabMetadata: (id: string, metadata: Partial<ConsoleTabMetadata>) => void;
  reorderTabs: (sourceId: string, destinationId: string) => void;

  // UI actions
  setSettingsModalOpen: (open: boolean) => void;

  // Preference actions
  updatePreferences: (prefs: Partial<PreferenceState>) => void;
  resetPreferences: () => void;

  // DbType actions
  fetchSupportedDbTypes: () => Promise<void>;
}

/**
 * Helper to get current state from all sub-stores
 */
const getAggregatedState = (): WorkspaceState => {
  const tabState = useTabStore.getState();
  const prefState = usePreferenceStore.getState();
  const dbState = useDbTypeStore.getState();
  const uiState = useUiStore.getState();

  // Will be overridden with actual actions below
  return {
    // Tab state
    tabs: tabState.tabs,
    activeTabId: tabState.activeTabId,

    // Preference state
    resultBehavior: prefState.resultBehavior,
    tableDblClickMode: prefState.tableDblClickMode,
    tableDblClickConsoleTarget: prefState.tableDblClickConsoleTarget,
    aiAutoSelect: prefState.aiAutoSelect,
    aiAutoWrite: prefState.aiAutoWrite,
    aiWriteTransaction: prefState.aiWriteTransaction,
    aiMaxRetries: prefState.aiMaxRetries,

    // DbType state
    supportedDbTypes: dbState.supportedDbTypes,
    supportedDbTypesLoading: dbState.isLoading,

    // UI state
    isSettingsModalOpen: uiState.isSettingsModalOpen,

    // Placeholder actions (will be overridden)
    openTab: () => {},
    closeTab: () => {},
    switchTab: () => {},
    updateTabContent: () => {},
    updateTabMetadata: () => {},
    reorderTabs: () => {},
    setSettingsModalOpen: () => {},
    updatePreferences: () => {},
    resetPreferences: () => {},
    fetchSupportedDbTypes: async () => {},
  };
};

/**
 * Create a facade store that aggregates and delegates to sub-stores.
 * Actions delegate to sub-stores AND update the facade store state.
 */
export const useWorkspaceStore = create<WorkspaceState>((set) => {
  return {
    ...getAggregatedState(),

    // Tab actions - delegate to tabStore AND update facade state
    openTab: (tab) => {
      useTabStore.getState().openTab(tab);
      const tabState = useTabStore.getState();
      set({
        tabs: tabState.tabs,
        activeTabId: tabState.activeTabId,
      });
    },

    closeTab: (id) => {
      useTabStore.getState().closeTab(id);
      const tabState = useTabStore.getState();
      set({
        tabs: tabState.tabs,
        activeTabId: tabState.activeTabId,
      });
    },

    switchTab: (id) => {
      useTabStore.getState().switchTab(id);
      const tabState = useTabStore.getState();
      set({
        tabs: tabState.tabs,
        activeTabId: tabState.activeTabId,
      });
    },

    updateTabContent: (id, content) => {
      useTabStore.getState().updateTabContent(id, content);
      const tabState = useTabStore.getState();
      set({
        tabs: tabState.tabs,
      });
    },

    updateTabMetadata: (id, metadata) => {
      useTabStore.getState().updateTabMetadata(id, metadata);
      const tabState = useTabStore.getState();
      set({
        tabs: tabState.tabs,
      });
    },

    reorderTabs: (sourceId, destinationId) => {
      useTabStore.getState().reorderTabs(sourceId, destinationId);
      const tabState = useTabStore.getState();
      set({
        tabs: tabState.tabs,
      });
    },

    // UI actions - delegate to uiStore AND update facade state
    setSettingsModalOpen: (open) => {
      useUiStore.getState().setSettingsModalOpen(open);
      set({
        isSettingsModalOpen: open,
      });
    },

    // Preference actions - delegate to preferenceStore AND update facade state
    updatePreferences: (prefs) => {
      usePreferenceStore.getState().updatePreferences(prefs);
      const prefState = usePreferenceStore.getState();
      set({
        resultBehavior: prefState.resultBehavior,
        tableDblClickMode: prefState.tableDblClickMode,
        tableDblClickConsoleTarget: prefState.tableDblClickConsoleTarget,
        aiAutoSelect: prefState.aiAutoSelect,
        aiAutoWrite: prefState.aiAutoWrite,
        aiWriteTransaction: prefState.aiWriteTransaction,
        aiMaxRetries: prefState.aiMaxRetries,
      });
    },

    resetPreferences: () => {
      usePreferenceStore.getState().resetPreferences();
      const prefState = usePreferenceStore.getState();
      set({
        resultBehavior: prefState.resultBehavior,
        tableDblClickMode: prefState.tableDblClickMode,
        tableDblClickConsoleTarget: prefState.tableDblClickConsoleTarget,
        aiAutoSelect: prefState.aiAutoSelect,
        aiAutoWrite: prefState.aiAutoWrite,
        aiWriteTransaction: prefState.aiWriteTransaction,
        aiMaxRetries: prefState.aiMaxRetries,
      });
    },

    // DbType actions - delegate to dbTypeStore AND update facade state
    fetchSupportedDbTypes: async () => {
      await useDbTypeStore.getState().fetchSupportedDbTypes();
      const dbState = useDbTypeStore.getState();
      set({
        supportedDbTypes: dbState.supportedDbTypes,
        supportedDbTypesLoading: dbState.isLoading,
      });
    },
  };
});

/**
 * Override getState to always return the latest aggregated state from all sub-stores.
 * This ensures consistency even if sub-stores are updated directly.
 */
const originalGetState = useWorkspaceStore.getState;
useWorkspaceStore.getState = () => {
  const aggregated = getAggregatedState();
  // Get the action functions from the current store state
  const current = originalGetState();
  return {
    ...aggregated,
    openTab: current.openTab,
    closeTab: current.closeTab,
    switchTab: current.switchTab,
    updateTabContent: current.updateTabContent,
    updateTabMetadata: current.updateTabMetadata,
    reorderTabs: current.reorderTabs,
    setSettingsModalOpen: current.setSettingsModalOpen,
    updatePreferences: current.updatePreferences,
    resetPreferences: current.resetPreferences,
    fetchSupportedDbTypes: current.fetchSupportedDbTypes,
  } as WorkspaceState;
};
