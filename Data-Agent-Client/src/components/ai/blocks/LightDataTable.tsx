import React from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { useToast } from '../../../hooks/useToast';

export interface LightDataTableCell {
  text: string;
  title?: string;
  isNull?: boolean;
}

interface LightDataTableProps {
  headers: string[];
  rows: LightDataTableCell[][];
  enableActions?: boolean;
  children?: React.ReactNode;
  emptyText?: string;
  headerStart?: React.ReactNode;
  alwaysShowActions?: boolean;
}

function escapeMarkdownTableCell(value: string): string {
  return value
    .replace(/\\/g, '\\\\')
    .replace(/\|/g, '\\|')
    .replace(/\r?\n/g, '<br/>');
}

function buildMarkdownTable(headers: string[], rows: string[][]): string {
  const normalizedHeaders = headers.map(escapeMarkdownTableCell);
  const separator = normalizedHeaders.map(() => '---');
  const body = rows.map((row) =>
    row.map((cell) => escapeMarkdownTableCell(cell ?? '')).join(' | ')
  );

  return [
    `| ${normalizedHeaders.join(' | ')} |`,
    `| ${separator.join(' | ')} |`,
    ...body.map((row) => `| ${row} |`),
  ].join('\n');
}

function defaultTableFilename(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  const h = String(now.getHours()).padStart(2, '0');
  const min = String(now.getMinutes()).padStart(2, '0');
  const sec = String(now.getSeconds()).padStart(2, '0');
  return `table-${y}-${m}-${d}-${h}${min}${sec}.md`;
}

function downloadMarkdown(content: string, filename?: string): void {
  const name = filename ?? defaultTableFilename();
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = name;
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}

export function LightDataTable({
  headers,
  rows,
  enableActions = true,
  children,
  emptyText,
  headerStart,
  alwaysShowActions = false,
}: LightDataTableProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const hasTableData = headers.length > 0;
  const markdownRows = rows.map((row) => row.map((cell) => cell.text));
  const resolvedEmptyText = emptyText ?? t(I18N_KEYS.COMMON.NO_DATA);
  const showTopBar = !!headerStart || (enableActions && hasTableData);

  const handleCopy = async () => {
    if (!hasTableData) {
      return;
    }
    try {
      await navigator.clipboard.writeText(buildMarkdownTable(headers, markdownRows));
      toast.success(t(I18N_KEYS.AI.TABLE_ACTIONS.COPIED));
    } catch {
      toast.error(t(I18N_KEYS.AI.TABLE_ACTIONS.COPY_TABLE));
    }
  };

  const handleDownload = () => {
    if (!hasTableData) {
      return;
    }
    try {
      downloadMarkdown(buildMarkdownTable(headers, markdownRows), defaultTableFilename());
      toast.success(t(I18N_KEYS.AI.TABLE_ACTIONS.DOWNLOAD_SUCCESS));
    } catch {
      toast.error(t(I18N_KEYS.AI.TABLE_ACTIONS.DOWNLOAD_FAILED));
    }
  };

  return (
    <div className="group/table my-2">
      {showTopBar && (
        <div className="flex items-center justify-between gap-2 rounded-t-md border border-b-0 theme-border bg-[color:var(--bg-panel)] px-3 py-2 text-[10px]">
          <div className="min-w-0 flex-1 theme-text-secondary">
            {headerStart}
          </div>
          {enableActions && hasTableData && (
            <div
              className={
                alwaysShowActions
                  ? 'flex items-center gap-1.5'
                  : 'pointer-events-none flex items-center gap-1.5 opacity-0 transition-opacity duration-150 group-hover/table:pointer-events-auto group-hover/table:opacity-100 group-focus-within/table:pointer-events-auto group-focus-within/table:opacity-100'
              }
            >
              <button
                type="button"
                onClick={handleCopy}
                className="rounded border theme-border bg-[color:var(--bg-panel)] px-2 py-1 text-[10px] theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5"
                title={t(I18N_KEYS.AI.TABLE_ACTIONS.COPY_TABLE)}
              >
                {t(I18N_KEYS.AI.COPY)}
              </button>
              <button
                type="button"
                onClick={handleDownload}
                className="rounded border theme-border bg-[color:var(--bg-panel)] px-2 py-1 text-[10px] theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5"
                title={t(I18N_KEYS.AI.TABLE_ACTIONS.DOWNLOAD)}
              >
                {t(I18N_KEYS.AI.TABLE_ACTIONS.DOWNLOAD)}
              </button>
            </div>
          )}
        </div>
      )}
      <div className="overflow-x-auto">
        <table className={`w-full border-collapse border theme-border ${showTopBar ? 'border-t-0' : ''}`}>
          {children ?? (
            <>
              <thead className="theme-bg-hover">
                <tr className="border-b theme-border">
                  {headers.map((header, index) => (
                    <th
                      key={`${header}-${index}`}
                      className="border theme-border px-2 py-1.5 text-left font-medium theme-text-secondary"
                    >
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.length > 0 ? (
                  rows.map((row, rowIndex) => (
                    <tr key={rowIndex} className="border-b theme-border">
                      {row.map((cell, cellIndex) => (
                        <td
                          key={`${rowIndex}-${cellIndex}`}
                          className="border theme-border px-2 py-1.5 text-left"
                          title={cell.title ?? cell.text}
                        >
                          {cell.isNull ? (
                            <span className="text-gray-500 italic">NULL</span>
                          ) : (
                            cell.text
                          )}
                        </td>
                      ))}
                    </tr>
                  ))
                ) : (
                  <tr className="border-b theme-border">
                    <td
                      colSpan={Math.max(headers.length, 1)}
                      className="border theme-border px-2 py-3 text-center theme-text-secondary"
                    >
                      {resolvedEmptyText}
                    </td>
                  </tr>
                )}
              </tbody>
            </>
          )}
        </table>
      </div>
    </div>
  );
}
