import { useMemoryEditorState } from './useMemoryEditorState';
import { useMemoryListState } from './useMemoryListState';
import { useMemoryMetadataState } from './useMemoryMetadataState';

export function useMemoryWorkbench() {
  const metadataState = useMemoryMetadataState();
  const listState = useMemoryListState(metadataState.memoryMetadata);
  const editorState = useMemoryEditorState({
    memoryMetadata: metadataState.memoryMetadata,
    subTypeOptionsByType: metadataState.subTypeOptionsByType,
    reloadMemories: listState.loadMemories,
    removeSemanticResult: listState.removeSemanticResult,
  });

  return {
    page: listState.page,
    filterForm: listState.filterForm,
    memoryForm: editorState.memoryForm,
    formErrors: editorState.formErrors,
    listItems: listState.listItems,
    memoryTypeOptions: editorState.memoryTypeOptions,
    filterMemoryTypeOptions: listState.filterMemoryTypeOptions,
    filterScopeOptions: listState.filterScopeOptions,
    availableSubTypeOptions: editorState.availableSubTypeOptions,
    editorScopeOptions: editorState.editorScopeOptions,
    sourceTypeOptions: editorState.sourceTypeOptions,
    metadataLoading: metadataState.metadataLoading,
    listLoading: listState.listLoading,
    detailLoading: editorState.detailLoading,
    semanticLoading: listState.semanticLoading,
    submitting: editorState.submitting,
    semanticSearchEnabled: listState.semanticSearchEnabled,
    selectedMemoryId: editorState.selectedMemoryId,
    selectedMemory: editorState.selectedMemory,
    isDialogOpen: editorState.isDialogOpen,
    isEditing: editorState.isEditing,
    isSemanticMode: listState.isSemanticMode,
    handleFilterInputChange: listState.handleFilterInputChange,
    handleFormInputChange: editorState.handleFormInputChange,
    handleApplyFilters: listState.handleApplyFilters,
    handleResetFilters: listState.handleResetFilters,
    handleSemanticToggle: listState.handleSemanticToggle,
    refreshWorkspace: listState.refreshWorkspace,
    startCreateMemory: editorState.startCreateMemory,
    openMemory: editorState.openMemory,
    closeDetailDialog: editorState.closeDetailDialog,
    handleSubmit: editorState.handleSubmit,
    handleDisable: editorState.handleDisable,
    handleEnable: editorState.handleEnable,
    handleDelete: editorState.handleDelete,
    confirmDialog: editorState.confirmDialog,
    goToPreviousPage: listState.goToPreviousPage,
    goToNextPage: listState.goToNextPage,
  };
}
