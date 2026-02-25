import { useState, useEffect } from 'react';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { useConnectionTree } from '../../hooks/useConnectionTree';
import { useDialogState } from '../../hooks/useDialogState';
import { useConnectionActions } from '../../hooks/useConnectionActions';
import { useDataViewActions } from '../../hooks/useDataViewActions';
import { useDeleteActions } from '../../hooks/useDeleteActions';
import { DataAttributes } from '../../constants/dataAttributes';
import { ExplorerHeader } from './ExplorerHeader';
import { ExplorerTree } from './ExplorerTree';
import { ExplorerDialogs } from './ExplorerDialogs';

export function DatabaseExplorer() {
  const { supportedDbTypes, openTab } = useWorkspaceStore();
  const {
    treeDataState,
    setTreeDataState,
    loadNodeData,
    handleDisconnect,
    isConnectionsLoading,
    refetchConnections,
    deleteMutation,
  } = useConnectionTree();

  const [searchTerm, setSearchTerm] = useState('');

  // Dialog state management
  const dialogState = useDialogState();
  const {
    connectionModalOpen,
    setConnectionModalOpen,
    connectionModalMode,
    setConnectionModalMode,
    connectionEditId,
    setConnectionEditId,
    initialDbType,
    setInitialDbType,
    driverModalOpen,
    setDriverModalOpen,
    selectedDriverDbType,
    setSelectedDriverDbType,
    deleteConfirmId,
    setDeleteConfirmId,
    ddlDialogOpen,
    setDdlDialogOpen,
    selectedDdlNode,
    setSelectedDdlNode,
    tableDataDialogOpen,
    setTableDataDialogOpen,
    selectedTableDataNode,
    setSelectedTableDataNode,
    highlightColumn,
    setHighlightColumn,
    deleteState,
    setDeleteState,
  } = dialogState;

  // Connection actions
  const { openCreateModal, openEditModal } = useConnectionActions({
    setConnectionModalMode,
    setConnectionEditId,
    setInitialDbType,
    setConnectionModalOpen,
  });

  // Data view actions
  const { handleViewDdl, handleViewData, handleOpenQueryConsole, getDdlConfig } = useDataViewActions({
    setSelectedDdlNode,
    setDdlDialogOpen,
    setTableDataDialogOpen,
    setSelectedTableDataNode,
    setHighlightColumn,
    openTab,
    selectedDdlNode,
  });

  // Delete actions
  const { handleDelete, confirmDelete } = useDeleteActions({
    setDeleteState,
    deleteState,
    setTreeDataState,
  });

  const ddlConfig = getDdlConfig();

  useEffect(() => {
    useWorkspaceStore.getState().fetchSupportedDbTypes();
  }, []);

  return (
    <div
      className="flex flex-col h-full overflow-hidden"
      {...{ [DataAttributes.EXPLORER_TREE]: true }}
      onContextMenu={(e) => e.preventDefault()}
    >
      <ExplorerHeader
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        isLoading={isConnectionsLoading}
        onRefresh={refetchConnections}
        supportedDbTypes={supportedDbTypes}
        onAddDatabase={openCreateModal}
        onManageDriver={(dbType) => {
          setSelectedDriverDbType(dbType);
          setDriverModalOpen(true);
        }}
      />

      <ExplorerTree
        data={treeDataState}
        searchTerm={searchTerm}
        isLoading={isConnectionsLoading}
        onLoadData={loadNodeData}
        onDisconnect={handleDisconnect}
        onEditConnection={openEditModal}
        onDeleteConnection={(id) => setDeleteConfirmId(id)}
        onViewDdl={handleViewDdl}
        onViewData={handleViewData}
        onDelete={handleDelete}
        onOpenQueryConsole={handleOpenQueryConsole}
      />

      <ExplorerDialogs
        connectionModalOpen={connectionModalOpen}
        onConnectionModalOpenChange={setConnectionModalOpen}
        connectionModalMode={connectionModalMode}
        connectionEditId={connectionEditId}
        initialDbType={initialDbType}
        driverModalOpen={driverModalOpen}
        onDriverModalOpenChange={setDriverModalOpen}
        selectedDriverDbType={selectedDriverDbType}
        deleteConfirmId={deleteConfirmId}
        onDeleteConfirmIdChange={setDeleteConfirmId}
        onConfirmDeleteConnection={(id) => {
          deleteMutation.mutate(id);
          setDeleteConfirmId(null);
        }}
        isDeleteConnectionPending={deleteMutation.isPending}
        ddlDialogOpen={ddlDialogOpen}
        onDdlDialogOpenChange={setDdlDialogOpen}
        ddlConfig={ddlConfig}
        tableDataDialogOpen={tableDataDialogOpen}
        onTableDataDialogOpenChange={setTableDataDialogOpen}
        selectedTableDataNode={selectedTableDataNode}
        highlightColumn={highlightColumn}
        deleteState={deleteState}
        onDeleteStateChange={setDeleteState}
        onConfirmDelete={confirmDelete}
        onConnectionSuccess={refetchConnections}
      />
    </div>
  );
}
