import type { TableDataResponse } from '../../services/tableData.service';

export interface TableDataQueryState {
  pageSize: number;
  whereClause: string;
  orderByColumn: string;
  orderByDirection: 'asc' | 'desc';
}

export type TableDataViewMode = 'grid' | 'transpose';

export interface SelectedTableRow {
  rowIndex: number;
  row: unknown[];
}

export interface LoadDataOverrides extends Partial<TableDataQueryState> {}

export function parseOrderBy(orderBy?: string | null): Pick<TableDataQueryState, 'orderByColumn' | 'orderByDirection'> {
  const firstPart = orderBy?.split(',')[0]?.trim() ?? '';
  if (!firstPart) {
    return { orderByColumn: '', orderByDirection: 'asc' };
  }

  const match = firstPart.match(/^(.+?)(?:\s+(asc|desc))?$/i);
  if (!match) {
    return { orderByColumn: '', orderByDirection: 'asc' };
  }

  return {
    orderByColumn: match[1].trim(),
    orderByDirection: match[2]?.toLowerCase() === 'desc' ? 'desc' : 'asc',
  };
}

export function formatOrderBy(column: string, direction: 'asc' | 'desc'): string {
  return column ? `${column} ${direction}` : '';
}

export function toRowMatchValue(value: unknown): unknown {
  if (value == null || typeof value === 'number' || typeof value === 'boolean' || typeof value === 'string') {
    return value;
  }
  if (value instanceof Date) {
    return value.toISOString();
  }
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

export function formatCellValue(value: unknown): string {
  if (value === null || value === undefined) return 'NULL';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

export function getDisplayDbLabel(params: {
  catalog?: string | null;
  databaseName?: string | null;
  schema?: string | null;
  connectionName?: string | null;
}): string {
  return params.catalog || params.databaseName || params.schema || params.connectionName || '';
}

export function getDisplayName(objectName: string, params: {
  catalog?: string | null;
  schema?: string | null;
}): string {
  return [params.catalog, params.schema, objectName].filter(Boolean).join('.');
}

export function getColumns(data: TableDataResponse | null): string[] {
  return data?.headers ?? [];
}

export function getStartRow(data: TableDataResponse | null, currentPage: number, pageSize: number): number {
  return data && data.totalCount > 0 ? (currentPage - 1) * pageSize + 1 : 0;
}

export function getEndRow(data: TableDataResponse | null, currentPage: number, pageSize: number): number {
  return data ? Math.min(currentPage * pageSize, data.totalCount) : 0;
}
