import { useMemo, useState } from 'react';
import type { ColDef } from 'ag-grid-community';
import { AgGridReact } from 'ag-grid-react';
import { AlertTriangle, CheckCircle, ChevronDown, ChevronRight, Download, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { useTheme } from '../../../hooks/useTheme';
import { useToast } from '../../../hooks/useToast';
import { resolveErrorMessage } from '../../../lib/errorMessage';
import { exportFileService } from '../../../services/exportFile.service';
import type { ExportFilePreview, ExportedFileResult } from '../../../types/exportFile';
import { formatParameters } from './formatParameters';
import { GenericToolRun } from './GenericToolRun';

interface ExportFileToolBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError: boolean;
}

interface AgentToolResultWrapper {
  success?: boolean;
  message?: string;
  result?: unknown;
}

interface ExportFileParseResult {
  payload: ExportedFileResult | null;
  parseError?: string;
}

interface ExportPreviewGridRow {
  __rowNumber: number;
  [key: string]: string | number;
}

function parseJsonSafe<T>(raw: string): T | null {
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function normalizeResultPayload(value: unknown): unknown {
  if (typeof value === 'string') {
    return parseJsonSafe<unknown>(value) ?? value;
  }
  return value;
}

function toFiniteNumber(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return undefined;
}

function normalizeStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((item) => (item == null ? '' : String(item)));
}

function normalizeRows(value: unknown, expectedColumnCount: number): string[][] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((row) => {
    const cells = Array.isArray(row) ? row : [row];
    const normalized = cells.map((cell) => (cell == null ? '' : String(cell)));
    if (expectedColumnCount <= 0) {
      return normalized;
    }
    if (normalized.length >= expectedColumnCount) {
      return normalized.slice(0, expectedColumnCount);
    }
    return [...normalized, ...Array.from({ length: expectedColumnCount - normalized.length }, () => '')];
  });
}

function normalizePreview(value: unknown): ExportFilePreview | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined;
  }
  const previewObj = value as Record<string, unknown>;
  const columns = normalizeStringArray(previewObj.columns);
  const rows = normalizeRows(previewObj.rows, columns.length);
  if (columns.length === 0) {
    return undefined;
  }

  return {
    columns,
    rows,
    truncated: previewObj.truncated === true,
    totalRowCount: toFiniteNumber(previewObj.totalRowCount),
    totalColumnCount: toFiniteNumber(previewObj.totalColumnCount),
  };
}

function parseExportFilePayload(responseData: string): ExportFileParseResult {
  if (!responseData?.trim()) {
    return { payload: null, parseError: 'Empty tool response.' };
  }

  const parsed = parseJsonSafe<unknown>(responseData);
  if (!parsed || typeof parsed !== 'object') {
    return { payload: null, parseError: 'Tool response is not a valid JSON object.' };
  }

  const maybeWrapper = parsed as AgentToolResultWrapper;
  if (typeof maybeWrapper.success === 'boolean' && !maybeWrapper.success) {
    return {
      payload: null,
      parseError: typeof maybeWrapper.message === 'string'
        ? maybeWrapper.message
        : 'exportFile tool execution failed.',
    };
  }

  const payload = normalizeResultPayload(
    Object.prototype.hasOwnProperty.call(maybeWrapper, 'result') ? maybeWrapper.result : parsed
  );

  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return { payload: null, parseError: 'File payload is missing.' };
  }

  const payloadObj = payload as Record<string, unknown>;
  const fileIdRaw = payloadObj.fileId;
  const filenameRaw = payloadObj.filename;
  const formatRaw = payloadObj.format;

  if (typeof fileIdRaw !== 'string' || fileIdRaw.trim() === '') {
    return { payload: null, parseError: 'File id is missing in export result.' };
  }

  if (typeof filenameRaw !== 'string' || filenameRaw.trim() === '') {
    return { payload: null, parseError: 'Filename is missing in export result.' };
  }

  if (typeof formatRaw !== 'string' || formatRaw.trim() === '') {
    return { payload: null, parseError: 'File format is missing in export result.' };
  }

  return {
    payload: {
      fileId: fileIdRaw.trim(),
      filename: filenameRaw.trim(),
      format: formatRaw.trim(),
      mimeType: typeof payloadObj.mimeType === 'string' ? payloadObj.mimeType : undefined,
      sizeBytes: toFiniteNumber(payloadObj.sizeBytes),
      downloadPath: typeof payloadObj.downloadPath === 'string' ? payloadObj.downloadPath : undefined,
      createdAt: typeof payloadObj.createdAt === 'string' || typeof payloadObj.createdAt === 'number'
        ? payloadObj.createdAt
        : undefined,
      rowCount: toFiniteNumber(payloadObj.rowCount),
      columnCount: toFiniteNumber(payloadObj.columnCount),
      preview: normalizePreview(payloadObj.preview),
    },
  };
}

