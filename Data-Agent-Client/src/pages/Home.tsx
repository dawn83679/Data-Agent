import { useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { TabBar } from "../components/workspace/TabBar";
import { MonacoEditor, type MonacoEditorHandle } from "../components/editor/MonacoEditor";
import { ResultsPanel } from "../components/results/ResultsPanel";
import { TableDataTab } from "../components/results/TableDataTab";
import { Toolbar } from "../components/workspace/Toolbar";
import { EmptyState } from "../components/workspace/EmptyState";
import { useWorkspaceStore } from "../store/workspaceStore";
import type { ExecuteSqlResponse } from "../types/sql";
import type { TableDataTabMetadata } from "../types/tab";
import { sqlExecutionService } from "../services/sqlExecution.service";
import { I18N_KEYS } from "../constants/i18nKeys";
import { FileText, Minus, Plus } from "lucide-react";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "../components/ui/Tooltip";

export default function Home() {
    const { t } = useTranslation();
    const { tabs, activeTabId, updateTabContent, updateTabMetadata } = useWorkspaceStore();
    const editorRef = useRef<MonacoEditorHandle | null>(null);
    const [isResultsVisible, setIsResultsVisible] = useState(false);
    const [executeResult, setExecuteResult] = useState<ExecuteSqlResponse | null>(null);
    const [isRunning, setIsRunning] = useState(false);

    const activeTab = tabs.find(t => t.id === activeTabId);
    const sqlContext = activeTab?.metadata;
    const tableDataMetadata = activeTab?.type === 'tableData'
        ? activeTab.metadata as TableDataTabMetadata
        : null;

    const handleRunQuery = useCallback(async () => {
        const sql = editorRef.current?.getSelectionOrAllContent().trim()
                    ?? activeTab?.content?.trim() ?? '';
        if (!sql || !sqlContext?.connectionId) return;

        setIsRunning(true);
        setExecuteResult(null);
        setIsResultsVisible(true);

        try {
            const result = await sqlExecutionService.executeSql({
                connectionId: sqlContext.connectionId,
                databaseName: sqlContext.databaseName ?? undefined,
                schemaName: sqlContext.schemaName ?? undefined,
                sql,
            });
            setExecuteResult({
                ...result,
                originalSql: result.originalSql || sql,
                executedSql: result.executedSql || sql,
            });
        } catch (err: unknown) {
            setExecuteResult({
                success: false,
                errorMessage: String(err),
                executionTimeMs: 0,
                originalSql: sql,
                executedSql: sql,
                query: false,
                affectedRows: 0,
            });
        } finally {
            setIsRunning(false);
        }
    }, [activeTab, sqlContext]);

    // SQL Editor Shortcuts
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                handleRunQuery();
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [handleRunQuery]);

    return (
        <div className="flex-1 flex flex-col min-w-0 h-full overflow-hidden">
            {/* Tab Bar */}
            <TabBar />

            {/* Workspace Area */}
            <ResultsPanel
                isVisible={isResultsVisible}
                onClose={() => setIsResultsVisible(false)}
                executeResult={executeResult}
                isRunning={isRunning}
            >
                <div className="flex-1 flex flex-col min-h-0 relative">
                    {activeTab && (
                        <div className="h-8 flex items-center px-2 theme-bg-main border-b theme-border text-[10px] theme-text-secondary shrink-0 gap-1 overflow-visible">
                            <Toolbar
                                onRun={handleRunQuery}
                                onStop={() => setIsRunning(false)}
                                isRunning={isRunning}
                                connectionId={sqlContext?.connectionId}
                                currentDatabase={sqlContext?.databaseName}
                                onDatabaseChange={(db) =>
                                    updateTabMetadata(activeTab.id, { databaseName: db })
                                }
                                extraActions={
                                    activeTab.type === 'tableData' ? (
                                        <TooltipProvider>
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <button
                                                        type="button"
                                                        className="h-6 px-2 rounded flex items-center gap-1 text-[11px] hover:bg-accent/30 transition-colors"
                                                    >
                                                        <Plus className="w-3.5 h-3.5" />
                                                    </button>
                                                </TooltipTrigger>
                                                <TooltipContent className="text-[10px] theme-bg-panel theme-text-secondary border theme-border px-1.5 py-0.5">
                                                    新增行
                                                </TooltipContent>
                                            </Tooltip>
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <button
                                                        type="button"
                                                        className="h-6 px-2 rounded flex items-center gap-1 text-[11px] hover:bg-accent/30 transition-colors"
                                                    >
                                                        <Minus className="w-3.5 h-3.5" />
                                                    </button>
                                                </TooltipTrigger>
                                                <TooltipContent className="text-[10px] theme-bg-panel theme-text-secondary border theme-border px-1.5 py-0.5">
                                                    删除行
                                                </TooltipContent>
                                            </Tooltip>
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <button
                                                        type="button"
                                                        className="h-6 px-2 rounded flex items-center gap-1 text-[11px] hover:bg-accent/30 transition-colors"
                                                    >
                                                        <FileText className="w-3.5 h-3.5" />
                                                        DDL
                                                    </button>
                                                </TooltipTrigger>
                                                <TooltipContent className="text-[10px] theme-bg-panel theme-text-secondary border theme-border px-1.5 py-0.5">
                                                    DDL
                                                </TooltipContent>
                                            </Tooltip>
                                        </TooltipProvider>
                                    ) : null
                                }
                            />
                        </div>
                    )}

                    <div className="flex-1 min-h-0 relative overflow-hidden flex flex-col">
                        <div className="flex-1 min-h-0 relative overflow-hidden">
                            {activeTab?.type === 'file' ? (
                                <MonacoEditor
                                    ref={editorRef}
                                    value={activeTab.content || ''}
                                    onChange={(val) => updateTabContent(activeTab.id, val || '')}
                                />
                            ) : activeTab?.type === 'tableData' && tableDataMetadata ? (
                                <TableDataTab
                                    metadata={tableDataMetadata}
                                    onMetadataChange={(metadata) => updateTabMetadata(activeTab.id, metadata)}
                                />
                            ) : activeTab?.type === 'table' ? (
                                <div className="flex-1 h-full flex items-center justify-center theme-text-secondary italic text-xs">
                                    -- {t(I18N_KEYS.WORKSPACE.DATA_GRID_PLACEHOLDER)} --
                                </div>
                            ) : (
                                <EmptyState />
                            )}
                        </div>
                    </div>
                </div>
            </ResultsPanel>
        </div>
    );
}
