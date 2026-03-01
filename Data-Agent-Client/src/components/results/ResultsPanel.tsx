import React, { useState, useEffect } from 'react';
import { Database, Download, Trash2, Maximize2 } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { ExecuteSqlMessage, ExecuteSqlResponse, ExecuteSqlResultSet } from '../../types/sql';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../hooks/useTheme';
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
  const { theme } = useTheme();
  const toast = useToast();
  const [activeTab, setActiveTab] = useState<'result' | 'output'>('output');

  const primaryResultSet: ExecuteSqlResultSet | null | undefined =
    executeResult?.resultSet ??
    executeResult?.results?.find(r => r.type === 'QUERY')?.resultSet;

  const resultHeaders =
    primaryResultSet?.columns?.map(c => c.label || c.name || '').filter(Boolean) ??
    executeResult?.headers ??
    [];

  const resultRows = primaryResultSet?.rows ?? executeResult?.rows ?? [];

  // Determine if Results tab should be shown (SELECT query with data)
  const hasResultTab = !!(
    executeResult?.success &&
    (executeResult?.query || executeResult?.type === 'QUERY' || !!primaryResultSet?.columns?.length)
  );

  // Can export when we have a result set (headers; rows may be empty)
  const canExport = hasResultTab && !!resultHeaders.length;

  const handleExport = () => {
    if (!canExport || !resultHeaders.length) {
      toast.warning(t(I18N_KEYS.COMMON.EXPORT_NO_DATA));
      return;
    }
    const csv = buildCsvFromResult(
      resultHeaders,
      resultRows ?? []
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
    const duration = executeResult.executionInfo?.durationMs ?? executeResult.executionTimeMs;
    const fetched = executeResult.executionInfo?.fetchRows ?? resultRows.length;
    const affected = executeResult.executionInfo?.affectedRows ?? executeResult.affectedRows;
    if (executeResult.query || executeResult.type === 'QUERY') {
      return `${fetched} rows | ${duration}ms`;
    }
    return `${affected} affected | ${duration}ms`;
  };

  const outputMessages: ExecuteSqlMessage[] =
    executeResult?.messages?.filter(m => m?.message) ??
    (executeResult?.errorMessage ? [{ level: 'ERROR', message: executeResult.errorMessage }] : []);

  const getMessageColor = (level?: string | null) => {
    if (level === 'ERROR') return 'text-red-400';
    if (level === 'WARN') return 'text-amber-400';
    return 'theme-text-secondary';
  };
  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;

  const formatTimestamp = (ts?: number | null) => {
    if (!ts) return '--';
    const d = new Date(ts);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };

  const executionStartTime = executeResult?.executionInfo?.startTime ?? null;
  const durationMs = executeResult?.executionInfo?.durationMs ?? executeResult?.executionTimeMs ?? 0;
  const executionMs = executeResult?.executionInfo?.executionMs ?? null;
  const fetchingMs = executeResult?.executionInfo?.fetchingMs ?? null;
  const dbPrefix = executeResult?.databaseName
    ? `${executeResult.databaseName}${executeResult.schemaName ? `.${executeResult.schemaName}` : ''}`
    : '';
  const sqlText = executeResult?.originalSql || executeResult?.executedSql || '';
  const timestampPrefix = `[${formatTimestamp(executionStartTime)}] `;
  const sqlContextPrefix = dbPrefix ? `${dbPrefix}> ` : '';
  const sqlPrefixText = `${timestampPrefix}${sqlContextPrefix}`;
  const sqlLines = sqlText
    ? sqlText
        .split('\n')
        .map((line, index) => (index === 0 ? line.trimEnd() : line.trimStart()))
    : [];
  const fetchRows = executeResult?.executionInfo?.fetchRows ?? resultRows.length;

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
        <div className="flex-1 overflow-hidden theme-bg-main relative">
          {activeTab === 'result' && hasResultTab && executeResult ? (
            // Results Table
            <div className="overflow-auto h-full">
              {resultHeaders.length > 0 ? (
                <table className="text-[11px] w-full border-collapse">
                  <thead className="sticky top-0 theme-bg-panel">
                    <tr>
                      {resultHeaders.map((h) => (
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
                    {resultRows && resultRows.length > 0 ? (
                      resultRows.map((row, i) => (
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
                        <td colSpan={resultHeaders.length} className="px-3 py-4 text-center theme-text-secondary opacity-50">
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
            <div className="p-3 font-sans text-[12px] leading-5 whitespace-pre-wrap space-y-1">
              {isRunning ? (
                <div className="flex items-center space-x-2 text-amber-500">
                  <span className="animate-pulse">●</span>
                  <span>Running...</span>
                </div>
              ) : executeResult ? (
                <>
                  {executeResult.success && (
                    <div className="theme-text-secondary">
                      [{formatTimestamp(executionStartTime)}] 已连接
                    </div>
                  )}
                  {sqlLines.length > 0 && (
                    <div className="space-y-0.5">
                      {sqlLines.map((line, index) => (
                        <div key={index} className="theme-text-secondary flex">
                          <span className={index === 0 ? 'theme-text-secondary' : 'theme-text-secondary opacity-0'} aria-hidden={index !== 0}>
                            {sqlPrefixText}
                          </span>
                          <SyntaxHighlighter
                            language="sql"
                            style={syntaxTheme}
                            PreTag="span"
                            CodeTag="span"
                            customStyle={{
                              background: 'transparent',
                              padding: 0,
                              margin: 0,
                              fontFamily: 'inherit',
                              fontSize: 'inherit',
                              lineHeight: 'inherit',
                            }}
                            codeTagProps={{
                              style: {
                                fontFamily: 'inherit',
                                fontSize: 'inherit',
                                lineHeight: 'inherit',
                                color: 'inherit',
                              }
                            }}
                          >
                            {line}
                          </SyntaxHighlighter>
                        </div>
                      ))}
                    </div>
                  )}
                  {executeResult.success && (
                    <div className="theme-text-secondary">
                      [{formatTimestamp(executionStartTime)}] 在 {durationMs} ms (execution: {executionMs ?? Math.max(0, durationMs - (fetchingMs ?? 0))} ms, fetching: {fetchingMs ?? 0} ms) 内检索到从 1 开始的 {fetchRows} 行
                    </div>
                  )}
                  {!executeResult.success && (
                    outputMessages.length > 0 ? (
                      outputMessages.map((m, i) => (
                        <div key={i} className={getMessageColor(m.level)}>
                          [{formatTimestamp(executionStartTime)}] {m.message}
                        </div>
                      ))
                    ) : (
                      <div className="text-red-400">
                        [{formatTimestamp(executionStartTime)}] {executeResult.errorMessage || 'Unknown error'}
                      </div>
                    )
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

