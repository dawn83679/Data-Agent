import type { TableTabMetadata } from '../../types/tab';
import { formatCellValue } from './tableDataTabShared';
import { useTableDataQueryState } from './useTableDataQueryState';
import { useTableDataRowActions } from './useTableDataRowActions';

interface UseTableDataTabStateArgs {
  tabId: string;
  metadata: TableTabMetadata;
}

export function useTableDataTabState({ tabId, metadata }: UseTableDataTabStateArgs) {
  const queryState = useTableDataQueryState({ tabId, metadata });

  const rowActions = useTableDataRowActions({
    metadata,
    connId: String(metadata.connectionId),
    objectName: metadata.objectName,
    objectType: metadata.objectType,
    catalog: metadata.catalog ?? '',
    schema: metadata.schema ?? '',
    isTable: queryState.isTable,
    isTransposeMode: queryState.isTransposeMode,
    data: queryState.data,
    currentPage: queryState.currentPage,
    pageSize: queryState.pageSize,
    loadData: queryState.loadData,
  });

  return {
    ...queryState,
    ...rowActions,
    formatCellValue,
  };
}
