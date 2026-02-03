import React, { useState } from 'react';
import { Database, Download, Trash2, Maximize2 } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useTranslation } from 'react-i18next';
import { 
  Panel, 
  Group as PanelGroup, 
  Separator as PanelResizeHandle 
} from 'react-resizable-panels';

interface ResultsPanelProps {
  isVisible: boolean;
  onClose: () => void;
  hasResults?: boolean;
  children: React.ReactNode;
}

export function ResultsPanel({ isVisible, onClose, hasResults = false, children }: ResultsPanelProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<'result' | 'output'>(hasResults ? 'result' : 'output');

  if (!isVisible) {
    return (
      <PanelGroup orientation="vertical">
        <Panel className="flex flex-col min-h-0 relative">
          {children}
        </Panel>
      </PanelGroup>
    );
  }

  return (
    <PanelGroup orientation="vertical">
      <Panel className="flex flex-col min-h-0 relative">
        {children}
      </Panel>

      <PanelResizeHandle className="h-1 bg-border hover:bg-primary/50 transition-colors" />

      <Panel defaultSize="30%" minSize="15%" className="theme-bg-panel flex flex-col shrink-0 border-t theme-border relative">
        {/* Toolbar */}
        <div className="flex items-center h-9 px-2 border-b theme-border shrink-0">
          <div className="flex space-x-1 h-full">
            {hasResults && (
              <button 
                onClick={() => setActiveTab('result')}
                className={cn(
                  "px-3 py-1 text-[11px] font-medium transition-colors relative h-full flex items-center",
                  activeTab === 'result' 
                    ? "theme-text-primary after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-primary" 
                    : "theme-text-secondary hover:theme-text-primary"
                )}
              >
                {t('common.result')} 1
              </button>
            )}
            <button 
              onClick={() => setActiveTab('output')}
              className={cn(
                "px-3 py-1 text-[11px] font-medium transition-colors relative h-full flex items-center",
                (activeTab === 'output' || !hasResults)
                  ? "theme-text-primary after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-primary" 
                  : "theme-text-secondary hover:theme-text-primary"
              )}
            >
              {t('common.output')}
            </button>
          </div>

          <div className="flex-1" />

          <div className="flex items-center space-x-2 px-2">
            <button 
              onClick={onClose}
              className="p-1 hover:bg-accent rounded theme-text-secondary hover:theme-text-primary" 
              title={t('common.close_panel')}
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
            <button className="p-1 hover:bg-accent rounded theme-text-secondary hover:theme-text-primary" title={t('common.export')}>
              <Download className="w-3.5 h-3.5" />
            </button>
            <button className="p-1 hover:bg-accent rounded theme-text-secondary hover:theme-text-primary" title={t('common.maximize')}>
              <Maximize2 className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Content Area */}
        <div className="flex-1 overflow-auto theme-bg-main relative">
          {(activeTab === 'result' && hasResults) ? (
            <div className="h-full w-full flex flex-col items-center justify-center text-xs theme-text-secondary opacity-50 space-y-2">
              <Database className="w-8 h-8 opacity-20" />
              <span>{t('common.ready_hint')}</span>
            </div>
          ) : (
            <div className="p-3 font-mono text-[11px] theme-text-secondary whitespace-pre-wrap">
              <div className="text-gray-500 mb-2">-- {t('common.output')} Console --</div>
              <div className="text-green-500 opacity-70">{t('common.output_console_connected')}</div>
            </div>
          )}
        </div>

        {/* Status Bar */}
        <div className="h-6 border-t theme-border flex items-center px-2 text-[10px] theme-text-secondary justify-between shrink-0">
          <div className="flex items-center space-x-4">
            <span>{/* 0 rows retrieved */}</span>
            <span>{/* 42ms execution time */}</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="flex items-center">
              <span className={cn("w-2 h-2 rounded-full mr-1.5", "bg-gray-500")} />
              {t('common.disconnected')}
            </span>
            <span className="opacity-50">|</span>
            <span>{t('common.readonly')}</span>
          </div>
        </div>
      </Panel>
    </PanelGroup>
  );
}

