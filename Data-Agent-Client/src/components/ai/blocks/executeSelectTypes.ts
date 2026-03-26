import type { LightDataTableCell } from './LightDataTable';

interface AgentSqlColumnPayload {
  name?: unknown;
}

interface AgentSqlResultPayload {
  success?: unknown;
  error?: unknown;
  type?: unknown;
  columns?: unknown;
  rows?: unknown;
  affectedRows?: unknown;
  truncated?: unknown;
  limitApplied?: unknown;
  results?: unknown;
}

export interface ExecuteSelectResultSetView {
  key: string;
  label: string;
  headers: string[];
  rows: LightDataTableCell[][];
  rowCount: number;
  columnCount: number;
  truncated: boolean;
  limitApplied: boolean;
}

export interface ExecuteSelectPayloadView {
  success: boolean;
  error?: string;
  resultSets: ExecuteSelectResultSetView[];
  prettyJson: string;
}

function parseJsonSafe<T>(raw: string): T | null {
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function normalizeColumnName(value: unknown): string {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return '';
  }
  const column = value as AgentSqlColumnPayload;
  return typeof column.name === 'string' ? column.name.trim() : '';
}

function normalizeCell(value: unknown): LightDataTableCell {
  if (value == null) {
    return {
      text: 'NULL',
      title: 'NULL',
      isNull: true,
    };
  }
  if (typeof value === 'object') {
    const text = JSON.stringify(value);
    return { text, title: text };
  }
  const text = String(value);
  return { text, title: text };
}

function collectResultSets(value: unknown, collector: ExecuteSelectResultSetView[]): void {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return;
  }

  const payload = value as AgentSqlResultPayload;
  const headers = Array.isArray(payload.columns)
    ? payload.columns.map((column) => normalizeColumnName(column)).filter((header) => header !== '')
    : [];
  const rawRows = Array.isArray(payload.rows)
    ? payload.rows.filter((row): row is unknown[] => Array.isArray(row))
    : [];

  if (headers.length > 0) {
    collector.push({
      key: `result-${collector.length}`,
      label: `结果 ${collector.length + 1}`,
      headers,
      rows: rawRows.map((row) => row.map((cell) => normalizeCell(cell))),
      rowCount: rawRows.length,
      columnCount: headers.length,
      truncated: payload.truncated === true,
      limitApplied: payload.limitApplied === true,
    });
  }

  if (Array.isArray(payload.results)) {
    payload.results.forEach((child) => collectResultSets(child, collector));
  }
}

export function parseExecuteSelectResponse(responseData: string): ExecuteSelectPayloadView {
  const parsed = parseJsonSafe<unknown>(responseData);
  const prettyJson = parsed != null
    ? JSON.stringify(parsed, null, 2)
    : (responseData || '—');

  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return {
      success: false,
      resultSets: [],
      prettyJson,
    };
  }

  const payload = parsed as AgentSqlResultPayload;
  const resultSets: ExecuteSelectResultSetView[] = [];
  collectResultSets(payload, resultSets);

  return {
    success: payload.success === true,
    error: typeof payload.error === 'string' ? payload.error : undefined,
    resultSets,
    prettyJson,
  };
}
