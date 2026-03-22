import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useToast } from '../../hooks/useToast';
import { columnService, type ColumnMetadata } from '../../services/column.service';
import { primaryKeyService } from '../../services/primaryKey.service';
import { tableDataService, type TableDataResponse } from '../../services/tableData.service';
import type { ExecuteSqlResponse } from '../../types/sql';
import type { TableTabMetadata } from '../../types/tab';
import type { LoadDataOverrides, SelectedTableRow } from './tableDataTabShared';
import { toRowMatchValue } from './tableDataTabShared';

type DeleteConfirmMode = 'single' | 'force';

interface UseTableDataRowActionsArgs {
  metadata: TableTabMetadata;
  connId: string;
  objectName: string;
  objectType: TableTabMetadata['objectType'];
  catalog: string;
  schema: string;
  isTable: boolean;
  isTransposeMode: boolean;
  data: TableDataResponse | null;
  currentPage: number;
  pageSize: number;
  loadData: (page?: number, overrides?: LoadDataOverrides) => Promise<void>;
}

export function useTableDataRowActions({
  metadata,
  connId,
  objectName,
  objectType,
  catalog,
  schema,
  isTable,
  isTransposeMode,
  data,
  currentPage,
  pageSize,
  loadData,
}: UseTableDataRowActionsArgs) {
  const { t } = useTranslation();
  const toast = useToast();

  const [isAddingRow, setIsAddingRow] = useState(false);
  const [newRowValues, setNewRowValues] = useState<Record<string, string>>({});
  const [columnMetadata, setColumnMetadata] = useState<ColumnMetadata[]>([]);
  const [loadingColumns, setLoadingColumns] = useState(false);
  const [insertSubmitting, setInsertSubmitting] = useState(false);
  const [insertError, setInsertError] = useState<string | null>(null);
  const [selectedRow, setSelectedRow] = useState<SelectedTableRow | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletePending, setDeletePending] = useState(false);
  const [deleteConfirmMode, setDeleteConfirmMode] = useState<DeleteConfirmMode>('single');
  const [deleteForceCount, setDeleteForceCount] = useState(0);
  const [pendingDeleteMatchValues, setPendingDeleteMatchValues] = useState<Array<{ columnName: string; value: unknown }>>([]);

  const executeDelete = useCallback(async (
    matchValues: Array<{ columnName: string; value: unknown }>,
    force: boolean,
  ): Promise<ExecuteSqlResponse> => {
    return tableDataService.deleteRow({
      connectionId: metadata.connectionId,
      tableName: objectName,
      catalog: catalog || undefined,
      schema: schema || undefined,
      matchValues,
      force,
    });
  }, [catalog, metadata.connectionId, objectName, schema]);

  const resetRowState = useCallback(() => {
    setSelectedRow(null);
    setDeleteConfirmOpen(false);
    setDeleteConfirmMode('single');
    setDeleteForceCount(0);
    setPendingDeleteMatchValues([]);
    setIsAddingRow(false);
    setNewRowValues({});
    setInsertError(null);
  }, []);

  const closeDeleteConfirm = useCallback(() => {
    setDeleteConfirmOpen(false);
    setDeleteConfirmMode('single');
    setDeleteForceCount(0);
    setPendingDeleteMatchValues([]);
  }, []);

  useEffect(() => {
    resetRowState();
  }, [catalog, objectName, objectType, resetRowState, schema]);

  useEffect(() => {
    if (isTransposeMode) {
      resetRowState();
    }
  }, [isTransposeMode, resetRowState]);

  const handleGridSelectionChange = useCallback((selection: { rowIndex: number; row: unknown[] } | null) => {
    if (isTransposeMode) {
      setSelectedRow(null);
      return;
    }

    setSelectedRow(selection ? { rowIndex: selection.rowIndex, row: selection.row } : null);
  }, [isTransposeMode]);

  const handleAddRow = useCallback(() => {
    if (isTransposeMode) {
      toast.warning(t(I18N_KEYS.EXPLORER.TRANSPOSE_READONLY));
      return;
    }

    if (!isTable) {
      toast.warning(t(I18N_KEYS.EXPLORER.VIEW_READONLY));
      return;
    }

    setIsAddingRow(true);
    setNewRowValues({});
    setInsertError(null);
    setLoadingColumns(true);
    setSelectedRow(null);

    columnService
      .listColumns(connId, objectName, catalog || undefined, schema || undefined)
      .then((cols) => setColumnMetadata(cols || []))
      .catch(() => setColumnMetadata([]))
      .finally(() => setLoadingColumns(false));

    if (data && data.totalPages > 0) {
      void loadData(data.totalPages);
    }
  }, [catalog, connId, data, isTable, isTransposeMode, loadData, objectName, schema, t, toast]);

  const handleCancelAddRow = useCallback(() => {
    setIsAddingRow(false);
    setNewRowValues({});
    setInsertError(null);
  }, []);

  const handleNewRowValueChange = useCallback((colName: string, value: string) => {
    setNewRowValues((prev) => ({ ...prev, [colName]: value }));
    setInsertError(null);
  }, []);

  const handleConfirmInsert = useCallback(async () => {
    const editableColumns = columnMetadata.filter((column) => !column.isAutoIncrement);
    if (editableColumns.length === 0) {
      setInsertError('No editable columns');
      return;
    }

    const values: Array<{ columnName: string; value: unknown }> = [];
    for (const column of editableColumns) {
      const value = (newRowValues[column.name] ?? '').trim();
      const nullable = column.nullable ?? true;

      if (value === '' || value.toUpperCase() === 'NULL') {
        if (!nullable) {
          setInsertError(`Column ${column.name} is required`);
          return;
        }
        values.push({ columnName: column.name, value: null });
        continue;
      }
      values.push({ columnName: column.name, value });
    }

    setInsertSubmitting(true);
    setInsertError(null);

    try {
      const result = await tableDataService.insertRow({
        connectionId: metadata.connectionId,
        tableName: objectName,
        catalog: catalog || undefined,
        schema: schema || undefined,
        values,
      });

      if (result.success) {
        toast.success(t(I18N_KEYS.EXPLORER.ROW_INSERT_SUCCESS));
        setIsAddingRow(false);
        setNewRowValues({});
        const lastPage = data ? Math.max(1, Math.ceil((data.totalCount + 1) / pageSize)) : 1;
        await loadData(lastPage);
      } else {
        setInsertError(result.errorMessage || 'Insert failed');
      }
    } catch (err) {
      setInsertError((err as Error).message);
    } finally {
      setInsertSubmitting(false);
    }
  }, [catalog, columnMetadata, data, loadData, metadata.connectionId, newRowValues, objectName, pageSize, schema, t, toast]);

  const handleDeleteRow = useCallback(() => {
    if (isTransposeMode) {
      toast.warning(t(I18N_KEYS.EXPLORER.TRANSPOSE_READONLY));
      return;
    }

    if (!isTable) {
      toast.warning(t(I18N_KEYS.EXPLORER.VIEW_READONLY));
      return;
    }

    if (!selectedRow || !data) {
      toast.warning(t(I18N_KEYS.EXPLORER.SELECT_ROW_TO_DELETE));
      return;
    }

    setDeleteConfirmMode('single');
    setDeleteForceCount(0);
    setPendingDeleteMatchValues([]);
    setDeleteConfirmOpen(true);
  }, [data, isTable, isTransposeMode, selectedRow, t, toast]);

  const handleConfirmDelete = useCallback(async () => {
    const currentData = data;
    setDeletePending(true);

    try {
      let matchValues = pendingDeleteMatchValues;
      let forceDelete = deleteConfirmMode === 'force';

      if (!forceDelete) {
        if (!selectedRow || !currentData) {
          return;
        }

        const primaryKeys = await primaryKeyService.listPrimaryKeys(
          connId,
          objectName,
          catalog || undefined,
          schema || undefined,
        );
        const whereColumns =
          primaryKeys?.length && primaryKeys[0]?.columnNames?.length
            ? primaryKeys[0].columnNames
            : currentData.headers;

        matchValues = whereColumns
          .map((column) => {
            const columnIndex = currentData.headers.indexOf(column);
            if (columnIndex === -1) {
              return null;
            }

            return {
              columnName: column,
              value: toRowMatchValue(selectedRow.row[columnIndex]),
            };
          })
          .filter((entry): entry is { columnName: string; value: unknown } => entry != null);

        if (matchValues.length === 0) {
          toast.error('Cannot build DELETE: no matching columns');
          return;
        }
      }

      let result = await executeDelete(matchValues, forceDelete);

      const requiresForce = result.messages?.some((message) => message.code === 'DELETE_REQUIRES_FORCE') ?? false;
      if (!result.success && requiresForce) {
        setPendingDeleteMatchValues(matchValues);
        setDeleteForceCount(result.affectedRows);
        setDeleteConfirmMode('force');
        return;
      }

      if (result.success) {
        toast.success(t(I18N_KEYS.EXPLORER.ROW_DELETE_SUCCESS));
        closeDeleteConfirm();
        setSelectedRow(null);
        const remainingCount = Math.max((currentData?.totalCount ?? 1) - 1, 0);
        const nextPage =
          remainingCount === 0 ? 1 : Math.min(currentPage, Math.ceil(remainingCount / pageSize));
        await loadData(nextPage);
      } else {
        toast.error(result.errorMessage || 'Delete failed');
      }
    } catch (err) {
      toast.error((err as Error).message);
    } finally {
      setDeletePending(false);
    }
  }, [catalog, closeDeleteConfirm, connId, currentPage, data, deleteConfirmMode, executeDelete, loadData, objectName, pageSize, pendingDeleteMatchValues, schema, selectedRow, toast]);

  return {
    isAddingRow,
    newRowValues,
    columnMetadata,
    loadingColumns,
    insertSubmitting,
    insertError,
    selectedRowIndex: selectedRow?.rowIndex ?? null,
    hasSelectedRow: selectedRow != null,
    deleteConfirmOpen,
    closeDeleteConfirm,
    deletePending,
    deleteConfirmMode,
    deleteForceCount,
    handleGridSelectionChange,
    handleAddRow,
    handleCancelAddRow,
    handleNewRowValueChange,
    handleConfirmInsert,
    handleDeleteRow,
    handleConfirmDelete,
  };
}
