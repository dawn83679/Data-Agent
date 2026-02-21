import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { DbTypeOption } from '../types/dbType';
import { dbTypeService } from '../services/dbType.service';

export type TabType = 'file' | 'table';
// ... (rest of imports/types)

export interface Tab {
    id: string;
    name: string;
    type: TabType;
    icon?: string;
    content?: string;
    active: boolean;
    metadata?: any;
}

export type ResultBehavior = 'multi' | 'overwrite';
export type TableDblClickMode = 'table' | 'console';
export type TableDblClickConsoleTarget = 'reuse' | 'new';

/** Current connection/database/schema for SQL execution in the middle editor. */
export interface SqlContext {
    connectionId: number | null;
    databaseName: string | null;
    schemaName: string | null;
}

interface PreferenceState {
    resultBehavior: ResultBehavior;
    tableDblClickMode: TableDblClickMode;
    tableDblClickConsoleTarget: TableDblClickConsoleTarget;
    aiAutoSelect: boolean;
    aiAutoWrite: boolean;
    aiWriteTransaction: boolean;
    aiMaxRetries: number;
}

interface WorkspaceState extends PreferenceState {
    tabs: Tab[];
    activeTabId: string | null;
    isSettingsModalOpen: boolean;
    supportedDbTypes: DbTypeOption[];
    supportedDbTypesLoading: boolean;
    /** Current connection/database/schema for SQL execution. */
    sqlContext: SqlContext;

    // Actions
    openTab: (tab: Omit<Tab, 'active'>) => void;
    closeTab: (id: string) => void;
    switchTab: (id: string) => void;
    updateTabContent: (id: string, content: string) => void;
    setSettingsModalOpen: (open: boolean) => void;
    updatePreferences: (prefs: Partial<PreferenceState>) => void;
    resetPreferences: () => void;
    fetchSupportedDbTypes: () => Promise<void>;
    setSqlContext: (ctx: Partial<SqlContext>) => void;
}

const DEFAULT_PREFERENCES: PreferenceState = {
    resultBehavior: 'multi',
    tableDblClickMode: 'table',
    tableDblClickConsoleTarget: 'reuse',
    aiAutoSelect: true,
    aiAutoWrite: false,
    aiWriteTransaction: true,
    aiMaxRetries: 3,
};

export const useWorkspaceStore = create<WorkspaceState>()(
    persist(
        (set, get) => ({
            ...DEFAULT_PREFERENCES,
            tabs: [],
            activeTabId: null,
            isSettingsModalOpen: false,
            supportedDbTypes: [],
            supportedDbTypesLoading: false,
            sqlContext: { connectionId: null, databaseName: null, schemaName: null },

            openTab: (newTab) => set((state) => {
                const existingTab = state.tabs.find(t => t.id === newTab.id);
                if (existingTab) {
                    return {
                        tabs: state.tabs.map(t => ({ ...t, active: t.id === newTab.id })),
                        activeTabId: newTab.id
                    };
                }
                return {
                    tabs: [...state.tabs.map(t => ({ ...t, active: false })), { ...newTab, active: true }],
                    activeTabId: newTab.id
                };
            }),

            closeTab: (id) => set((state) => {
                const newTabs = state.tabs.filter(t => t.id !== id);
                let newActiveTabId = state.activeTabId;

                if (state.activeTabId === id) {
                    newActiveTabId = newTabs.length > 0 ? newTabs[newTabs.length - 1].id : null;
                }

                return {
                    tabs: newTabs.map(t => ({ ...t, active: t.id === newActiveTabId })),
                    activeTabId: newActiveTabId
                };
            }),

            switchTab: (id) => set((state) => ({
                tabs: state.tabs.map(t => ({ ...t, active: t.id === id })),
                activeTabId: id
            })),

            updateTabContent: (id, content) => set((state) => ({
                tabs: state.tabs.map(t => t.id === id ? { ...t, content } : t)
            })),

            setSettingsModalOpen: (open) => set({ isSettingsModalOpen: open }),

            updatePreferences: (prefs) => set((state) => ({ ...state, ...prefs })),

            resetPreferences: () => set((state) => ({ ...state, ...DEFAULT_PREFERENCES })),

            setSqlContext: (ctx) => set((state) => ({
                sqlContext: {
                    connectionId: ctx.connectionId !== undefined ? ctx.connectionId : state.sqlContext.connectionId,
                    databaseName: ctx.databaseName !== undefined ? ctx.databaseName : state.sqlContext.databaseName,
                    schemaName: ctx.schemaName !== undefined ? ctx.schemaName : state.sqlContext.schemaName,
                },
            })),

            fetchSupportedDbTypes: async () => {
                const state = get();
                if (state.supportedDbTypesLoading || state.supportedDbTypes.length > 0) return;
                set({ supportedDbTypesLoading: true });
                try {
                    const data = await dbTypeService.getSupportedDbTypes();
                    set({ supportedDbTypes: data || [] });
                } catch (error) {
                    console.error('Failed to fetch supported db types:', error);
                } finally {
                    set({ supportedDbTypesLoading: false });
                }
            },
        }),
        {
            name: 'data-agent-workspace-storage',
            partialize: (state) => ({
                resultBehavior: state.resultBehavior,
                tableDblClickMode: state.tableDblClickMode,
                tableDblClickConsoleTarget: state.tableDblClickConsoleTarget,
                aiAutoSelect: state.aiAutoSelect,
                aiAutoWrite: state.aiAutoWrite,
                aiWriteTransaction: state.aiWriteTransaction,
                aiMaxRetries: state.aiMaxRetries,
                // tabs: state.tabs, // Optional: persist tabs
                // activeTabId: state.activeTabId,
            }),
        }
    )
);
