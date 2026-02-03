import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { TabBar } from "../components/workspace/TabBar";
import { MonacoEditor } from "../components/editor/MonacoEditor";
import { ResultsPanel } from "../components/results/ResultsPanel";
import { Breadcrumbs } from "../components/workspace/Breadcrumbs";
import { Toolbar } from "../components/workspace/Toolbar";
import { EmptyState } from "../components/workspace/EmptyState";
import { useWorkspaceStore } from "../store/workspaceStore";

export default function Home() {
    const { t } = useTranslation();
    const { tabs, activeTabId, updateTabContent } = useWorkspaceStore();
    const [isResultsVisible, setIsResultsVisible] = useState(false);
    const [hasResults, setHasResults] = useState(false);

    const activeTab = tabs.find(t => t.id === activeTabId);

    const handleRunQuery = () => {
        console.log("Running query...");
        setIsResultsVisible(true);
        setHasResults(true);
    };

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
    }, []);

    return (
        <div className="flex-1 flex flex-col min-w-0 h-full overflow-hidden">
            {/* Tab Bar */}
            <TabBar />

            {/* Workspace Area */}
            <ResultsPanel 
                isVisible={isResultsVisible}
                onClose={() => setIsResultsVisible(false)}
                hasResults={hasResults}
            >
                <div className="flex-1 flex flex-col min-h-0 relative">
                    {activeTab && (
                        <div className="h-8 flex items-center px-4 theme-bg-main border-b theme-border text-[10px] theme-text-secondary shrink-0 justify-between">
                            <Breadcrumbs activeTabName={activeTab.name} />
                            <Toolbar onRun={handleRunQuery} />
                        </div>
                    )}
                    
                    <div className="flex-1 relative overflow-hidden flex flex-col">
                        <div className="flex-1 relative overflow-hidden">
                            {activeTab?.type === 'file' ? (
                                <MonacoEditor 
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
