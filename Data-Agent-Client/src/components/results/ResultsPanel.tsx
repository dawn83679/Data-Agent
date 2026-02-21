import React, { useState } from 'react';
import { Database, Download, Trash2, Maximize2, AlertCircle } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useTranslation } from 'react-i18next';
import { 
  Panel, 
  Group as PanelGroup, 
  Separator as PanelResizeHandle 
} from 'react-resizable-panels';
import type { ExecuteSqlResponse } from '../../types/sql';

interface ResultsPanelProps {
  isVisible: boolean;
  onClose: () => void;
  hasResults?: boolean;
  executeResult?: ExecuteSqlResponse | null;
  isRunning?: boolean;
  children: React.ReactNode;
}

export function ResultsPanel({ isVisible, onClose, hasResults = false, executeResult = null, isRunning = false, children }: ResultsPanelProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<'result' | 'output'>(hasResults ? 'result' : 'output');

  const success = executeResult?.success ?? false;
  const isQuery = executeResult?.query ?? false;
  const headers = executeResult?.headers ?? [];
  const rows = executeResult?.rows ?? [];
  const rowCount = isQuery ? rows.length : (executeResult?.affectedRows ?? 0);
  const execMs = executeResult?.executionTimeMs ?? 0;
  const errorMsg = executeResult?.errorMessage;

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
          {activeTab === 'result' && hasResults ? (
            <>
              {isRunning && (
                <div className="absolute inset-0 flex items-center justify-center theme-bg-main/80 z-10">
                  <span className="text-xs theme-text-secondary">{t('common.run')}...</span>
                </div>
              )}
              {!success && errorMsg && (
                <div className="p-3 flex items-start gap-2 text-red-500 text-xs">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <pre className="whitespace-pre-wrap break-words">{errorMsg}</pre>
                </div>
              )}
              {success && isQuery && headers.length > 0 && (
                <div className="overflow-auto">
                  <table className="w-full border-collapse text-[11px]">
                    <thead>
                      <tr className="theme-bg-panel border-b theme-border">
                        {headers.map((h, i) => (
                          <th key={i} className="text-left px-2 py-1.5 font-medium theme-text-primary whitespace-nowrap">
                            {String(h)}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((row, ri) => (
                        <tr key={ri} className="border-b theme-border hover:theme-bg-panel/50">
                          {row.map((cell, ci) => (
                            <td key={ci} className="px-2 py-1 theme-text-secondary whitespace-nowrap max-w-[200px] truncate" title={String(cell ?? '')}>
                              {cell != null ? String(cell) : 'NULL'}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {success && isQuery && headers.length === 0 && rows.length === 0 && (
                <div className="h-full flex flex-col items-center justify-center text-xs theme-text-secondary opacity-50 space-y-2">
                  <Database className="w-8 h-8 opacity-20" />
                  <span>{t('common.ready_hint')}</span>
                </div>
              )}
              {success && !isQuery && (
                <div className="p-3 text-xs theme-text-secondary">
                  {t('common.affected_rows', { count: executeResult?.affectedRows ?? 0 })}
                </div>
              )}
            </>
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
            {hasResults && success && (
              <>
                <span>{isQuery ? t('common.rows_retrieved', { count: rowCount }) : t('common.affected_rows', { count: rowCount })}</span>
                <span>{t('common.execution_time_ms', { ms: execMs })}</span>
              </>
            )}
          </div>
          <div className="flex items-center space-x-2">
            <span className="flex items-center">
              <span
                className={cn(
                  'w-2 h-2 rounded-full mr-1.5',
                  isRunning ? 'bg-amber-500 animate-pulse' : hasResults && success ? 'bg-green-500' : hasResults && !success ? 'bg-red-500' : 'bg-gray-500'
                )}
              />
              {isRunning ? t('common.run') + '...' : hasResults && success ? t('common.connected') : hasResults && !success ? 'Error' : t('common.disconnected')}
            </span>
            <span className="opacity-50">|</span>
            <span>{t('common.readonly')}</span>
          </div>
        </div>
      </Panel>
    </PanelGroup>
  );
}

