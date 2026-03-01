import { useEffect, useState, useCallback, useMemo } from 'react';
import { Loader2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../lib/utils';
import { tableDataService, type TableDataResponse } from '../../services/tableData.service';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { TableDataTabMetadata } from '../../types/tab';

interface TableDataTabProps {
  metadata: TableDataTabMetadata;
  onMetadataChange: (metadata: Partial<TableDataTabMetadata>) => void;
}

export function TableDataTab({ metadata, onMetadataChange }: TableDataTabProps) {
  const { t } = useTranslation();
  const [data, setData] = useState<TableDataResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [jumpPage, setJumpPage] = useState('');
  const [whereText, setWhereText] = useState(metadata.whereClause ?? '');
  const [orderText, setOrderText] = useState(metadata.orderBy ?? '');
  const pageSize = metadata.pageSize ?? 100;
  const currentPage = metadata.currentPage ?? 1;

  const loadData = useCallback(async (page: number) => {
    setLoading(true);
    setError(null);
    try {
      let result: TableDataResponse;
      if (metadata.objectType === 'table') {
        result = await tableDataService.getTableData(
          String(metadata.connectionId),
          metadata.objectName,
          metadata.catalog,
          metadata.schemaName ?? undefined,
          page,
          pageSize,
          metadata.whereClause,
          metadata.orderBy
        );
      } else {
        result = await tableDataService.getViewData(
          String(metadata.connectionId),
          metadata.objectName,
          metadata.catalog,
          metadata.schemaName ?? undefined,
          page,
          pageSize,
          metadata.whereClause,
          metadata.orderBy
        );
      }
      setData(result);
      if (metadata.currentPage !== page || metadata.pageSize !== pageSize) {
        onMetadataChange({ currentPage: page, pageSize });
      }
    } catch (err: unknown) {
      console.error('Failed to load table data:', err);
      setError((err as Error).message || t(I18N_KEYS.EXPLORER.LOAD_TABLE_DATA_FAILED));
    } finally {
      setLoading(false);
    }
  }, [metadata, onMetadataChange, pageSize, t]);

  useEffect(() => {
    loadData(currentPage);
    setJumpPage(String(currentPage));
    setWhereText(metadata.whereClause ?? '');
    setOrderText(metadata.orderBy ?? '');
  }, [loadData, currentPage, metadata.whereClause, metadata.orderBy]);

  const handlePrevPage = () => {
    if (currentPage > 1) {
      loadData(currentPage - 1);
    }
  };

  const handleFirstPage = () => {
    if (currentPage !== 1) {
      loadData(1);
    }
  };

  const handleLastPage = () => {
    if (data && currentPage !== data.totalPages) {
      loadData(data.totalPages);
    }
  };

  const handleNextPage = () => {
    if (data && currentPage < data.totalPages) {
      loadData(currentPage + 1);
    }
  };

  const handleJump = () => {
    const page = Number(jumpPage);
    if (!Number.isFinite(page) || page < 1 || !data || page > data.totalPages) {
      return;
    }
    loadData(page);
  };

  const applyWhere = (value: string) => {
    onMetadataChange({ whereClause: value, currentPage: 1 });
  };

  const normalizeOrderBy = (value: string) => {
    const firstPart = value.split(',')[0]?.trim() ?? '';
    if (firstPart === '') return '';
    const match = firstPart.match(/^([^\s]+)\s*(asc|desc)?$/i);
    if (!match) return '';
    const dir = (match[2] || 'asc').toLowerCase();
    return `${match[1]} ${dir}`;
  };

  const applyOrderBy = (value: string) => {
    const normalized = normalizeOrderBy(value);
    if (normalized !== orderText) {
      setOrderText(normalized);
    }
    onMetadataChange({ orderBy: normalized, currentPage: 1 });
  };

  const getSortDir = (column: string) => {
    const parts = (orderText || '')
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean);
    for (const part of parts) {
      const match = part.match(/^([^\s]+)\s*(asc|desc)?$/i);
      if (!match) continue;
      if (match[1].toLowerCase() === column.toLowerCase()) {
        return (match[2] || 'asc').toLowerCase();
      }
    }
    return null;
  };

  const toggleOrderBy = (column: string) => {
    const currentDir = getSortDir(column);
    const nextDir = currentDir === 'asc' ? 'desc' : 'asc';
    const nextOrder = `${column} ${nextDir}`;
    setOrderText(nextOrder);
    applyOrderBy(nextOrder);
  };

  const formatCellValue = (value: unknown): string => {
    if (value === null || value === undefined) {
      return 'NULL';
    }
    if (typeof value === 'object') {
      return JSON.stringify(value);
    }
    return String(value);
  };

  const sortedRows = useMemo(() => {
    if (!data || !metadata.orderBy || metadata.orderBy.trim() === '') {
      return data?.rows ?? [];
    }
    const columns = data.headers;
    const firstPart = metadata.orderBy.split(',')[0]?.trim() ?? '';
    const match = firstPart.match(/^([^\s]+)\s*(asc|desc)?$/i);
    if (!match) {
      return data.rows;
    }
    const parts = [{
      name: match[1],
      dir: (match[2] || 'asc').toLowerCase(),
    }];

    const columnIndexMap = new Map<string, number>();
    columns.forEach((col, idx) => columnIndexMap.set(col.toLowerCase(), idx));

    const isNumericString = (value: string) => /^-?\d+(\.\d+)?$/.test(value.trim());

    const compareValues = (a: unknown, b: unknown, columnName: string) => {
      if (a == null && b == null) return 0;
      if (a == null) return 1;
      if (b == null) return -1;
      if (typeof a === 'number' && typeof b === 'number') {
        return a - b;
      }
      const aStr = String(a);
      const bStr = String(b);
      if (isNumericString(aStr) && isNumericString(bStr)) {
        return Number(aStr) - Number(bStr);
      }
      if (/date|time/i.test(columnName)) {
        const aTime = Date.parse(aStr);
        const bTime = Date.parse(bStr);
        if (!Number.isNaN(aTime) && !Number.isNaN(bTime)) {
          return aTime - bTime;
        }
      }
      return aStr.localeCompare(bStr, undefined, { numeric: true, sensitivity: 'base' });
    };

    const nextRows = [...data.rows];
    nextRows.sort((rowA, rowB) => {
      for (const part of parts) {
        const idx = columnIndexMap.get(part.name.toLowerCase());
        if (idx == null) continue;
        const cmp = compareValues(rowA[idx], rowB[idx], columns[idx]);
        if (cmp !== 0) {
          return part.dir === 'desc' ? -cmp : cmp;
        }
      }
      return 0;
    });
    return nextRows;
  }, [data, metadata.orderBy]);

  return (
    <div className="flex-1 h-full min-h-0 overflow-hidden flex flex-col gap-2 p-0">

      {loading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-6 h-6 animate-spin theme-text-secondary" />
        </div>
      )}

      {error && (
        <div className="p-4 bg-destructive/10 text-destructive rounded-md text-sm">
          {error}
        </div>
      )}

      {!loading && !error && data && (
        <div className="flex-1 min-h-0 flex flex-col gap-2">
          <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
            <div className="flex items-center justify-between px-1 py-0.5 text-[11px] theme-text-secondary bg-accent/40 border-b theme-border">
              <div className="flex items-center gap-2 flex-1 min-w-0">
                <span className="uppercase tracking-wide opacity-70">WHERE</span>
                <input
                  value={whereText}
                  onChange={(e) => setWhereText(e.target.value)}
                  onBlur={() => applyWhere(whereText)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') applyWhere(whereText);
                  }}
                  className="w-full max-w-[220px] h-5 px-2 rounded border theme-border theme-bg-panel text-[11px] theme-text-secondary/80 placeholder:theme-text-secondary/20"
                  placeholder="status = 1 AND name like '%a%'"
                />
              </div>
              <div className="flex items-center gap-2 flex-1 min-w-0 justify-center">
                <span className="uppercase tracking-wide opacity-70">ORDER BY</span>
                <input
                  value={orderText}
                  onChange={(e) => setOrderText(e.target.value)}
                  onBlur={() => applyOrderBy(orderText)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') applyOrderBy(orderText);
                  }}
                  className="w-full max-w-[260px] h-5 px-2 rounded border theme-border theme-bg-panel text-[11px] theme-text-secondary/80 placeholder:theme-text-secondary/20"
                  placeholder="col1 asc, col2 desc"
                />
              </div>
            </div>
            <div className="flex-1 min-h-0 overflow-auto">
              <table className="w-full text-xs border-collapse">
                <thead className="sticky top-0 bg-accent">
                  <tr>
                    <th className="border theme-border border-t-0 px-2 py-1.5 text-left font-semibold bg-accent w-10">#</th>
                    {data.headers.map((col) => (
                      <th
                        key={col}
                        className={cn(
                          "border theme-border border-t-0 px-2 py-1.5 text-left font-semibold bg-accent",
                          metadata.highlightColumn === col && "bg-yellow-200 dark:bg-yellow-800"
                        )}
                      >
                        <button
                          type="button"
                          onClick={() => toggleOrderBy(col)}
                          className="inline-flex items-center gap-1 hover:theme-text-primary"
                        >
                          {col}
                          <span className="text-[9px] theme-text-secondary">
                            {getSortDir(col) === 'asc' ? '▲' : getSortDir(col) === 'desc' ? '▼' : '▾'}
                          </span>
                        </button>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {sortedRows.map((row, rowIndex) => (
                    <tr
                      key={rowIndex}
                      className="hover:bg-accent/50"
                    >
                      <td className="border theme-border px-2 py-1 text-right theme-text-secondary">
                        {(currentPage - 1) * pageSize + rowIndex + 1}
                      </td>
                      {data.headers.map((col, colIndex) => (
                        <td
                          key={col}
                          className={cn(
                            "border theme-border px-2 py-1 max-w-xs truncate",
                            metadata.highlightColumn === col && "bg-yellow-100 dark:bg-yellow-900/30"
                          )}
                          title={formatCellValue(row[colIndex])}
                        >
                          {formatCellValue(row[colIndex])}
                        </td>
                      ))}
                    </tr>
                  ))}
                  {sortedRows.length === 0 && (
                    <tr>
                      <td
                        colSpan={data.headers.length + 1}
                        className="border theme-border px-2 py-4 text-center theme-text-secondary"
                      >
                        {t(I18N_KEYS.EXPLORER.NO_DATA)}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between px-1 py-1 text-[10px] theme-text-secondary bg-accent/30">
              <div className="flex items-center gap-2">
                <span className="opacity-70">每页</span>
                <select
                  value={pageSize}
                  onChange={(e) => onMetadataChange({ pageSize: Number(e.target.value), currentPage: 1 })}
                  className="h-5 px-1 rounded border theme-border theme-bg-panel text-[10px] theme-text-secondary"
                >
                  {[5, 10, 20, 30, 50, 100].map((size) => (
                    <option key={size} value={size}>
                      {size}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex items-center gap-1.5">
                <span>
                  {`${(currentPage - 1) * pageSize + 1}-${Math.min(currentPage * pageSize, data.totalCount)}`} / {data.totalPages}
                </span>
                <button
                  onClick={handleFirstPage}
                  disabled={currentPage <= 1}
                  className="px-1.5 py-0.5 rounded hover:bg-accent/50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  ⏮
                </button>
                <button
                  onClick={handleLastPage}
                  disabled={currentPage >= data.totalPages}
                  className="px-1.5 py-0.5 rounded hover:bg-accent/50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  ⏭
                </button>
                <button
                  onClick={handlePrevPage}
                  disabled={currentPage <= 1}
                  className="px-1.5 py-0.5 rounded hover:bg-accent/50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  &lt;
                </button>
                <button
                  onClick={handleNextPage}
                  disabled={currentPage >= data.totalPages}
                  className="px-1.5 py-0.5 rounded hover:bg-accent/50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  &gt;
                </button>
                <input
                  value={jumpPage}
                  onChange={(e) => setJumpPage(e.target.value)}
                  className="w-12 h-5 px-1 rounded border theme-border theme-bg-panel text-[10px] theme-text-secondary"
                  placeholder="页码"
                />
                <button
                  onClick={handleJump}
                  className="px-1.5 py-0.5 rounded hover:bg-accent/50"
                >
                  ↵
                </button>
              </div>
            </div>
          </div>

          
        </div>
      )}
    </div>
  );
}
