import { create } from 'zustand';
import type { ConsoleTabMetadata } from '../types/tab';

export interface Tab {
  id: string;
  name: string;
  type: 'file' | 'table';
  icon?: string;
  content?: string;
  active: boolean;
  metadata?: ConsoleTabMetadata;
}

interface TabState {
  tabs: Tab[];
  activeTabId: string | null;

  // Actions
  openTab: (tab: Omit<Tab, 'active'>) => void;
  closeTab: (id: string) => void;
  switchTab: (id: string) => void;
  updateTabContent: (id: string, content: string) => void;
  updateTabMetadata: (id: string, metadata: Partial<ConsoleTabMetadata>) => void;
  reorderTabs: (sourceId: string, destinationId: string) => void;
}

export const useTabStore = create<TabState>((set) => ({
  tabs: [],
  activeTabId: null,

  openTab: (newTab) =>
    set((state) => {
      const existingTab = state.tabs.find((t) => t.id === newTab.id);
      if (existingTab) {
        return {
          tabs: state.tabs.map((t) => ({ ...t, active: t.id === newTab.id })),
          activeTabId: newTab.id,
        };
      }
      return {
        tabs: [...state.tabs.map((t) => ({ ...t, active: false })), { ...newTab, active: true }],
        activeTabId: newTab.id,
      };
    }),

  closeTab: (id) =>
    set((state) => {
      const newTabs = state.tabs.filter((t) => t.id !== id);
      let newActiveTabId = state.activeTabId;

      if (state.activeTabId === id) {
        newActiveTabId = newTabs.length > 0 ? newTabs[newTabs.length - 1].id : null;
      }

      return {
        tabs: newTabs.map((t) => ({ ...t, active: t.id === newActiveTabId })),
        activeTabId: newActiveTabId,
      };
    }),

  switchTab: (id) =>
    set((state) => ({
      tabs: state.tabs.map((t) => ({ ...t, active: t.id === id })),
      activeTabId: id,
    })),

  updateTabContent: (id, content) =>
    set((state) => ({
      tabs: state.tabs.map((t) => (t.id === id ? { ...t, content } : t)),
    })),

  updateTabMetadata: (id, metadata) =>
    set((state) => ({
      tabs: state.tabs.map((t) =>
        t.id === id ? { ...t, metadata: { ...t.metadata, ...metadata } as ConsoleTabMetadata } : t
      ),
    })),

  reorderTabs: (sourceId, destinationId) =>
    set((state) => {
      const tabs = [...state.tabs];
      const fromIndex = tabs.findIndex((t) => t.id === sourceId);
      const toIndex = tabs.findIndex((t) => t.id === destinationId);
      if (fromIndex === -1 || toIndex === -1 || fromIndex === toIndex) return state;
      const [moved] = tabs.splice(fromIndex, 1);
      tabs.splice(toIndex, 0, moved);
      return { tabs };
    }),
}));
