import React, { useEffect, useRef } from 'react';
import { 
  Panel, 
  Group as PanelGroup, 
  Separator as PanelResizeHandle,
  PanelImperativeHandle
} from 'react-resizable-panels';
import { DatabaseExplorer } from '../explorer/DatabaseExplorer';
import { AIAssistant } from '../ai/AIAssistant';

interface WorkspaceLayoutProps {
    children: React.ReactNode;
    showAI: boolean;
    onToggleAI: () => void;
    showExplorer: boolean;
    onToggleExplorer: () => void;
}

export function WorkspaceLayout({ children, showAI, showExplorer }: WorkspaceLayoutProps) {
    const explorerPanelRef = useRef<PanelImperativeHandle>(null);

    useEffect(() => {
        const panel = explorerPanelRef.current;
        if (panel) {
            if (showExplorer) {
                panel.expand();
            } else {
                panel.collapse();
            }
        }
    }, [showExplorer]);

    return (
        <div className="flex-1 flex overflow-hidden relative">
            <PanelGroup orientation="horizontal">
                {/* Left Sidebar: Database Explorer */}
                <Panel 
                    panelRef={explorerPanelRef}
                    defaultSize="20%" 
                    minSize="15%" 
                    maxSize="40%" 
                    collapsible={true}
                    className="theme-bg-panel flex flex-col overflow-hidden"
                >
                    <DatabaseExplorer />
                </Panel>

                <PanelResizeHandle className="w-1 bg-border hover:bg-primary/50 transition-colors" />

                {/* Main Content Area */}
                <Panel className="flex flex-col theme-bg-main min-w-0 relative animate-fade-in">
                    {children}
                </Panel>

                {showAI && (
                    <>
                        <PanelResizeHandle className="w-1 bg-border hover:bg-primary/50 transition-colors" />
                        {/* Right Sidebar: AI Assistant */}
                        <Panel defaultSize="30%" minSize="20%" maxSize="50%" className="theme-bg-panel flex flex-col overflow-hidden animate-in slide-in-from-right duration-300">
                            <AIAssistant />
                        </Panel>
                    </>
                )}
            </PanelGroup>
        </div>
    );
}