function parseFilenameFromContentDisposition(contentDisposition: string | undefined): string | null {
  if (!contentDisposition || !contentDisposition.trim()) {
    return null;
  }

  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].trim().replace(/["']/g, ''));
    } catch {
      return utf8Match[1].trim().replace(/["']/g, '');
    }
  }

  const fallbackMatch = /filename="?([^"]+)"?/i.exec(contentDisposition);
  if (fallbackMatch?.[1]) {
    return fallbackMatch[1].trim();
  }
  return null;
}

function triggerBrowserDownload(blob: Blob, filename: string): void {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = filename;
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(objectUrl);
}

function formatBytes(bytes: number | undefined): string {
  if (bytes == null || !Number.isFinite(bytes) || bytes < 0) {
    return '--';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes / 1024;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(value >= 10 ? 1 : 2)} ${units[unitIndex]}`;
}

function formatTimestamp(value: string | number | undefined): string {
  if (value == null) {
    return '--';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '--';
  }

  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  const sec = String(date.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}:${sec}`;
}

async function resolveBlobErrorMessage(error: unknown, fallback: string): Promise<string> {
  const err = error as { response?: { data?: unknown } } | undefined;
  const blob = err?.response?.data;
  if (blob instanceof Blob) {
    try {
      const text = await blob.text();
      if (!text.trim()) {
        return fallback;
      }
      const parsed = parseJsonSafe<{ message?: unknown }>(text);
      if (parsed && typeof parsed.message === 'string' && parsed.message.trim() !== '') {
        return parsed.message.trim();
      }
      return text.length <= 300 ? text : fallback;
    } catch {
      return fallback;
    }
  }
  return resolveErrorMessage(error, fallback);
}

