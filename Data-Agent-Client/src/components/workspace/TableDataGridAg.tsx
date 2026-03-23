import { useEffect, useMemo, useRef } from 'react';
import type {
  GridApi,
  GridReadyEvent,
  SelectionChangedEvent,
  SortChangedEvent,
} from 'ag-grid-community';
import { AgGridReact } from 'ag-grid-react';
import { useTranslation } from 'react-i18next';
import { useTheme } from '../../hooks/useTheme';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { TableDataResponse } from '../../services/tableData.service';
import {
  buildTableDataGridColumnDefs,
  buildTableDataGridRows,
  buildTransposeTableDataGridColumnDefs,
  buildTransposeTableDataGridRows,
  TABLE_DATA_GRID_ROW_NUMBER_COL_ID,
  type TableDataGridAgRow,
} from './tableDataGridAgUtils';

interface TableDataGridAgProps {
  data: TableDataResponse | null;
  loading: boolean;
  error: string | null;
  viewMode: 'grid' | 'transpose';
  orderByColumn: string;
  orderByDirection: 'asc' | 'desc';
  selectedRowIndex: number | null;
  startRow: number;
  onRowSelectionChange: (selection: { rowIndex: number; row: unknown[] } | null) => void;
  onGridSortChange: (column: string | null, direction: 'asc' | 'desc' | null) => void;
  formatCellValue: (value: unknown) => string;
}

