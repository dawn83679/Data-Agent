import { useState, useEffect, useCallback, useRef } from 'react';
import { Play, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, ChevronUp, ChevronDown, Plus, Minus, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../lib/utils';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { tableDataService, type TableDataResponse } from '../../services/tableData.service';
import { tableService } from '../../services/table.service';
import { viewService } from '../../services/view.service';
import { sqlExecutionService } from '../../services/sqlExecution.service';
import { primaryKeyService } from '../../services/primaryKey.service';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { TableTabMetadata } from '../../types/tab';
import { TransactionMode, IsolationLevel } from '../../constants/transactionSettings';
import { TransactionModeSelector } from './TransactionModeSelector';
import { DdlViewerDialog } from '../explorer/DdlViewerDialog';
import { AddRowDialog } from './AddRowDialog';
import { useToast } from '../../hooks/useToast';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { databaseService } from '../../services/database.service';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/Dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';

interface TableDataTabProps {
  tabId: string;
  metadata: TableTabMetadata;
}

const PAGE_SIZE_OPTIONS = [50, 100, 200, 500];

export function TableDataTab({ tabId, metadata }: TableDataTabProps) {
  const { t } = useTranslation();
  const [data, setData] = useState<TableDataResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [whereClause, setWhereClause] = useState('');
  const [orderByColumn, setOrderByColumn] = useState<string>('');
  const [orderByDirection, setOrderByDirection] = useState<'asc' | 'desc'>('asc');
  const orderByRef = useRef({ orderByColumn: '', orderByDirection: 'asc' as const });
  useEffect(() => {
    orderByRef.current = { orderByColumn, orderByDirection };
  }, [orderByColumn, orderByDirection]);
  const [txMode, setTxMode] = useState<TransactionMode>(TransactionMode.AUTO);
  const [isolationLevel, setIsolationLevel] = useState<IsolationLevel>(IsolationLevel.DEFAULT);
  const [ddlDialogOpen, setDdlDialogOpen] = useState(false);
  const [addRowDialogOpen, setAddRowDialogOpen] = useState(false);
  const [selectedRowIndex, setSelectedRowIndex] = useState<number | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletePending, setDeletePending] = useState(false);
  const [databases, setDatabases] = useState<string[]>([]);
  const [loadingDatabases, setLoadingDatabases] = useState(false);

  const toast = useToast();
  const updateTabMetadata = useWorkspaceStore((s) => s.updateTabMetadata);
  const connId = String(metadata.connectionId);
  const objectName = metadata.objectName;
  const objectType = metadata.objectType;
  const isTable = objectType === 'table';
  const catalog = metadata.catalog ?? '';
  const schema = metadata.schema ?? '';

  const loadData = useCallback(
    async (
      page: number = 1,
      overrides?: { orderByColumn?: string; orderByDirection?: 'asc' | 'desc' }
    ) => {
      setLoading(true);
      setError(null);
      try {
        const where = whereClause.trim() || undefined;
        const ref = orderByRef.current;
        const orderCol = (overrides?.orderByColumn ?? ref.orderByColumn).trim() || undefined;
        const orderDir = overrides?.orderByDirection ?? ref.orderByDirection;

        let result: TableDataResponse;
        if (objectType === 'table') {
          result = await tableDataService.getTableData(
            connId,
            objectName,
            catalog || undefined,
            schema || undefined,
            page,
            pageSize,
            where,
            orderCol,
            orderDir
          );
        } else {
          result = await tableDataService.getViewData(
            connId,
            objectName,
            catalog || undefined,
            schema || undefined,
            page,
            pageSize,
            where,
            orderCol,
            orderDir
          );
        }
        setData(result);
        setCurrentPage(page);
      } catch (err: unknown) {
        console.error('Failed to load table data:', err);
        setError((err as Error).message || t(I18N_KEYS.EXPLORER.LOAD_TABLE_DATA_FAILED));
      } finally {
        setLoading(false);
      }
    },
    [connId, objectName, objectType, catalog, schema, pageSize, whereClause, t]
  );

  useEffect(() => {
    loadData(1);
  }, [loadData]);

  useEffect(() => {
    const loadDbs = async () => {
      setLoadingDatabases(true);
      try {
        const list = await databaseService.listDatabases(connId);
        setDatabases(list || []);
      } catch {
        setDatabases([]);
      } finally {
        setLoadingDatabases(false);
      }
    };
    loadDbs();
  }, [connId]);

  const handleRun = () => loadData(1);

  const handleDatabaseChange = useCallback(
    (db: string) => {
      updateTabMetadata(tabId, { catalog: db, databaseName: db });
    },
    [tabId, updateTabMetadata]
  );

  const handleFirstPage = () => loadData(1);
  const handlePrevPage = () => {
    if (currentPage > 1) loadData(currentPage - 1);
  };
  const handleNextPage = () => {
    if (data && currentPage < data.totalPages) loadData(currentPage + 1);
  };
  const handleLastPage = () => {
    if (data && data.totalPages > 0) loadData(data.totalPages);
  };

  const handleColumnSort = useCallback(
    (col: string) => {
      const newDir =
        orderByColumn === col ? (orderByDirection === 'asc' ? 'desc' : 'asc') : 'asc';
      setOrderByColumn(col);
      setOrderByDirection(newDir);
      orderByRef.current = { orderByColumn: col, orderByDirection: newDir };
      loadData(1, { orderByColumn: col, orderByDirection: newDir });
    },
    [orderByColumn, orderByDirection, loadData]
  );

  const executeInsert = useCallback(
    async (sql: string) => {
      try {
        const res = await sqlExecutionService.executeSql({
          connectionId: metadata.connectionId,
          databaseName: metadata.databaseName ?? undefined,
          schemaName: metadata.schemaName ?? undefined,
          sql,
        });
        return { success: res.success, errorMessage: res.errorMessage ?? undefined };
      } catch (err) {
        return { success: false, errorMessage: (err as Error).message };
      }
    },
    [metadata.connectionId, metadata.databaseName, metadata.schemaName]
  );

  const handleAddRow = () => {
    if (!isTable) {
      toast.warning(t(I18N_KEYS.EXPLORER.VIEW_READONLY));
      return;
    }
    setAddRowDialogOpen(true);
  };

  const handleDeleteRow = () => {
    if (!isTable) {
      toast.warning(t(I18N_KEYS.EXPLORER.VIEW_READONLY));
      return;
    }
    if (selectedRowIndex === null || !data) {
      toast.warning(t(I18N_KEYS.EXPLORER.SELECT_ROW_TO_DELETE));
      return;
    }
    setDeleteConfirmOpen(true);
  };

  const handleConfirmDelete = useCallback(async () => {
    if (selectedRowIndex === null || !data) return;
    const row = data.rows[selectedRowIndex];
    if (!row) return;

    setDeletePending(true);
    try {
      const pks = await primaryKeyService.listPrimaryKeys(connId, objectName, catalog || undefined, schema || undefined);
      const whereColumns = pks?.length && pks[0]?.columnNames?.length
        ? pks[0].columnNames
        : data.headers;

      const quoteId = (n: string) => '`' + n.replace(/`/g, '``') + '`';
      const formatVal = (v: unknown): string => {
        if (v === null || v === undefined) return 'NULL';
        if (typeof v === 'number') return String(v);
        return "'" + String(v).replace(/'/g, "''") + "'";
      };

      const conditions = whereColumns.map((col) => {
        const colIndex = data.headers.indexOf(col);
        if (colIndex === -1) return null;
        const val = row[colIndex];
        if (val === null || val === undefined) return `${quoteId(col)} IS NULL`;
        return `${quoteId(col)} = ${formatVal(val)}`;
      }).filter(Boolean) as string[];

      if (conditions.length === 0) {
        toast.error('Cannot build DELETE: no matching columns');
        return;
      }

      const catalogOrSchema = catalog || schema || '';
      const tablePart = catalogOrSchema ? `${quoteId(catalogOrSchema)}.${quoteId(objectName)}` : quoteId(objectName);
      const sql = `DELETE FROM ${tablePart} WHERE ${conditions.join(' AND ')}`;
      const res = await sqlExecutionService.executeSql({
        connectionId: metadata.connectionId,
        databaseName: metadata.databaseName ?? undefined,
        schemaName: metadata.schemaName ?? undefined,
        sql,
      });
      if (res.success) {
        toast.success(t(I18N_KEYS.EXPLORER.ROW_DELETE_SUCCESS));
        setDeleteConfirmOpen(false);
        setSelectedRowIndex(null);
        loadData(currentPage);
      } else {
        toast.error(res.errorMessage || 'Delete failed');
      }
    } catch (err) {
      toast.error((err as Error).message);
    } finally {
      setDeletePending(false);
    }
  }, [selectedRowIndex, data, connId, objectName, catalog, schema, metadata, currentPage, loadData, t]);

  const formatCellValue = (value: unknown): string => {
    if (value === null || value === undefined) return 'NULL';
    if (typeof value === 'object') return JSON.stringify(value);
    return String(value);
  };

  const columns = data?.headers ?? [];
  const startRow = data ? (currentPage - 1) * pageSize + 1 : 0;
  const endRow = data ? Math.min(currentPage * pageSize, data.totalCount) : 0;

  const displayDbLabel = metadata.catalog || metadata.schema || metadata.databaseName || metadata.connectionName || '';

  const loadDdl = useCallback(async () => {
    if (objectType === 'table') {
      return tableService.getTableDdl(connId, objectName, catalog || undefined, schema || undefined);
    }
    return viewService.getViewDdl(connId, objectName, catalog || undefined, schema || undefined);
  }, [connId, objectName, objectType, catalog, schema]);

  const displayName = [metadata.catalog, metadata.schema, objectName].filter(Boolean).join('.');

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* Toolbar */}
      <div className="h-8 flex items-center px-2 theme-bg-main border-b theme-border text-[10px] theme-text-secondary shrink-0 gap-1">
        <Button
          variant="ghost"
          size="icon"
          onClick={handleRun}
          disabled={loading}
          title={t(I18N_KEYS.COMMON.EXECUTE_QUERY)}
          className="h-6 w-6"
        >
          <Play className="w-3.5 h-3.5 fill-current text-green-500" />
        </Button>
        <TransactionModeSelector
          transactionMode={txMode}
          isolationLevel={isolationLevel}
          onTransactionModeChange={setTxMode}
          onIsolationLevelChange={setIsolationLevel}
        />
        <div className="w-px h-4 bg-border mx-0.5" />
        <Button
          variant="ghost"
          size="icon"
          className="h-6 w-6"
          title={t(I18N_KEYS.EXPLORER.ADD_ROW)}
          onClick={handleAddRow}
        >
          <Plus className="w-3.5 h-3.5 theme-text-secondary" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-6 w-6"
          title={t(I18N_KEYS.EXPLORER.DELETE_ROW)}
          onClick={handleDeleteRow}
        >
          <Minus className="w-3.5 h-3.5 theme-text-secondary" />
        </Button>
        <div className="w-px h-4 bg-border mx-0.5" />
        <Button
          variant="ghost"
          size="sm"
          className="h-6 px-2 gap-1 text-[11px]"
          onClick={() => setDdlDialogOpen(true)}
          title={t(I18N_KEYS.EXPLORER.VIEW_DDL)}
        >
          <FileText className="w-3.5 h-3.5 theme-text-secondary" />
          DDL
        </Button>
        <div className="flex-1" />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="h-6 px-2 rounded flex items-center gap-1 text-[11px] theme-text-primary hover:bg-accent/30 transition-colors">
              <span>{displayDbLabel || metadata.connectionName}</span>
              <ChevronDown className="w-3 h-3 theme-text-secondary" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-[140px]">
            {loadingDatabases ? (
              <div className="px-3 py-2 text-[10px] theme-text-secondary">
                {t(I18N_KEYS.COMMON.LOADING)}...
              </div>
            ) : databases.length === 0 ? (
              <div className="px-3 py-2 text-[10px] theme-text-secondary">
                {t(I18N_KEYS.COMMON.NO_DATA)}
              </div>
            ) : (
              databases.map((db) => (
                <DropdownMenuItem
                  key={db}
                  onClick={() => handleDatabaseChange(db)}
                  className={`text-[11px] px-2 py-1.5 ${
                    displayDbLabel === db
                      ? 'theme-text-primary font-semibold'
                      : 'theme-text-secondary'
                  }`}
                >
                  <span className="w-4">{displayDbLabel === db && <span>✓</span>}</span>
                  <span className="ml-2">{db}</span>
                </DropdownMenuItem>
              ))
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* WHERE & ORDER BY — 一半一半 */}
      <div className="grid grid-cols-2 gap-3 px-2 py-1.5 border-b theme-border shrink-0 text-[11px]">
        <div className="flex items-center gap-2 min-w-0">
          <span className="shrink-0 theme-text-secondary font-medium">{t(I18N_KEYS.EXPLORER.WHERE_LABEL)}</span>
          <Input
            value={whereClause}
            onChange={(e) => setWhereClause(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleRun()}
            placeholder="status = 1 AND name LIKE '%a%'"
            className="h-7 text-[11px] font-mono flex-1 min-w-0"
          />
        </div>
        <div className="flex items-center gap-2 min-w-0">
          <span className="shrink-0 theme-text-secondary font-medium">{t(I18N_KEYS.EXPLORER.ORDER_BY_LABEL)}</span>
          <select
            value={orderByColumn}
            onChange={(e) => {
              const col = e.target.value;
              setOrderByColumn(col);
              const dir = col ? 'asc' : orderByDirection;
              if (col) setOrderByDirection('asc');
              loadData(1, { orderByColumn: col, orderByDirection: dir });
            }}
            className="h-7 px-2 rounded border theme-border theme-bg-main theme-text-primary text-[11px] font-mono flex-1 min-w-0"
          >
            <option value="">--</option>
            {columns.map((col) => (
              <option key={col} value={col}>
                {col}
              </option>
            ))}
          </select>
          {orderByColumn && (
            <button
              type="button"
              onClick={() => {
                const newDir = orderByDirection === 'asc' ? 'desc' : 'asc';
                setOrderByDirection(newDir);
                loadData(1, { orderByColumn, orderByDirection: newDir });
              }}
              className="h-7 px-2 rounded border theme-border theme-bg-main theme-text-primary text-[11px] hover:bg-accent/50 flex items-center gap-1 shrink-0"
              title={orderByDirection === 'asc' ? t(I18N_KEYS.EXPLORER.ORDER_ASC) : t(I18N_KEYS.EXPLORER.ORDER_DESC)}
            >
              {orderByDirection === 'asc' ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
              {orderByDirection === 'asc' ? t(I18N_KEYS.EXPLORER.ORDER_ASC) : t(I18N_KEYS.EXPLORER.ORDER_DESC)}
            </button>
          )}
        </div>
      </div>

      <DdlViewerDialog
        open={ddlDialogOpen}
        onOpenChange={setDdlDialogOpen}
        title={objectType === 'table' ? t(I18N_KEYS.EXPLORER.TABLE_DDL) : t(I18N_KEYS.EXPLORER.VIEW_DDL_TITLE)}
        displayName={displayName}
        loadDdl={loadDdl}
      />

      <AddRowDialog
        open={addRowDialogOpen}
        onOpenChange={setAddRowDialogOpen}
        tableName={objectName}
        connectionId={connId}
        catalog={catalog || undefined}
        schema={schema || undefined}
        displayName={displayName}
        onSuccess={() => {
          toast.success(t(I18N_KEYS.EXPLORER.ROW_INSERT_SUCCESS));
          loadData(1);
        }}
        executeInsert={executeInsert}
      />

      <Dialog open={deleteConfirmOpen} onOpenChange={setDeleteConfirmOpen}>
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle>{t(I18N_KEYS.EXPLORER.DELETE_ROW)}</DialogTitle>
            <DialogDescription>{t(I18N_KEYS.EXPLORER.DELETE_ROW_CONFIRM)}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteConfirmOpen(false)} disabled={deletePending}>
              {t(I18N_KEYS.CONNECTIONS.CANCEL)}
            </Button>
            <Button variant="destructive" disabled={deletePending} onClick={handleConfirmDelete}>
              {deletePending ? t(I18N_KEYS.COMMON.LOADING) + '...' : t(I18N_KEYS.EXPLORER.DELETE_ROW)}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Data grid */}
      <div className="flex-1 overflow-auto min-h-0">
        {loading && (
          <div className="flex items-center justify-center py-12">
            <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && (
          <div className="p-4 m-2 bg-destructive/10 text-destructive rounded-md text-sm">{error}</div>
        )}

        {!loading && !error && data && (
          <div className="overflow-auto h-full">
            <table className="text-[11px] w-full border-collapse">
              <thead className="sticky top-0 theme-bg-panel z-10">
                <tr>
                  <th className="px-3 py-1.5 text-left font-medium theme-text-secondary border-b border-r theme-border whitespace-nowrap w-10">
                    #
                  </th>
                  {data.headers.map((col) => (
                    <th
                      key={col}
                      onClick={() => handleColumnSort(col)}
                      className={cn(
                        'px-3 py-1.5 text-left font-medium theme-text-secondary border-b border-r theme-border whitespace-nowrap cursor-pointer hover:bg-accent/50',
                        orderByColumn === col && 'bg-accent/30'
                      )}
                    >
                      <span className="inline-flex items-center gap-1">
                        <span className="truncate min-w-0">{col}</span>
                        <span
                          className={cn(
                            'shrink-0 w-3 h-3 flex items-center justify-center',
                            orderByColumn !== col && 'invisible'
                          )}
                          aria-hidden
                        >
                          {orderByDirection === 'asc' ? (
                            <ChevronUp className="w-2.5 h-2.5" />
                          ) : (
                            <ChevronDown className="w-2.5 h-2.5" />
                          )}
                        </span>
                      </span>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.rows.map((row, rowIndex) => (
                  <tr
                    key={rowIndex}
                    onClick={() => setSelectedRowIndex(selectedRowIndex === rowIndex ? null : rowIndex)}
                    className={cn(
                      'hover:bg-accent/30 cursor-pointer',
                      selectedRowIndex === rowIndex && 'bg-accent/50'
                    )}
                  >
                    <td className="px-3 py-1 theme-text-secondary border-b border-r theme-border">
                      {startRow + rowIndex}
                    </td>
                    {data.headers.map((col, colIndex) => (
                      <td
                        key={col}
                        className="px-3 py-1 max-w-xs truncate theme-text-primary border-b border-r theme-border"
                        title={formatCellValue(row[colIndex])}
                      >
                        {formatCellValue(row[colIndex])}
                      </td>
                    ))}
                  </tr>
                ))}
                {data.rows.length === 0 && (
                  <tr>
                    <td
                      colSpan={data.headers.length + 1}
                      className="px-3 py-8 text-center theme-text-secondary"
                    >
                      {t(I18N_KEYS.EXPLORER.NO_DATA)}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Pagination */}
      {!loading && !error && data && data.totalCount > 0 && (
        <div className="flex items-center justify-center gap-2 py-2 border-t theme-border shrink-0 text-[11px] theme-text-secondary">
          <span className="flex items-center gap-1">
            {t(I18N_KEYS.EXPLORER.ROWS_PER_PAGE)}
            <select
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setCurrentPage(1);
              }}
              className="h-6 px-1 rounded border theme-border theme-bg-main theme-text-primary text-[11px] ml-1"
            >
              {PAGE_SIZE_OPTIONS.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </span>
          <span>
            {startRow}-{endRow} / {data.totalCount}
          </span>
          <Button variant="ghost" size="sm" onClick={handleFirstPage} disabled={currentPage <= 1} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.FIRST_PAGE)}>
            <ChevronsLeft className="w-4 h-4" />
          </Button>
          <Button variant="ghost" size="sm" onClick={handlePrevPage} disabled={currentPage <= 1} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.PREVIOUS)}>
            <ChevronLeft className="w-4 h-4" />
          </Button>
          <span className="px-2">{currentPage}</span>
          <Button variant="ghost" size="sm" onClick={handleNextPage} disabled={currentPage >= data.totalPages} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.NEXT)}>
            <ChevronRight className="w-4 h-4" />
          </Button>
          <Button variant="ghost" size="sm" onClick={handleLastPage} disabled={currentPage >= data.totalPages} className="h-6 w-6 p-0" title={t(I18N_KEYS.EXPLORER.LAST_PAGE)}>
            <ChevronsRight className="w-4 h-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
