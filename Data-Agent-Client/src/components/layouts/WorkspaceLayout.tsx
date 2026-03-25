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

export function WorkspaceLayout({ children, showAI, onToggleAI, showExplorer }: WorkspaceLayoutProps) {
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
        <div className="workbench-shell flex-1 min-h-0 relative">
            <PanelGroup orientation="horizontal" className="workbench-shell__group">
                {/* Left Sidebar: Database Explorer */}
                <Panel 
                    panelRef={explorerPanelRef}
                    defaultSize="22%"
                    minSize="16%"
                    maxSize="32%"
                    collapsible={true}
                    className="workbench-panel workbench-panel--sidebar"
                >
                    <DatabaseExplorer />
                </Panel>

                <PanelResizeHandle className="workbench-resize-handle" />

                {/* Main Content Area */}
                <Panel className="workbench-panel workbench-panel--main min-w-0 relative animate-fade-in">
                    {children}
                </Panel>
                {showAI && (
                    <>
                        <PanelResizeHandle className="workbench-resize-handle" />
                        {/* Right Sidebar: AI Assistant */}
                        <Panel
                            defaultSize="34%"
                            minSize="24%"
                            maxSize="42%"
                            className="workbench-panel workbench-panel--assistant animate-in slide-in-from-right duration-300"
                        >
                            <AIAssistant onClosePanel={onToggleAI} />
                        </Panel>
                    </>
                )}
            </PanelGroup>
        </div>
    );
}