export function TableDataGridAg({
  data,
  loading,
  error,
  viewMode,
  orderByColumn,
  orderByDirection,
  selectedRowIndex,
  startRow,
  onRowSelectionChange,
  onGridSortChange,
  formatCellValue,
}: TableDataGridAgProps) {
  const { t } = useTranslation();
  const { theme } = useTheme();
  const gridApiRef = useRef<GridApi<TableDataGridAgRow> | null>(null);
  const agThemeClass = theme === 'light' ? 'ag-theme-quartz' : 'ag-theme-quartz-dark';
  const syncingSelectionRef = useRef(false);
  const syncingSortRef = useRef(false);
  const isTransposeMode = viewMode === 'transpose';

  const rowData = useMemo(
    () => (isTransposeMode ? buildTransposeTableDataGridRows(data) : buildTableDataGridRows(data, startRow)),
    [data, isTransposeMode, startRow],
  );

  const columnDefs = useMemo(
    () =>
      isTransposeMode
        ? buildTransposeTableDataGridColumnDefs(
            data,
            startRow,
            t(I18N_KEYS.EXPLORER.TRANSPOSE_FIELD),
            formatCellValue,
          )
        : buildTableDataGridColumnDefs(
            data?.headers ?? [],
            formatCellValue,
            data?.totalCount ?? rowData.length,
          ),
    [data, data?.headers, data?.totalCount, formatCellValue, isTransposeMode, rowData.length, startRow, t],
  );

  const hasRows = rowData.length > 0;

  useEffect(() => {
    const api = gridApiRef.current;
    if (!api || isTransposeMode) {
      return;
    }

    syncingSortRef.current = true;
    api.applyColumnState({
      defaultState: { sort: null },
      state: orderByColumn
        ? [{ colId: orderByColumn, sort: orderByDirection }]
        : [],
    });
    syncingSortRef.current = false;
  }, [columnDefs, isTransposeMode, orderByColumn, orderByDirection]);

  useEffect(() => {
    const api = gridApiRef.current;
    if (!api) {
      return;
    }

    syncingSelectionRef.current = true;

    if (isTransposeMode) {
      api.deselectAll();
      syncingSelectionRef.current = false;
      return;
    }

    if (selectedRowIndex == null) {
      api.deselectAll();
      syncingSelectionRef.current = false;
      return;
    }

    let matched = false;
    api.forEachNode((node) => {
      const shouldSelect = node.data?.__rowIndex === selectedRowIndex;
      node.setSelected(shouldSelect);
      if (shouldSelect) {
        matched = true;
      }
    });

    if (!matched) {
      api.deselectAll();
    }

    syncingSelectionRef.current = false;
  }, [isTransposeMode, rowData, selectedRowIndex]);

  const handleGridReady = (event: GridReadyEvent<TableDataGridAgRow>) => {
    gridApiRef.current = event.api;

    if (isTransposeMode) {
      event.api.deselectAll();
      return;
    }

    syncingSortRef.current = true;
    event.api.applyColumnState({
      defaultState: { sort: null },
      state: orderByColumn
        ? [{ colId: orderByColumn, sort: orderByDirection }]
        : [],
    });
    syncingSortRef.current = false;

    if (selectedRowIndex != null) {
      syncingSelectionRef.current = true;
      event.api.forEachNode((node) => {
        node.setSelected(node.data?.__rowIndex === selectedRowIndex);
      });
      syncingSelectionRef.current = false;
    }
  };

  const handleSelectionChanged = (event: SelectionChangedEvent<TableDataGridAgRow>) => {
    if (syncingSelectionRef.current || isTransposeMode) {
      return;
    }

    const selectedNode = event.api.getSelectedNodes()[0];
    if (
      !selectedNode?.data
      || selectedNode.data.__rowIndex == null
      || selectedNode.data.__sourceRow == null
    ) {
      onRowSelectionChange(null);
      return;
    }

    onRowSelectionChange({
      rowIndex: selectedNode.data.__rowIndex,
      row: selectedNode.data.__sourceRow,
    });
  };

  const handleSortChanged = (event: SortChangedEvent<TableDataGridAgRow>) => {
    if (syncingSortRef.current || isTransposeMode) {
      return;
    }

    const sortedColumn = event.api
      .getColumnState()
      .find((column) => column.colId !== TABLE_DATA_GRID_ROW_NUMBER_COL_ID && column.sort);

    onGridSortChange(
      sortedColumn?.colId ?? null,
      (sortedColumn?.sort as 'asc' | 'desc' | undefined) ?? null,
    );
  };

  return (
    <div className="flex-1 min-h-0 overflow-hidden border-t theme-border">
      <div className="relative h-full min-h-0">
        <div className={`workspace-ag-grid ${agThemeClass} h-full min-h-0 w-full`}>
          <AgGridReact<TableDataGridAgRow>
            rowData={rowData}
            columnDefs={columnDefs}
            rowModelType="clientSide"
            animateRows={false}
            suppressMultiSort
            suppressColumnMoveAnimation
            suppressCellFocus={isTransposeMode}
            rowHeight={34}
            headerHeight={36}
            rowSelection={
              isTransposeMode
                ? undefined
                : {
                    mode: 'singleRow',
                    enableClickSelection: true,
                    checkboxes: false,
                  }
            }
            suppressNoRowsOverlay
            defaultColDef={{
              sortable: !isTransposeMode,
              resizable: true,
              minWidth: 120,
              menuTabs: isTransposeMode ? [] : ['generalMenuTab', 'columnsMenuTab'],
            }}
            getRowId={(params) => params.data.__rowId}
            onGridReady={handleGridReady}
            onSelectionChanged={handleSelectionChanged}
            onSortChanged={handleSortChanged}
          />
        </div>

        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-[color:var(--bg-main)]/86 px-4">
            <div className="workspace-ag-grid__state-card flex items-center gap-3">
              <div className="h-4 w-4 rounded-full border-2 border-primary border-t-transparent animate-spin" />
              <span>{t(I18N_KEYS.COMMON.LOADING)}...</span>
            </div>
          </div>
        ) : null}

        {!loading && !hasRows && !error && data ? (
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
            <div className="workspace-ag-grid__state-card">
              {t(I18N_KEYS.EXPLORER.NO_DATA)}
            </div>
          </div>
        ) : null}

        {error ? (
          <div className="absolute inset-0 flex items-center justify-center bg-[color:var(--bg-main)]/90 px-4">
            <div className="workspace-ag-grid__state-card workspace-ag-grid__state-card--error">
              {error}
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}
