import React, { useState, useEffect } from 'react';
import { Database, Download, Trash2, Maximize2 } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { ExecuteSqlResponse } from '../../types/sql';
import { buildCsvFromResult, downloadCsv } from '../../utils/exportResult';
import { useToast } from '../../hooks/useToast';
import {
  Panel,
  Group as PanelGroup,
  Separator as PanelResizeHandle
} from 'react-resizable-panels';

interface ResultsPanelProps {
  isVisible: boolean;
  onClose: () => void;
  executeResult?: ExecuteSqlResponse | null;
  isRunning?: boolean;
  children: React.ReactNode;
}

export function ResultsPanel({ isVisible, onClose, executeResult, isRunning = false, children }: ResultsPanelProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const [activeTab, setActiveTab] = useState<'result' | 'output'>('output');

  // Determine if Results tab should be shown (SELECT query with data)
  const hasResultTab = !!(executeResult?.success && executeResult?.query);

  // Can export when we have a result set (headers; rows may be empty)
  const canExport = hasResultTab && !!executeResult?.headers?.length;

  const handleExport = () => {
    if (!canExport || !executeResult?.headers) {
      toast.warning(t(I18N_KEYS.COMMON.EXPORT_NO_DATA));
      return;
    }
    const csv = buildCsvFromResult(
      executeResult.headers,
      executeResult.rows ?? []
    );
    downloadCsv(csv);
    toast.success(t(I18N_KEYS.COMMON.EXPORT_SUCCESS));
  };

  // Auto-switch to Results tab when SELECT completes
  useEffect(() => {
    if (executeResult) {
      setActiveTab(hasResultTab ? 'result' : 'output');
    }
  }, [executeResult, hasResultTab]);

  const getStatusIndicatorColor = () => {
    if (isRunning) return 'bg-amber-500 animate-pulse';
    if (!executeResult) return 'bg-gray-500';
    if (executeResult.success) return 'bg-green-500';
    return 'bg-red-500';
  };

  const getStatusText = () => {
    if (isRunning) return 'Running...';
    if (!executeResult) return 'Ready';
    if (!executeResult.success) return 'Error';
    if (executeResult.query && executeResult.rows) {
      return `${executeResult.rows.length} rows | ${executeResult.executionTimeMs}ms`;
    }
    return `${executeResult.affectedRows} affected | ${executeResult.executionTimeMs}ms`;
  };

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
            {hasResultTab && (
              <button
                onClick={() => setActiveTab('result')}
                className={cn(
                  "px-3 py-1 text-[11px] font-medium transition-colors relative h-full flex items-center",
                  activeTab === 'result'
                    ? "theme-text-primary after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-primary"
                    : "theme-text-secondary hover:theme-text-primary"
                )}
              >
                Results
              </button>
            )}
            <button
              onClick={() => setActiveTab('output')}
              className={cn(
                "px-3 py-1 text-[11px] font-medium transition-colors relative h-full flex items-center",
                (activeTab === 'output' || !hasResultTab)
                  ? "theme-text-primary after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-primary"
                  : "theme-text-secondary hover:theme-text-primary"
              )}
            >
              Output
            </button>
          </div>

          <div className="flex-1" />

          <div className="flex items-center space-x-2 px-2">
            <button
              onClick={onClose}
              className="p-1 hover:bg-accent rounded theme-text-secondary hover:theme-text-primary"
              title={t(I18N_KEYS.COMMON.CLOSE_PANEL)}
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={handleExport}
              disabled={!canExport}
              className={cn(
                "p-1 rounded theme-text-secondary",
                canExport
                  ? "hover:bg-accent hover:theme-text-primary"
                  : "opacity-50 cursor-not-allowed"
              )}
              title={canExport ? t(I18N_KEYS.COMMON.EXPORT) : t(I18N_KEYS.COMMON.EXPORT_NO_DATA)}
            >
              <Download className="w-3.5 h-3.5" />
            </button>
            <button className="p-1 hover:bg-accent rounded theme-text-secondary hover:theme-text-primary" title={t(I18N_KEYS.COMMON.MAXIMIZE)}>
              <Maximize2 className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Content Area */}
        <div className="flex-1 overflow-auto theme-bg-main relative">
          {activeTab === 'result' && hasResultTab && executeResult ? (
            // Results Table
            <div className="overflow-auto h-full">
              {executeResult.headers && executeResult.headers.length > 0 ? (
                <table className="text-[11px] w-full border-collapse">
                  <thead className="sticky top-0 theme-bg-panel">
                    <tr>
                      {executeResult.headers.map((h) => (
                        <th
                          key={h}
                          className="px-3 py-1.5 text-left font-medium theme-text-secondary border-b border-r theme-border whitespace-nowrap"
                        >
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {executeResult.rows && executeResult.rows.length > 0 ? (
                      executeResult.rows.map((row, i) => (
                        <tr key={i} className="hover:bg-accent/30 border-b theme-border">
                          {row.map((cell, j) => (
                            <td
                              key={j}
                              className="px-3 py-1 border-r theme-border theme-text-primary truncate max-w-[300px]"
                              title={cell == null ? 'NULL' : String(cell)}
                            >
                              {cell == null ? (
                                <span className="text-gray-500 italic">NULL</span>
                              ) : (
                                String(cell)
                              )}
                            </td>
                          ))}
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={executeResult.headers.length} className="px-3 py-4 text-center theme-text-secondary opacity-50">
                          No data
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              ) : (
                <div className="h-full w-full flex flex-col items-center justify-center text-xs theme-text-secondary opacity-50 space-y-2">
                  <Database className="w-8 h-8 opacity-20" />
                  <span>{t(I18N_KEYS.COMMON.READY_HINT)}</span>
                </div>
              )}
            </div>
          ) : (
            // Output Log
            <div className="p-3 font-mono text-[11px] whitespace-pre-wrap">
              {isRunning ? (
                <div className="flex items-center space-x-2 text-amber-500">
                  <span className="animate-pulse">●</span>
                  <span>Running...</span>
                </div>
              ) : executeResult ? (
                <>
                  {executeResult.success ? (
                    <div className="text-green-500">
                      {executeResult.query ? (
                        <>✓ {executeResult.rows?.length || 0} rows retrieved in {executeResult.executionTimeMs}ms</>
                      ) : (
                        <>✓ {executeResult.affectedRows} row(s) affected in {executeResult.executionTimeMs}ms</>
                      )}
                    </div>
                  ) : (
                    <div className="text-red-400">
                      ✕ {executeResult.errorMessage || 'Unknown error'}
                    </div>
                  )}
                </>
              ) : (
                <div className="text-gray-500 opacity-50">-- Output Console --</div>
              )}
            </div>
          )}
        </div>

        {/* Status Bar */}
        <div className="h-6 border-t theme-border flex items-center px-2 text-[10px] theme-text-secondary justify-between shrink-0">
          <div className="flex items-center space-x-2">
            <span className="flex items-center">
              <span className={cn('w-2 h-2 rounded-full mr-1.5', getStatusIndicatorColor())} />
              {getStatusText()}
            </span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="opacity-50">|</span>
            <span>{t(I18N_KEYS.COMMON.READONLY)}</span>
          </div>
        </div>
      </Panel>
    </PanelGroup>
  );
}

