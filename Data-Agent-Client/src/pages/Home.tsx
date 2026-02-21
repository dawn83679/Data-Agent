import { useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { TabBar } from "../components/workspace/TabBar";
import { MonacoEditor, type MonacoEditorHandle } from "../components/editor/MonacoEditor";
import { ResultsPanel } from "../components/results/ResultsPanel";
import { Breadcrumbs } from "../components/workspace/Breadcrumbs";
import { Toolbar } from "../components/workspace/Toolbar";
import { EmptyState } from "../components/workspace/EmptyState";
import { useWorkspaceStore } from "../store/workspaceStore";
import { sqlExecutionService } from "../services/sqlExecution.service";
import { formatSql } from "../utils/sql";
import type { ExecuteSqlResponse } from "../types/sql";

export default function Home() {
    const { t } = useTranslation();
    const { tabs, activeTabId, updateTabContent, sqlContext } = useWorkspaceStore();
    const [isResultsVisible, setIsResultsVisible] = useState(false);
    const [executeResult, setExecuteResult] = useState<ExecuteSqlResponse | null>(null);
    const [isRunning, setIsRunning] = useState(false);
    const editorRef = useRef<MonacoEditorHandle | null>(null);

    const activeTab = tabs.find(tab => tab.id === activeTabId);
    const hasResults = executeResult !== null;

    const getSqlToRun = useCallback((): string => {
        if (editorRef.current) {
            return editorRef.current.getSelectionOrAllContent().trim();
        }
        if (activeTab?.type === 'file' && activeTab.content) {
            return activeTab.content.trim();
        }
        return '';
    }, [activeTab]);

    const handleRunQuery = useCallback(async () => {
        const sql = getSqlToRun();
        if (!sql) return;

        if (sqlContext.connectionId == null) {
            alert(t('common.select_connection_first'));
            return;
        }

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
            setExecuteResult(result);
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : String(err);
            setExecuteResult({
                success: false,
                errorMessage: message,
                executionTimeMs: 0,
                query: false,
                affectedRows: 0,
            });
        } finally {
            setIsRunning(false);
        }
    }, [getSqlToRun, sqlContext.connectionId, sqlContext.databaseName, sqlContext.schemaName, t]);

    const handleFormatSql = useCallback(() => {
        if (editorRef.current && activeTab?.type === 'file') {
            const current = activeTab.content || '';
            const formatted = formatSql(current);
            if (formatted !== current) {
                editorRef.current.setValue(formatted);
                updateTabContent(activeTab.id, formatted);
            }
        }
    }, [activeTab, updateTabContent]);

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
            <TabBar />

            <ResultsPanel
                isVisible={isResultsVisible}
                onClose={() => setIsResultsVisible(false)}
                hasResults={hasResults}
                executeResult={executeResult}
                isRunning={isRunning}
            >
                <div className="flex-1 flex flex-col min-h-0 relative">
                    {activeTab && (
                        <div className="h-8 flex items-center px-4 theme-bg-main border-b theme-border text-[10px] theme-text-secondary shrink-0 justify-between">
                            <Breadcrumbs activeTabName={activeTab.name} />
                            <Toolbar onRun={handleRunQuery} onFormat={handleFormatSql} isRunning={isRunning} />
                        </div>
                    )}

                    <div className="flex-1 relative overflow-hidden flex flex-col">
                        <div className="flex-1 relative overflow-hidden">
                            {activeTab?.type === 'file' ? (
                                <MonacoEditor
                                    ref={editorRef}
                                    value={activeTab.content || ''}
                                    onChange={(val) => updateTabContent(activeTab.id, val || '')}
                                />
                            ) : activeTab?.type === 'table' ? (
                                <div className="flex-1 h-full flex items-center justify-center theme-text-secondary italic text-xs">
                                    -- {t('workspace.data_grid_placeholder')} --
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
