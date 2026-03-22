import { useTranslation } from 'react-i18next';
import { Button } from '../ui/Button';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { TableTabMetadata } from '../../types/tab';
import { DdlViewerDialog } from '../explorer/DdlViewerDialog';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/Dialog';
import { TABLE_DATA_PAGE_SIZE_OPTIONS } from './tableDataTabConstants';
import { TableDataFilterBar } from './TableDataFilterBar';
import { TableDataGridAg } from './TableDataGridAg';
import { TableDataInsertBar } from './TableDataInsertBar';
import { TableDataPagination } from './TableDataPagination';
import { TableDataToolbar } from './TableDataToolbar';
import { useTableDataTabState } from './useTableDataTabState';

interface TableDataTabProps {
  tabId: string;
  metadata: TableTabMetadata;
}

export function TableDataTab({ tabId, metadata }: TableDataTabProps) {
  const { t } = useTranslation();
  const {
    data,
    loading,
    error,
    currentPage,
    pageSize,
    whereClause,
    orderByColumn,
    orderByDirection,
    viewMode,
    orderControlsDisabled,
    txMode,
    setTxMode,
    isolationLevel,
    setIsolationLevel,
    ddlDialogOpen,
    setDdlDialogOpen,
    isAddingRow,
    newRowValues,
    columnMetadata,
    loadingColumns,
    insertSubmitting,
    insertError,
    selectedRowIndex,
    hasSelectedRow,
    deleteConfirmOpen,
    closeDeleteConfirm,
    deletePending,
    deleteConfirmMode,
    deleteForceCount,
    databases,
    loadingDatabases,
    isTable,
    handleRun,
    handlePageSizeChange,
    handleWhereClauseChange,
    handleOrderByColumnChange,
    handleToggleOrderByDirection,
    handleViewModeChange,
    handleGridSortChange,
    handleGridSelectionChange,
    handleDatabaseChange,
    handleFirstPage,
    handlePrevPage,
    handleNextPage,
    handleLastPage,
    handleAddRow,
    handleCancelAddRow,
    handleNewRowValueChange,
    handleConfirmInsert,
    handleDeleteRow,
    handleConfirmDelete,
    formatCellValue,
    columns,
    startRow,
    endRow,
    displayDbLabel,
    displayName,
    loadDdl,
  } = useTableDataTabState({ tabId, metadata });

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden">
      <TableDataToolbar
        loading={loading}
        isTable={isTable}
        viewMode={viewMode}
        hasRowSelection={hasSelectedRow}
        deletePending={deletePending}
        txMode={txMode}
        isolationLevel={isolationLevel}
        displayDbLabel={displayDbLabel}
        connectionName={metadata.connectionName}
        databases={databases}
        loadingDatabases={loadingDatabases}
        onRun={handleRun}
        onRefresh={() => void handleRun()}
        onTransactionModeChange={setTxMode}
        onIsolationLevelChange={setIsolationLevel}
        onAddRow={handleAddRow}
        onDeleteRow={handleDeleteRow}
        onOpenDdl={() => setDdlDialogOpen(true)}
        onDatabaseChange={handleDatabaseChange}
        onViewModeChange={handleViewModeChange}
      />

      <TableDataFilterBar
        columns={columns}
        whereClause={whereClause}
        orderByColumn={orderByColumn}
        orderByDirection={orderByDirection}
        orderControlsDisabled={orderControlsDisabled}
        onWhereClauseChange={handleWhereClauseChange}
        onRun={handleRun}
        onOrderByColumnChange={handleOrderByColumnChange}
        onToggleOrderByDirection={handleToggleOrderByDirection}
      />

      <TableDataInsertBar
        isVisible={isAddingRow && isTable}
        headers={columns}
        columnMetadata={columnMetadata}
        loadingColumns={loadingColumns}
        insertSubmitting={insertSubmitting}
        insertError={insertError}
        newRowValues={newRowValues}
        onNewRowValueChange={handleNewRowValueChange}
        onConfirmInsert={handleConfirmInsert}
        onCancelInsert={handleCancelAddRow}
      />

      <DdlViewerDialog
        open={ddlDialogOpen}
        onOpenChange={setDdlDialogOpen}
        title={isTable ? t(I18N_KEYS.EXPLORER.TABLE_DDL) : t(I18N_KEYS.EXPLORER.VIEW_DDL_TITLE)}
        displayName={displayName}
        loadDdl={loadDdl}
      />

      <Dialog open={deleteConfirmOpen} onOpenChange={(open) => (!open ? closeDeleteConfirm() : undefined)}>
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle>{t(I18N_KEYS.EXPLORER.DELETE_ROW)}</DialogTitle>
            <DialogDescription>
              {deleteConfirmMode === 'force'
                ? `${t(I18N_KEYS.EXPLORER.DELETE_ROW_FORCE_PROMPT, { count: deleteForceCount })} ${t(I18N_KEYS.EXPLORER.DELETE_ROW_FORCE_CONTINUE)}`
                : t(I18N_KEYS.EXPLORER.DELETE_ROW_CONFIRM)}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={closeDeleteConfirm} disabled={deletePending}>
              {t(I18N_KEYS.CONNECTIONS.CANCEL)}
            </Button>
            <Button variant="destructive" disabled={deletePending} onClick={handleConfirmDelete}>
              {deletePending
                ? t(I18N_KEYS.COMMON.LOADING) + '...'
                : deleteConfirmMode === 'force'
                  ? t(I18N_KEYS.EXPLORER.DELETE_ROW_FORCE_ACTION)
                  : t(I18N_KEYS.EXPLORER.DELETE_ROW)}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <TableDataGridAg
        data={data}
        loading={loading}
        error={error}
        viewMode={viewMode}
        orderByColumn={orderByColumn}
        orderByDirection={orderByDirection}
        selectedRowIndex={selectedRowIndex}
        startRow={startRow}
        onRowSelectionChange={handleGridSelectionChange}
        onGridSortChange={handleGridSortChange}
        formatCellValue={formatCellValue}
      />

      {!loading && !error && data && data.totalCount > 0 && (
        <TableDataPagination
          currentPage={currentPage}
          pageSize={pageSize}
          totalCount={data.totalCount}
          totalPages={data.totalPages}
          startRow={startRow}
          endRow={endRow}
          pageSizeOptions={TABLE_DATA_PAGE_SIZE_OPTIONS}
          onPageSizeChange={handlePageSizeChange}
          onFirstPage={handleFirstPage}
          onPrevPage={handlePrevPage}
          onNextPage={handleNextPage}
          onLastPage={handleLastPage}
        />
      )}
    </div>
  );
}
