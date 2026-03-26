import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle, ChevronDown, ChevronRight, Download } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { useToast } from '../../../hooks/useToast';
import { resolveErrorMessage } from '../../../lib/errorMessage';
import { exportFileService } from '../../../services/exportFile.service';
import type { ExportFilePreview, ExportedFileResult, ExportFileStatus } from '../../../types/exportFile';
import { formatParameters } from './formatParameters';
import { GenericToolRun } from './GenericToolRun';
import { markdownRemarkPlugins, useMarkdownComponents } from './markdownComponents';

interface ExportFileToolBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError: boolean;
  checkAvailability?: boolean;
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

function escapeMarkdownTableCell(value: string): string {
  return value
    .replace(/\\/g, '\\\\')
    .replace(/\|/g, '\\|')
    .replace(/\r?\n/g, '<br/>');
}

function buildPreviewMarkdown(preview: ExportFilePreview | undefined): string {
  if (!preview || preview.columns.length === 0) {
    return '';
  }

  const headers = ['#', ...preview.columns].map(escapeMarkdownTableCell);
  const separator = headers.map(() => '---');
  const rows = preview.rows.map((row, rowIndex) => {
    const numberedRow = [String(rowIndex + 1), ...row];
    return numberedRow
      .map((cell) => escapeMarkdownTableCell(cell ?? ''))
      .join(' | ');
  });

  return [
    `| ${headers.join(' | ')} |`,
    `| ${separator.join(' | ')} |`,
    ...rows.map((row) => `| ${row} |`),
  ].join('\n');
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

function resolveFileType(value: string | undefined): string {
  if (!value) {
    return '';
  }
  return value.trim().toLowerCase();
}

function inferExportFileKind(payload: ExportedFileResult | null): 'csv' | 'xlsx' | 'docx' | 'pdf' | 'generic' {
  const filename = resolveFileType(payload?.filename);
  if (filename.endsWith('.csv')) return 'csv';
  if (filename.endsWith('.xlsx') || filename.endsWith('.xls')) return 'xlsx';
  if (filename.endsWith('.docx') || filename.endsWith('.doc')) return 'docx';
  if (filename.endsWith('.pdf')) return 'pdf';

  const format = resolveFileType(payload?.format);
  if (format === 'csv') return 'csv';
  if (format === 'xlsx' || format === 'xls') return 'xlsx';
  if (format === 'docx' || format === 'doc') return 'docx';
  if (format === 'pdf') return 'pdf';
  return 'generic';
}

interface SplitFileIconProps {
  letter: string;
  tileColor: string;
  tileShadow: string;
  panelColor: string;
  foldColor: string;
  accentColor: string;
  kind: 'text' | 'grid' | 'pdf';
}

function SplitFileIcon({
  letter,
  tileColor,
  tileShadow,
  panelColor,
  foldColor,
  accentColor,
  kind,
}: SplitFileIconProps) {
  return (
    <span className="inline-flex h-8 w-8 shrink-0 items-center justify-center" aria-hidden="true">
      <svg viewBox="0 0 32 32" className="h-8 w-8">
        <rect x="12" y="3" width="16" height="24" rx="4" fill={panelColor} />
        <path d="M22 3h1.4c1 0 1.6.1 2.1.4.4.2.7.5 1 .8l1.3 1.3c.3.3.6.6.8 1 .3.5.4 1.1.4 2.1V10h-5.2c-1.6 0-2.8-1.2-2.8-2.8V3Z" fill={foldColor} />
        {kind === 'text' && (
          <>
            <rect x="17" y="12" width="7.5" height="1.8" rx="0.9" fill={accentColor} opacity="0.95" />
            <rect x="17" y="15.8" width="8.5" height="1.8" rx="0.9" fill={accentColor} opacity="0.78" />
            <rect x="17" y="19.6" width="6.2" height="1.8" rx="0.9" fill={accentColor} opacity="0.6" />
          </>
        )}
        {kind === 'grid' && (
          <>
            <rect x="17" y="11.4" width="8.8" height="10.8" rx="1.6" fill={accentColor} opacity="0.18" />
            <path d="M17 15h8.8M17 18.6h8.8M19.95 11.4v10.8M22.9 11.4v10.8" stroke={accentColor} strokeWidth="1.4" strokeLinecap="round" opacity="0.95" />
          </>
        )}
        {kind === 'pdf' && (
          <>
            <path d="M18.2 20.6c2.8-3.8 3.9-7 3.4-8-.3-.6-1.2-.8-1.7-.3-.8.8-.6 2.8.3 5.2 1.2 3.1 3.4 5 5 4.5 1.1-.4.8-1.9-.8-2.3-2.8-.7-7.2.1-10.2 2.1-1.5 1-1.1 2.4.2 2.4 1.8 0 3.3-1.8 4.8-5.2" fill="none" stroke={accentColor} strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
          </>
        )}
        <rect x="3.5" y="7.2" width="12.2" height="17.6" rx="3.2" fill={tileColor} />
        <rect x="3.5" y="7.2" width="12.2" height="17.6" rx="3.2" fill={tileShadow} opacity="0.18" />
        <text
          x="9.6"
          y="18.7"
          textAnchor="middle"
          fontSize="8.4"
          fontWeight="700"
          fill="#FFFFFF"
          style={{ fontFamily: 'Segoe UI, Arial, sans-serif' }}
        >
          {letter}
        </text>
      </svg>
    </span>
  );
}

function FileTypeIcon({ payload }: { payload: ExportedFileResult | null }) {
  const kind = inferExportFileKind(payload);

  if (kind === 'docx') {
    return (
      <SplitFileIcon
        letter="W"
        tileColor="#185ABD"
        tileShadow="#0F3F91"
        panelColor="#41A5EE"
        foldColor="#7DC8F7"
        accentColor="#EAF4FF"
        kind="text"
      />
    );
  }

  if (kind === 'xlsx') {
    return (
      <SplitFileIcon
        letter="X"
        tileColor="#107C41"
        tileShadow="#0B5C2E"
        panelColor="#33C481"
        foldColor="#7BE0AD"
        accentColor="#E9FFF4"
        kind="grid"
      />
    );
  }

  if (kind === 'csv') {
    return (
      <SplitFileIcon
        letter="C"
        tileColor="#107C41"
        tileShadow="#0B5C2E"
        panelColor="#33C481"
        foldColor="#7BE0AD"
        accentColor="#E9FFF4"
        kind="grid"
      />
    );
  }

  if (kind === 'pdf') {
    return (
      <SplitFileIcon
        letter="P"
        tileColor="#C43E1C"
        tileShadow="#962C13"
        panelColor="#FF6B57"
        foldColor="#FF9B8F"
        accentColor="#FFF4F2"
        kind="pdf"
      />
    );
  }

  return (
    <SplitFileIcon
      letter="F"
      tileColor="#5B6472"
      tileShadow="#434A55"
      panelColor="#97A1AF"
      foldColor="#C8D0DB"
      accentColor="#F8FAFC"
      kind="text"
    />
  );
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
  checkAvailability = false,
}: ExportFileToolBlockProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const markdownComponents = useMarkdownComponents({ enableTableActions: false });

  const [collapsed, setCollapsed] = useState(checkAvailability);
  const [previewCollapsed, setPreviewCollapsed] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const parseResult = useMemo(() => parseExportFilePayload(responseData), [responseData]);
  const payload = parseResult.payload;
  const preview = payload?.preview;
  const previewMarkdown = useMemo(() => buildPreviewMarkdown(preview), [preview]);
  const [fileStatus, setFileStatus] = useState<ExportFileStatus | null>(null);
  const [statusChecked, setStatusChecked] = useState(() => !checkAvailability || !payload?.fileId);

  const summaryText = useMemo(() => {
    const rowCount = payload?.rowCount ?? preview?.totalRowCount ?? preview?.rows.length ?? 0;
    const columnCount = payload?.columnCount ?? preview?.totalColumnCount ?? preview?.columns.length ?? 0;
    return t(I18N_KEYS.AI.EXPORT_FILE.SUMMARY, {
      rowCount,
      columnCount,
    });
  }, [payload?.rowCount, payload?.columnCount, preview?.totalRowCount, preview?.totalColumnCount, preview?.columns.length, preview?.rows.length, t]);

  const parseOrResponseError = parseResult.parseError
    ?? (responseError ? t(I18N_KEYS.AI.EXPORT_FILE.TOOL_FAILED) : null);

  useEffect(() => {
    if (!checkAvailability || !payload?.fileId || parseOrResponseError) {
      setFileStatus(null);
      setStatusChecked(true);
      return;
    }

    let cancelled = false;
    setFileStatus(null);
    setStatusChecked(false);

    exportFileService.getFileStatus(payload.fileId)
      .then((status) => {
        if (cancelled) {
          return;
        }
        setFileStatus(status);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        setFileStatus(null);
      })
      .finally(() => {
        if (!cancelled) {
          setStatusChecked(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [checkAvailability, payload?.fileId, parseOrResponseError]);

  const formattedParameters = useMemo(() => formatParameters(parametersData), [parametersData]);
  const canDownload = !!payload?.fileId && !downloading && !parseOrResponseError;

  if (checkAvailability && payload?.fileId && !parseOrResponseError && !statusChecked) {
    return null;
  }

  if (checkAvailability && payload?.fileId && !parseOrResponseError && fileStatus && !fileStatus.available) {
    return null;
  }

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
        <FileTypeIcon payload={payload} />
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
              <span className="theme-text-primary">{formatBytes(fileStatus?.sizeBytes ?? payload?.sizeBytes)}</span>
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

          {preview && previewMarkdown ? (
            <div className="rounded border theme-border p-2">
              <button
                type="button"
                onClick={() => setPreviewCollapsed((value) => !value)}
                className="mb-1 flex w-full items-center gap-2 rounded px-1 py-1 text-left text-[11px] theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5"
              >
                {previewCollapsed ? (
                  <ChevronRight className="h-3.5 w-3.5 shrink-0" />
                ) : (
                  <ChevronDown className="h-3.5 w-3.5 shrink-0" />
                )}
                <span>{t(I18N_KEYS.AI.EXPORT_FILE.PREVIEW)}</span>
              </button>
              {!previewCollapsed && (
                <div className="max-h-[260px] overflow-auto text-[11px]">
                  <ReactMarkdown
                    components={markdownComponents}
                    remarkPlugins={markdownRemarkPlugins}
                  >
                    {previewMarkdown}
                  </ReactMarkdown>
                </div>
              )}
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