export function ExportFileToolBlock({
  toolName,
  parametersData,
  responseData,
  responseError,
}: ExportFileToolBlockProps) {
  const { t } = useTranslation();
  const { theme } = useTheme();
  const toast = useToast();
  const agThemeClass = theme === 'light' ? 'ag-theme-quartz' : 'ag-theme-quartz-dark';

  const [collapsed, setCollapsed] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const parseResult = useMemo(() => parseExportFilePayload(responseData), [responseData]);
  const payload = parseResult.payload;
  const preview = payload?.preview;

  const previewRows = useMemo<ExportPreviewGridRow[]>(() => {
    if (!preview) {
      return [];
    }
    return preview.rows.map((row, rowIndex) => {
      const mapped: ExportPreviewGridRow = {
        __rowNumber: rowIndex + 1,
      };
      preview.columns.forEach((_, colIndex) => {
        mapped[`__col_${colIndex}`] = row[colIndex] ?? '';
      });
      return mapped;
    });
  }, [preview]);

  const previewColumns = useMemo<ColDef<ExportPreviewGridRow>[]>(() => {
    if (!preview) {
      return [];
    }
    const leading: ColDef<ExportPreviewGridRow> = {
      colId: '__rowNumber',
      field: '__rowNumber',
      headerName: '#',
      width: 60,
      minWidth: 60,
      maxWidth: 60,
      pinned: 'left',
      lockPinned: true,
      lockPosition: 'left',
      suppressMovable: true,
      sortable: false,
      resizable: false,
      cellClass: 'workspace-ag-grid__row-number-cell',
      headerClass: 'workspace-ag-grid__row-number-header',
    };

    const dataColumns = preview.columns.map<ColDef<ExportPreviewGridRow>>((column, index) => ({
      colId: `preview-col-${index}`,
      field: `__col_${index}`,
      headerName: column || `Column ${index + 1}`,
      sortable: false,
      resizable: true,
      minWidth: 120,
      flex: 1,
      suppressHeaderMenuButton: true,
      cellClass: 'theme-text-primary',
      headerClass: 'theme-text-secondary',
    }));

    return [leading, ...dataColumns];
  }, [preview]);

  const summaryText = useMemo(() => {
    const rowCount = payload?.rowCount ?? preview?.totalRowCount ?? previewRows.length;
    const columnCount = payload?.columnCount ?? preview?.totalColumnCount ?? preview?.columns.length ?? 0;
    return t(I18N_KEYS.AI.EXPORT_FILE.SUMMARY, {
      rowCount,
      columnCount,
    });
  }, [payload?.rowCount, payload?.columnCount, preview?.totalRowCount, preview?.totalColumnCount, preview?.columns.length, previewRows.length, t]);

  const parseOrResponseError = parseResult.parseError
    ?? (responseError ? t(I18N_KEYS.AI.EXPORT_FILE.TOOL_FAILED) : null);

  const formattedParameters = useMemo(() => formatParameters(parametersData), [parametersData]);
  const canDownload = !!payload?.fileId && !downloading && !parseOrResponseError;

  const handleDownload = async () => {
    if (!payload?.fileId) {
      toast.error(t(I18N_KEYS.AI.EXPORT_FILE.FILE_ID_MISSING));
      return;
    }

    setDownloading(true);
    try {
      const response = await exportFileService.downloadFile(payload.fileId);
      const downloadedFilename =
        parseFilenameFromContentDisposition(response.contentDisposition) || payload.filename;
      triggerBrowserDownload(response.blob, downloadedFilename);
      toast.success(t(I18N_KEYS.AI.EXPORT_FILE.DOWNLOAD_SUCCESS));
    } catch (error) {
      const message = await resolveBlobErrorMessage(error, t(I18N_KEYS.AI.EXPORT_FILE.DOWNLOAD_FAILED));
      toast.error(message);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="mb-2 space-y-2 rounded border theme-border p-2">
      <button
        type="button"
        onClick={() => setCollapsed((v) => !v)}
        className="w-full flex items-center gap-2 text-left text-[11px] font-medium theme-text-primary rounded hover:bg-black/5 dark:hover:bg-white/5 px-1 py-1"
      >
        {parseOrResponseError ? (
          <AlertTriangle className="w-3.5 h-3.5 text-amber-500 shrink-0" />
        ) : (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" />
        )}
        <FileText className="w-3.5 h-3.5 shrink-0 theme-text-secondary" />
        <span>{payload?.filename ?? t(I18N_KEYS.AI.EXPORT_FILE.LABEL)}</span>
        <span className="ml-auto opacity-70">
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <>
          <div className="rounded border theme-border theme-bg-main px-2 py-2 text-[11px]">
            <div className="flex items-center gap-2">
              <span className="theme-text-secondary">{t(I18N_KEYS.AI.EXPORT_FILE.FORMAT)}</span>
              <span className="theme-text-primary font-medium">{payload?.format ?? '--'}</span>
              <span className="theme-text-secondary">{t(I18N_KEYS.AI.EXPORT_FILE.SIZE)}</span>
              <span className="theme-text-primary">{formatBytes(payload?.sizeBytes)}</span>
            </div>
            <div className="mt-1 flex items-center gap-2">
              <span className="theme-text-secondary">{t(I18N_KEYS.AI.EXPORT_FILE.CREATED_AT)}</span>
              <span className="theme-text-primary">{formatTimestamp(payload?.createdAt)}</span>
            </div>
            <div className="mt-1 theme-text-secondary">{summaryText}</div>
            {preview?.truncated === true && (
              <div className="mt-1 text-amber-600 dark:text-amber-400">
                {t(I18N_KEYS.AI.EXPORT_FILE.TRUNCATED_HINT)}
              </div>
            )}
            <div className="mt-2">
              <button
                type="button"
                onClick={handleDownload}
                disabled={!canDownload}
                className="inline-flex items-center gap-1.5 rounded border theme-border px-2 py-1 theme-bg-main hover:bg-[color:var(--bg-popup)]/60 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Download className="w-3.5 h-3.5" />
                <span>{downloading ? t(I18N_KEYS.AI.EXPORT_FILE.DOWNLOADING) : t(I18N_KEYS.AI.EXPORT_FILE.DOWNLOAD)}</span>
              </button>
            </div>
          </div>

          {preview && previewColumns.length > 1 ? (
            <div className="rounded border theme-border p-2">
              <div className="mb-2 text-[11px] theme-text-secondary">{t(I18N_KEYS.AI.EXPORT_FILE.PREVIEW)}</div>
              <div className={`workspace-ag-grid ${agThemeClass} h-[220px] min-h-0 w-full rounded`}>
                <AgGridReact<ExportPreviewGridRow>
                  rowData={previewRows}
                  columnDefs={previewColumns}
                  rowModelType="clientSide"
                  animateRows={false}
                  suppressCellFocus
                  suppressRowHoverHighlight
                  suppressMovableColumns
                  rowSelection={undefined}
                  rowHeight={32}
                  headerHeight={34}
                  defaultColDef={{
                    sortable: false,
                    resizable: true,
                    minWidth: 120,
                    suppressHeaderMenuButton: true,
                  }}
                />
              </div>
            </div>
          ) : (
            <div className="rounded border theme-border px-2 py-1.5 text-[11px] theme-text-secondary">
              {t(I18N_KEYS.AI.EXPORT_FILE.NO_PREVIEW)}
            </div>
          )}

          {parseOrResponseError && (
            <>
              <div className="rounded border border-amber-500/40 bg-amber-500/10 px-2 py-1 text-[11px] theme-text-primary flex items-start gap-2">
                <AlertTriangle className="w-3.5 h-3.5 mt-0.5 text-amber-500" />
                <div>{parseOrResponseError}</div>
              </div>
              <GenericToolRun
                toolName={toolName}
                formattedParameters={formattedParameters}
                responseData={responseData}
                responseError={true}
              />
            </>
          )}
        </>
      )}
    </div>
  );
}
