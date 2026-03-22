import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { IsolationLevel, TransactionMode } from '../../constants/transactionSettings';
import { databaseService } from '../../services/database.service';
import { tableDataService, type TableDataResponse } from '../../services/tableData.service';
import { tableService } from '../../services/table.service';
import { viewService } from '../../services/view.service';
import { useWorkspaceStore } from '../../store/workspaceStore';
import type { WorkspaceTabMetadataUpdate } from '../../store/tabStore';
import type { TableTabMetadata } from '../../types/tab';
import {
  type LoadDataOverrides,
  parseOrderBy,
  formatOrderBy,
  type TableDataQueryState,
  type TableDataViewMode,
  getColumns,
  getDisplayDbLabel,
  getDisplayName,
  getEndRow,
  getStartRow,
} from './tableDataTabShared';

interface UseTableDataQueryStateArgs {
  tabId: string;
  metadata: TableTabMetadata;
}

export function useTableDataQueryState({ tabId, metadata }: UseTableDataQueryStateArgs) {
  const { t } = useTranslation();
  const updateTabMetadata = useWorkspaceStore((s) => s.updateTabMetadata);

  const initialSort = parseOrderBy(metadata.orderBy);
  const connId = String(metadata.connectionId);
  const objectName = metadata.objectName;
  const objectType = metadata.objectType;
  const isTable = objectType === 'table';
  const catalog = metadata.catalog ?? '';
  const schema = metadata.schema ?? '';

  const [data, setData] = useState<TableDataResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(metadata.currentPage ?? 1);
  const [pageSize, setPageSize] = useState(metadata.pageSize ?? 100);
  const [whereClause, setWhereClause] = useState(metadata.whereClause ?? '');
  const [orderByColumn, setOrderByColumn] = useState(initialSort.orderByColumn);
  const [orderByDirection, setOrderByDirection] = useState(initialSort.orderByDirection);
  const [viewMode, setViewMode] = useState<TableDataViewMode>(metadata.viewMode ?? 'grid');
  const [txMode, setTxMode] = useState<TransactionMode>(TransactionMode.AUTO);
  const [isolationLevel, setIsolationLevel] = useState<IsolationLevel>(IsolationLevel.DEFAULT);
  const [ddlDialogOpen, setDdlDialogOpen] = useState(false);
  const [databases, setDatabases] = useState<string[]>([]);
  const [loadingDatabases, setLoadingDatabases] = useState(false);

  const queryStateRef = useRef<TableDataQueryState>({
    pageSize: metadata.pageSize ?? 100,
    whereClause: metadata.whereClause ?? '',
    orderByColumn: initialSort.orderByColumn,
    orderByDirection: initialSort.orderByDirection,
  });

  const persistMetadata = useCallback(
    (updates: WorkspaceTabMetadataUpdate) => {
      updateTabMetadata(tabId, updates);
    },
    [tabId, updateTabMetadata],
  );

  useEffect(() => {
    queryStateRef.current = {
      pageSize,
      whereClause,
      orderByColumn,
      orderByDirection,
    };
  }, [orderByColumn, orderByDirection, pageSize, whereClause]);

  useEffect(() => {
    persistMetadata({
      currentPage,
      pageSize,
      whereClause,
      orderBy: formatOrderBy(orderByColumn, orderByDirection),
      viewMode,
    });
  }, [currentPage, orderByColumn, orderByDirection, pageSize, persistMetadata, viewMode, whereClause]);

  const loadData = useCallback(
    async (page: number = 1, overrides: LoadDataOverrides = {}) => {
      setLoading(true);
      setError(null);

      try {
        const currentQuery = queryStateRef.current;
        const nextPageSize = overrides.pageSize ?? currentQuery.pageSize;
        const nextWhereClause = (overrides.whereClause ?? currentQuery.whereClause).trim();
        const nextOrderByColumn = (overrides.orderByColumn ?? currentQuery.orderByColumn).trim();
        const nextOrderByDirection = overrides.orderByDirection ?? currentQuery.orderByDirection;

        const result = objectType === 'table'
          ? await tableDataService.getTableData(
              connId,
              objectName,
              catalog || undefined,
              schema || undefined,
              page,
              nextPageSize,
              nextWhereClause || undefined,
              nextOrderByColumn || undefined,
              nextOrderByDirection,
            )
          : await tableDataService.getViewData(
              connId,
              objectName,
              catalog || undefined,
              schema || undefined,
              page,
              nextPageSize,
              nextWhereClause || undefined,
              nextOrderByColumn || undefined,
              nextOrderByDirection,
            );

        setData(result);
        setCurrentPage(result.currentPage || page);

        if (result.pageSize && result.pageSize !== nextPageSize) {
          queryStateRef.current = { ...queryStateRef.current, pageSize: result.pageSize };
          setPageSize(result.pageSize);
        }
      } catch (err: unknown) {
        setError((err as Error).message || t(I18N_KEYS.EXPLORER.LOAD_TABLE_DATA_FAILED));
      } finally {
        setLoading(false);
      }
    },
    [catalog, connId, objectName, objectType, schema, t],
  );

  useEffect(() => {
    void loadData(currentPage);
  }, [loadData]);

  useEffect(() => {
    const loadDatabases = async () => {
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

    void loadDatabases();
  }, [connId]);

  const applyOrderBy = useCallback((column: string, direction: 'asc' | 'desc') => {
    queryStateRef.current = {
      ...queryStateRef.current,
      orderByColumn: column,
      orderByDirection: direction,
    };
    setOrderByColumn(column);
    setOrderByDirection(direction);
    setCurrentPage(1);
    void loadData(1, { orderByColumn: column, orderByDirection: direction });
  }, [loadData]);

  const handleRun = useCallback(() => {
    setCurrentPage(1);
    void loadData(1);
  }, [loadData]);

  const handlePageSizeChange = useCallback((value: number) => {
    queryStateRef.current = { ...queryStateRef.current, pageSize: value };
    setPageSize(value);
    setCurrentPage(1);
    void loadData(1, { pageSize: value });
  }, [loadData]);

  const handleWhereClauseChange = useCallback((value: string) => {
    queryStateRef.current = { ...queryStateRef.current, whereClause: value };
    setWhereClause(value);
  }, []);

  const handleOrderByColumnChange = useCallback((column: string) => {
    if (viewMode === 'transpose') {
      return;
    }

    const nextColumn = column.trim();
    const nextDirection = nextColumn
      ? queryStateRef.current.orderByColumn === nextColumn
        ? queryStateRef.current.orderByDirection
        : 'asc'
      : 'asc';
    applyOrderBy(nextColumn, nextDirection);
  }, [applyOrderBy, viewMode]);

  const handleToggleOrderByDirection = useCallback(() => {
    if (viewMode === 'transpose' || !queryStateRef.current.orderByColumn) {
      return;
    }

    applyOrderBy(
      queryStateRef.current.orderByColumn,
      queryStateRef.current.orderByDirection === 'asc' ? 'desc' : 'asc',
    );
  }, [applyOrderBy, viewMode]);

  const handleGridSortChange = useCallback((column: string | null, direction: 'asc' | 'desc' | null) => {
    if (viewMode === 'transpose') {
      return;
    }

    if (!column || !direction) {
      applyOrderBy('', 'asc');
      return;
    }

    applyOrderBy(column, direction);
  }, [applyOrderBy, viewMode]);

  const handleViewModeChange = useCallback((mode: TableDataViewMode) => {
    setViewMode(mode);
  }, []);

  const handleDatabaseChange = useCallback((db: string) => {
    setCurrentPage(1);
    persistMetadata({ catalog: db, databaseName: db, currentPage: 1 });
  }, [persistMetadata]);

  const handleFirstPage = useCallback(() => {
    void loadData(1);
  }, [loadData]);

  const handlePrevPage = useCallback(() => {
    if (currentPage > 1) {
      void loadData(currentPage - 1);
    }
  }, [currentPage, loadData]);

  const handleNextPage = useCallback(() => {
    if (data && currentPage < data.totalPages) {
      void loadData(currentPage + 1);
    }
  }, [currentPage, data, loadData]);

  const handleLastPage = useCallback(() => {
    if (data && data.totalPages > 0) {
      void loadData(data.totalPages);
    }
  }, [data, loadData]);

  const loadDdl = useCallback(async () => {
    if (objectType === 'table') {
      return tableService.getTableDdl(connId, objectName, catalog || undefined, schema || undefined);
    }

    return viewService.getViewDdl(connId, objectName, catalog || undefined, schema || undefined);
  }, [catalog, connId, objectName, objectType, schema]);

  return {
    data,
    loading,
    error,
    currentPage,
    pageSize,
    whereClause,
    orderByColumn,
    orderByDirection,
    viewMode,
    isTransposeMode: viewMode === 'transpose',
    orderControlsDisabled: viewMode === 'transpose',
    txMode,
    setTxMode,
    isolationLevel,
    setIsolationLevel,
    ddlDialogOpen,
    setDdlDialogOpen,
    databases,
    loadingDatabases,
    isTable,
    loadData,
    handleRun,
    handlePageSizeChange,
    handleWhereClauseChange,
    handleOrderByColumnChange,
    handleToggleOrderByDirection,
    handleViewModeChange,
    handleGridSortChange,
    handleDatabaseChange,
    handleFirstPage,
    handlePrevPage,
    handleNextPage,
    handleLastPage,
    columns: getColumns(data),
    startRow: getStartRow(data, currentPage, pageSize),
    endRow: getEndRow(data, currentPage, pageSize),
    displayDbLabel: getDisplayDbLabel(metadata),
    displayName: getDisplayName(objectName, metadata),
    loadDdl,
  };
}
