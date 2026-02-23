import * as React from 'react';
import { useState } from 'react';
import { Database, Plus, Search, RefreshCw, Settings } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { useTranslation } from 'react-i18next';
import { Tree as ArboristTree, NodeApi } from 'react-arborist';
import { ConnectionFormModal, type ConnectionFormMode } from '../common/ConnectionFormModal';
import { DriverManageModal } from '../common/DriverManageModal';
import { DeleteConnectionDialog } from './DeleteConnectionDialog';
import { DeleteTableDialog } from './DeleteTableDialog';
import { DdlViewerDialog } from './DdlViewerDialog';
import { TableDataDialog } from './TableDataDialog';
import { ExplorerTreeNode } from './ExplorerTreeNode';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSub,
  DropdownMenuSubTrigger,
  DropdownMenuSubContent,
  DropdownMenuPortal,
} from '../ui/DropdownMenu';
import { useConnectionTree, type ExplorerNode } from '../../hooks/useConnectionTree';
import { ExplorerTreeConfig, ExplorerNodeType } from '../../constants/explorer';
import { DataAttributes } from '../../constants/dataAttributes';
import { tableService } from '../../services/table.service';
import { databaseService } from '../../services/database.service';
import { viewService } from '../../services/view.service';
import { functionService } from '../../services/function.service';
import { procedureService } from '../../services/procedure.service';
import { triggerService } from '../../services/trigger.service';
import { databaseExportImportService } from '../../services/databaseExportImport.service';
import { useToast } from '../../hooks/useToast';

export function DatabaseExplorer() {
  const { t } = useTranslation();
  const { supportedDbTypes } = useWorkspaceStore();
  const toast = useToast();
  const {
    treeDataState,
    setTreeDataState,
    loadNodeData,
    handleDisconnect,
    isConnectionsLoading,
    refetchConnections,
    deleteMutation,
    queryClient,
  } = useConnectionTree();

  const [searchTerm, setSearchTerm] = useState('');
  const [connectionModalOpen, setConnectionModalOpen] = useState(false);
  const [connectionModalMode, setConnectionModalMode] = useState<ConnectionFormMode>('create');
  const [connectionEditId, setConnectionEditId] = useState<number | undefined>(undefined);
  const [initialDbType, setInitialDbType] = useState<string | undefined>(undefined);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [driverModalOpen, setDriverModalOpen] = useState(false);
  const [selectedDriverDbType, setSelectedDriverDbType] = useState<string>('');

  const [ddlDialogOpen, setDdlDialogOpen] = useState(false);
  const [selectedDdlNode, setSelectedDdlNode] = useState<ExplorerNode | null>(null);

  const [deleteTableDialogOpen, setDeleteTableDialogOpen] = useState(false);
  const [deleteTableNode, setDeleteTableNode] = useState<ExplorerNode | null>(null);
  const [isDeletingTable, setIsDeletingTable] = useState(false);

  const [deleteViewDialogOpen, setDeleteViewDialogOpen] = useState(false);
  const [deleteViewNode, setDeleteViewNode] = useState<ExplorerNode | null>(null);
  const [isDeletingView, setIsDeletingView] = useState(false);

  const [deleteFunctionDialogOpen, setDeleteFunctionDialogOpen] = useState(false);
  const [deleteFunctionNode, setDeleteFunctionNode] = useState<ExplorerNode | null>(null);
  const [isDeletingFunction, setIsDeletingFunction] = useState(false);

  const [deleteProcedureDialogOpen, setDeleteProcedureDialogOpen] = useState(false);
  const [deleteProcedureNode, setDeleteProcedureNode] = useState<ExplorerNode | null>(null);
  const [isDeletingProcedure, setIsDeletingProcedure] = useState(false);

  const [deleteTriggerDialogOpen, setDeleteTriggerDialogOpen] = useState(false);
  const [deleteTriggerNode, setDeleteTriggerNode] = useState<ExplorerNode | null>(null);
  const [isDeletingTrigger, setIsDeletingTrigger] = useState(false);

  const [deleteFolderDialogOpen, setDeleteFolderDialogOpen] = useState(false);
  const [deleteFolderNode, setDeleteFolderNode] = useState<ExplorerNode | null>(null);
  const [isDeletingFolder, setIsDeletingFolder] = useState(false);

  const [deleteDatabaseDialogOpen, setDeleteDatabaseDialogOpen] = useState(false);
  const [deleteDatabaseNode, setDeleteDatabaseNode] = useState<ExplorerNode | null>(null);
  const [isDeletingDatabase, setIsDeletingDatabase] = useState(false);

  React.useEffect(() => {
    useWorkspaceStore.getState().fetchSupportedDbTypes();
  }, []);

  const openCreateModal = (dbType?: string) => {
    setConnectionModalMode('create');
    setConnectionEditId(undefined);
    setInitialDbType(dbType);
    setConnectionModalOpen(true);
  };

  const openEditModal = (connId: number) => {
    setConnectionModalMode('edit');
    setConnectionEditId(connId);
    setConnectionModalOpen(true);
  };

  const handleViewDdl = (node: ExplorerNode) => {
    setSelectedDdlNode(node);
    setDdlDialogOpen(true);
  };

  const [tableDataDialogOpen, setTableDataDialogOpen] = useState(false);
  const [selectedTableDataNode, setSelectedTableDataNode] = useState<ExplorerNode | null>(null);

  const [highlightColumn, setHighlightColumn] = useState<string | undefined>(undefined);

  const handleViewData = (node: ExplorerNode, highlightCol?: string) => {
    if (!node.connectionId) return;
    setHighlightColumn(highlightCol);
    setSelectedTableDataNode(node);
    setTableDataDialogOpen(true);
  };

  const handleDeleteTable = (node: ExplorerNode) => {
    if (!node.connectionId) return;
    setDeleteTableNode(node);
    setDeleteTableDialogOpen(true);
  };

  const confirmDeleteTable = async () => {
    if (!deleteTableNode?.connectionId) return;

    setIsDeletingTable(true);
    try {
      await tableService.deleteTable(
        deleteTableNode.connectionId,
        deleteTableNode.name,
        deleteTableNode.catalog,
        deleteTableNode.schema
      );

      // Remove the deleted table node from tree state
      const tableNodeId = deleteTableNode.id;
      setTreeDataState((prev) => {
        const removeFromTree = (nodes: ExplorerNode[]): ExplorerNode[] => {
          return nodes
            .map((node) => {
              if (node.id === tableNodeId) {
                // Found the node to remove, return null (will be filtered out)
                return null;
              }
              if (node.children && node.children.length > 0) {
                return { ...node, children: removeFromTree(node.children) };
              }
              return node;
            })
            .filter((n): n is ExplorerNode => n !== null);
        };
        return removeFromTree(prev);
      });
    } catch (error) {
      console.error('Failed to delete table:', error);
      alert(t('explorer.delete_table_failed'));
    } finally {
      setIsDeletingTable(false);
      setDeleteTableNode(null);
    }
  };

  const handleDeleteView = (node: ExplorerNode) => {
    if (!node.connectionId) return;
    setDeleteViewNode(node);
    setDeleteViewDialogOpen(true);
  };

  const confirmDeleteView = async () => {
    if (!deleteViewNode?.connectionId) return;

    setIsDeletingView(true);
    try {
      await viewService.deleteView(
        deleteViewNode.connectionId,
        deleteViewNode.name,
        deleteViewNode.catalog,
        deleteViewNode.schema
      );

      // Remove the deleted view node from tree state
      const viewNodeId = deleteViewNode.id;
      setTreeDataState((prev) => {
        const removeFromTree = (nodes: ExplorerNode[]): ExplorerNode[] => {
          return nodes
            .map((node) => {
              if (node.id === viewNodeId) {
                return null;
              }
              if (node.children && node.children.length > 0) {
                return { ...node, children: removeFromTree(node.children) };
              }
              return node;
            })
            .filter((n): n is ExplorerNode => n !== null);
        };
        return removeFromTree(prev);
      });
    } catch (error) {
      console.error('Failed to delete view:', error);
      alert(t('explorer.delete_view_failed'));
    } finally {
      setIsDeletingView(false);
      setDeleteViewNode(null);
    }
  };

  const handleDeleteFunction = (node: ExplorerNode) => {
    if (!node.connectionId) return;
    setDeleteFunctionNode(node);
    setDeleteFunctionDialogOpen(true);
  };

  const confirmDeleteFunction = async () => {
    if (!deleteFunctionNode?.connectionId) return;

    setIsDeletingFunction(true);
    try {
      await functionService.deleteFunction(
        deleteFunctionNode.connectionId,
        deleteFunctionNode.name,
        deleteFunctionNode.catalog,
        deleteFunctionNode.schema
      );

      const functionNodeId = deleteFunctionNode.id;
      setTreeDataState((prev) => {
        const removeFromTree = (nodes: ExplorerNode[]): ExplorerNode[] => {
          return nodes
            .map((node) => {
              if (node.id === functionNodeId) {
                return null;
              }
              if (node.children && node.children.length > 0) {
                return { ...node, children: removeFromTree(node.children) };
              }
              return node;
            })
            .filter((n): n is ExplorerNode => n !== null);
        };
        return removeFromTree(prev);
      });
    } catch (error) {
      console.error('Failed to delete function:', error);
      alert(t('explorer.delete_function_failed'));
    } finally {
      setIsDeletingFunction(false);
      setDeleteFunctionNode(null);
    }
  };

  const handleDeleteProcedure = (node: ExplorerNode) => {
    if (!node.connectionId) return;
    setDeleteProcedureNode(node);
    setDeleteProcedureDialogOpen(true);
  };

  const confirmDeleteProcedure = async () => {
    if (!deleteProcedureNode?.connectionId) return;

    setIsDeletingProcedure(true);
    try {
      await procedureService.deleteProcedure(
        deleteProcedureNode.connectionId,
        deleteProcedureNode.name,
        deleteProcedureNode.catalog,
        deleteProcedureNode.schema
      );

      const procedureNodeId = deleteProcedureNode.id;
      setTreeDataState((prev) => {
        const removeFromTree = (nodes: ExplorerNode[]): ExplorerNode[] => {
          return nodes
            .map((node) => {
              if (node.id === procedureNodeId) {
                return null;
              }
              if (node.children && node.children.length > 0) {
                return { ...node, children: removeFromTree(node.children) };
              }
              return node;
            })
            .filter((n): n is ExplorerNode => n !== null);
        };
        return removeFromTree(prev);
      });
    } catch (error) {
      console.error('Failed to delete procedure:', error);
      alert(t('explorer.delete_procedure_failed'));
    } finally {
      setIsDeletingProcedure(false);
      setDeleteProcedureNode(null);
    }
  };

  const handleDeleteTrigger = (node: ExplorerNode) => {
    if (!node.connectionId) return;
    setDeleteTriggerNode(node);
    setDeleteTriggerDialogOpen(true);
  };

  const confirmDeleteTrigger = async () => {
    if (!deleteTriggerNode?.connectionId) return;

    setIsDeletingTrigger(true);
    try {
      await triggerService.deleteTrigger(
        deleteTriggerNode.connectionId,
        deleteTriggerNode.name,
        deleteTriggerNode.catalog,
        deleteTriggerNode.schema
      );

      const triggerNodeId = deleteTriggerNode.id;
      setTreeDataState((prev) => {
        const removeFromTree = (nodes: ExplorerNode[]): ExplorerNode[] => {
          return nodes
            .map((node) => {
              if (node.id === triggerNodeId) {
                return null;
              }
              if (node.children && node.children.length > 0) {
                return { ...node, children: removeFromTree(node.children) };
              }
              return node;
            })
            .filter((n): n is ExplorerNode => n !== null);
        };
        return removeFromTree(prev);
      });
    } catch (error) {
      console.error('Failed to delete trigger:', error);
      alert(t('explorer.delete_trigger_failed'));
    } finally {
      setIsDeletingTrigger(false);
      setDeleteTriggerNode(null);
    }
  };

  const handleDeleteAllInFolder = (node: ExplorerNode) => {
    if (!node.connectionId) return;
    setDeleteFolderNode(node);
    setDeleteFolderDialogOpen(true);
  };

  const confirmDeleteFolder = async () => {
    if (!deleteFolderNode?.connectionId || !deleteFolderNode.children) return;

    setIsDeletingFolder(true);
    try {
      const children = deleteFolderNode.children.filter(c => c.type !== 'empty');
      const folderName = deleteFolderNode.name;

      // Delete each item in the folder
      for (const child of children) {
        try {
          if (child.type === 'table') {
            await tableService.deleteTable(
              deleteFolderNode.connectionId,
              child.name,
              deleteFolderNode.catalog,
              deleteFolderNode.schema
            );
          } else if (child.type === 'view') {
            await viewService.deleteView(
              deleteFolderNode.connectionId,
              child.name,
              deleteFolderNode.catalog,
              deleteFolderNode.schema
            );
          } else if (child.type === 'function') {
            await functionService.deleteFunction(
              deleteFolderNode.connectionId,
              child.name,
              deleteFolderNode.catalog,
              deleteFolderNode.schema
            );
          } else if (child.type === 'procedure') {
            await procedureService.deleteProcedure(
              deleteFolderNode.connectionId,
              child.name,
              deleteFolderNode.catalog,
              deleteFolderNode.schema
            );
          } else if (child.type === 'trigger') {
            await triggerService.deleteTrigger(
              deleteFolderNode.connectionId,
              child.name,
              deleteFolderNode.catalog,
              deleteFolderNode.schema
            );
          }
        } catch (err) {
          console.error(`Failed to delete ${child.type} ${child.name}:`, err);
        }
      }

      // Remove the folder node from tree state
      const folderNodeId = deleteFolderNode.id;
      setTreeDataState((prev) => {
        const removeFolder = (nodes: ExplorerNode[]): ExplorerNode[] => {
          return nodes
            .filter((node) => node.id !== folderNodeId)
            .map((node) => {
              if (node.children && node.children.length > 0) {
                return { ...node, children: removeFolder(node.children) };
              }
              return node;
            });
        };
        return removeFolder(prev);
      });

      // Refresh all queries to update the tree
      await queryClient.invalidateQueries();
    } catch (error) {
      console.error('Failed to delete folder items:', error);
      alert(t('explorer.delete_folder_failed'));
    } finally {
      setIsDeletingFolder(false);
      setDeleteFolderNode(null);
    }
  };

  const handleDeleteDatabase = (node: ExplorerNode) => {
    if (!node.connectionId) return;
    setDeleteDatabaseNode(node);
    setDeleteDatabaseDialogOpen(true);
  };

  const handleExportDatabase = async (node: ExplorerNode) => {
    if (!node.connectionId) return;
    try {
      const ddl = await databaseExportImportService.exportDatabase(
        String(node.connectionId),
        node.name
      );
      // Create and download a .sql file
      const blob = new Blob([ddl], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${node.name}.sql`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      toast.success(t('explorer.export_database_success'));
    } catch (error) {
      console.error('Failed to export database:', error);
      toast.error(t('explorer.export_database_failed'));
    }
  };

  const handleImportDatabase = async (node: ExplorerNode) => {
    if (!node.connectionId) return;
    // Create a file input to select SQL file
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.sql,.txt';
    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;
      try {
        await databaseExportImportService.importFile(String(node.connectionId), file);
        toast.success(t('explorer.import_database_success'));
        // Refresh all queries to update the tree
        await queryClient.invalidateQueries();
      } catch (error) {
        console.error('Failed to import database:', error);
        toast.error(t('explorer.import_database_failed'));
      }
    };
    input.click();
  };

  const confirmDeleteDatabase = async () => {
    if (!deleteDatabaseNode?.connectionId) return;

    setIsDeletingDatabase(true);
    try {
      await databaseService.deleteDatabase(
        deleteDatabaseNode.connectionId,
        deleteDatabaseNode.name
      );

      // Remove the deleted database node from tree state
      const dbNodeId = deleteDatabaseNode.id;
      setTreeDataState((prev) => {
        const removeFromTree = (nodes: ExplorerNode[]): ExplorerNode[] => {
          return nodes
            .map((node) => {
              if (node.id === dbNodeId) {
                return null;
              }
              if (node.children && node.children.length > 0) {
                return { ...node, children: removeFromTree(node.children) };
              }
              return node;
            })
            .filter((n): n is ExplorerNode => n !== null);
        };
        return removeFromTree(prev);
      });
    } catch (error) {
      console.error('Failed to delete database:', error);
      alert(t('explorer.delete_database_failed'));
    } finally {
      setIsDeletingDatabase(false);
      setDeleteDatabaseNode(null);
    }
  };

  const getDdlConfig = () => {
    const node = selectedDdlNode;
    if (!node?.connectionId) return null;
    const connId = node.connectionId;
    const catalog = node.catalog ?? '';
    const schema = node.schema;
    const displayName = [catalog, schema, node.name].filter(Boolean).join('.');
    const objectName = node.objectName ?? node.name;

    switch (node.type) {
      case ExplorerNodeType.TABLE:
        return {
          title: t('explorer.table_ddl'),
          displayName,
          loadDdl: () => tableService.getTableDdl(connId, objectName, catalog, schema),
        };
      case ExplorerNodeType.VIEW:
        return {
          title: t('explorer.view_ddl_title'),
          displayName,
          loadDdl: () => viewService.getViewDdl(connId, objectName, catalog, schema),
        };
      case ExplorerNodeType.FUNCTION:
        return {
          title: t('explorer.function_ddl_title'),
          displayName,
          loadDdl: () => functionService.getFunctionDdl(connId, objectName, catalog, schema),
        };
      case ExplorerNodeType.PROCEDURE:
        return {
          title: t('explorer.procedure_ddl_title'),
          displayName,
          loadDdl: () => procedureService.getProcedureDdl(connId, objectName, catalog, schema),
        };
      case ExplorerNodeType.TRIGGER:
        return {
          title: t('explorer.trigger_ddl_title'),
          displayName,
          loadDdl: () => triggerService.getTriggerDdl(connId, objectName, catalog, schema),
        };
      default:
        return null;
    }
  };

  const ddlConfig = getDdlConfig();

  const renderNode = ({ node, style, dragHandle }: { node: NodeApi<ExplorerNode>; style: React.CSSProperties; dragHandle?: unknown }) => {
    const isLoading = node.isInternal && node.isOpen && (!node.data.children || node.data.children.length === 0);

    return (
      <ExplorerTreeNode
        node={node}
        style={style}
        dragHandle={dragHandle as React.RefObject<HTMLDivElement>}
        isLoading={isLoading}
        onLoadData={loadNodeData}
        onDisconnect={handleDisconnect}
        onEditConnection={openEditModal}
        onDeleteConnection={(id) => setDeleteConfirmId(id)}
        onViewDdl={handleViewDdl}
        onViewData={handleViewData}
        onDeleteTable={handleDeleteTable}
        onDeleteView={handleDeleteView}
        onDeleteFunction={handleDeleteFunction}
        onDeleteProcedure={handleDeleteProcedure}
        onDeleteTrigger={handleDeleteTrigger}
        onDeleteAllInFolder={handleDeleteAllInFolder}
        onDeleteDatabase={handleDeleteDatabase}
        onExportDatabase={handleExportDatabase}
        onImportDatabase={handleImportDatabase}
      />
    );
  };

  return (
    <div
      className="flex flex-col h-full overflow-hidden"
      {...{ [DataAttributes.EXPLORER_TREE]: true }}
      onContextMenu={(e) => e.preventDefault()}
    >
      <div className="flex items-center justify-between px-3 py-2 theme-text-secondary text-[10px] uppercase font-bold tracking-wider border-b theme-border shrink-0">
        <span>{t('explorer.title')}</span>
        <div className="flex items-center space-x-2">
          <button onClick={() => refetchConnections()} title={t('common.refresh')}>
            <RefreshCw className={cn('w-3 h-3 hover:text-blue-500 transition-colors', isConnectionsLoading && 'animate-spin')} />
          </button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button title={t('common.add')} aria-label={t('common.add')}>
                <Plus className="w-3.5 h-3.5 hover:text-blue-500 cursor-pointer" />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-48">
              <DropdownMenuSub>
                <DropdownMenuSubTrigger>
                  <Database className="w-4 h-4 mr-2 text-blue-400" />
                  <span>{t('explorer.database')}</span>
                </DropdownMenuSubTrigger>
                <DropdownMenuPortal>
                  <DropdownMenuSubContent className="w-48">
                    {supportedDbTypes.length === 0 ? (
                      <DropdownMenuItem disabled className="text-xs theme-text-secondary">
                        {t('explorer.loading')}
                      </DropdownMenuItem>
                    ) : (
                      supportedDbTypes.map((type) => (
                        <DropdownMenuItem key={type.code} onClick={() => openCreateModal(type.code)}>
                          <Database className="w-4 h-4 mr-2 text-blue-400" />
                          <span>{type.displayName}</span>
                        </DropdownMenuItem>
                      ))
                    )}
                  </DropdownMenuSubContent>
                </DropdownMenuPortal>
              </DropdownMenuSub>

              <DropdownMenuSub>
                <DropdownMenuSubTrigger>
                  <Settings className="w-4 h-4 mr-2" />
                  <span>{t('explorer.driver')}</span>
                </DropdownMenuSubTrigger>
                <DropdownMenuPortal>
                  <DropdownMenuSubContent className="w-48">
                    {supportedDbTypes.length === 0 ? (
                      <DropdownMenuItem disabled className="text-xs theme-text-secondary">
                        {t('explorer.loading')}
                      </DropdownMenuItem>
                    ) : (
                      supportedDbTypes.map((type) => (
                        <DropdownMenuItem
                          key={type.code}
                          onClick={() => {
                            setSelectedDriverDbType(type.code);
                            setDriverModalOpen(true);
                          }}
                        >
                          <Database className="w-4 h-4 mr-2 text-blue-400" />
                          <span>{type.displayName}</span>
                        </DropdownMenuItem>
                      ))
                    )}
                  </DropdownMenuSubContent>
                </DropdownMenuPortal>
              </DropdownMenuSub>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      <div className="p-2 shrink-0">
        <div className="relative">
          <Search className="absolute left-2 top-1/2 -translate-y-1/2 w-3 h-3 theme-text-secondary" />
          <input
            type="text"
            placeholder={t('common.search')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-accent/30 border theme-border rounded px-7 py-1 text-[11px] focus:outline-none focus:border-primary/50"
          />
        </div>
      </div>

      <div className="flex-1 overflow-hidden py-1">
        {treeDataState.length === 0 && !isConnectionsLoading ? (
          <div className="p-4 text-center text-xs theme-text-secondary opacity-50">{t('common.no_connections')}</div>
        ) : (
          <ArboristTree
            data={treeDataState}
            openByDefault={false}
            width="100%"
            height={ExplorerTreeConfig.HEIGHT}
            indent={ExplorerTreeConfig.INDENT}
            rowHeight={ExplorerTreeConfig.ROW_HEIGHT}
            searchTerm={searchTerm}
            searchMatch={(node, term) => node.data.name.toLowerCase().includes(term.toLowerCase())}
          >
            {renderNode}
          </ArboristTree>
        )}
      </div>

      <ConnectionFormModal
        open={connectionModalOpen}
        onOpenChange={setConnectionModalOpen}
        mode={connectionModalMode}
        editId={connectionEditId}
        initialDbType={initialDbType}
        onSuccess={() => refetchConnections()}
      />
      <DriverManageModal
        open={driverModalOpen}
        onOpenChange={setDriverModalOpen}
        databaseType={selectedDriverDbType}
        onSelectDriver={() => {}}
      />
      <DeleteConnectionDialog
        open={deleteConfirmId != null}
        onOpenChange={(open) => !open && setDeleteConfirmId(null)}
        connectionId={deleteConfirmId}
        onConfirm={(id) => {
          deleteMutation.mutate(id);
          setDeleteConfirmId(null);
        }}
        isPending={deleteMutation.isPending}
      />

      {ddlConfig && (
        <DdlViewerDialog
          open={ddlDialogOpen}
          onOpenChange={(open) => {
            setDdlDialogOpen(open);
            if (!open) setSelectedDdlNode(null);
          }}
          title={ddlConfig.title}
          displayName={ddlConfig.displayName}
          loadDdl={ddlConfig.loadDdl}
        />
      )}

      {selectedTableDataNode && (
        <TableDataDialog
          open={tableDataDialogOpen}
          onOpenChange={(open) => {
            setTableDataDialogOpen(open);
            if (!open) {
              setSelectedTableDataNode(null);
              setHighlightColumn(undefined);
            }
          }}
          title={selectedTableDataNode.type === ExplorerNodeType.TABLE ? t('explorer.table_data') : t('explorer.view_data_title')}
          displayName={[selectedTableDataNode.catalog, selectedTableDataNode.schema, selectedTableDataNode.tableName || selectedTableDataNode.name].filter(Boolean).join('.')}
          connectionId={selectedTableDataNode.connectionId!}
          objectName={selectedTableDataNode.tableName || selectedTableDataNode.objectName || selectedTableDataNode.name}
          objectType={selectedTableDataNode.type === ExplorerNodeType.TABLE ? 'table' : selectedTableDataNode.type === ExplorerNodeType.VIEW ? 'view' : 'table'}
          catalog={selectedTableDataNode.catalog}
          schema={selectedTableDataNode.schema}
          highlightColumn={highlightColumn}
        />
      )}

      <DeleteTableDialog
        open={deleteTableDialogOpen}
        onOpenChange={(open) => {
          setDeleteTableDialogOpen(open);
          if (!open) setDeleteTableNode(null);
        }}
        tableName={deleteTableNode?.name || ''}
        onConfirm={confirmDeleteTable}
        isPending={isDeletingTable}
      />

      <DeleteTableDialog
        open={deleteViewDialogOpen}
        onOpenChange={(open) => {
          setDeleteViewDialogOpen(open);
          if (!open) setDeleteViewNode(null);
        }}
        tableName={deleteViewNode?.name || ''}
        onConfirm={confirmDeleteView}
        isPending={isDeletingView}
        title={t('explorer.delete_view')}
        confirmMessage={t('explorer.delete_view_confirm', { name: deleteViewNode?.name || '' })}
      />

      <DeleteTableDialog
        open={deleteFunctionDialogOpen}
        onOpenChange={(open) => {
          setDeleteFunctionDialogOpen(open);
          if (!open) setDeleteFunctionNode(null);
        }}
        tableName={deleteFunctionNode?.name || ''}
        onConfirm={confirmDeleteFunction}
        isPending={isDeletingFunction}
        title={t('explorer.delete_function')}
        confirmMessage={t('explorer.delete_function_confirm', { name: deleteFunctionNode?.name || '' })}
      />

      <DeleteTableDialog
        open={deleteProcedureDialogOpen}
        onOpenChange={(open) => {
          setDeleteProcedureDialogOpen(open);
          if (!open) setDeleteProcedureNode(null);
        }}
        tableName={deleteProcedureNode?.name || ''}
        onConfirm={confirmDeleteProcedure}
        isPending={isDeletingProcedure}
        title={t('explorer.delete_procedure')}
        confirmMessage={t('explorer.delete_procedure_confirm', { name: deleteProcedureNode?.name || '' })}
      />

      <DeleteTableDialog
        open={deleteTriggerDialogOpen}
        onOpenChange={(open) => {
          setDeleteTriggerDialogOpen(open);
          if (!open) setDeleteTriggerNode(null);
        }}
        tableName={deleteTriggerNode?.name || ''}
        onConfirm={confirmDeleteTrigger}
        isPending={isDeletingTrigger}
        title={t('explorer.delete_trigger')}
        confirmMessage={t('explorer.delete_trigger_confirm', { name: deleteTriggerNode?.name || '' })}
      />

      <DeleteTableDialog
        open={deleteFolderDialogOpen}
        onOpenChange={(open) => {
          setDeleteFolderDialogOpen(open);
          if (!open) setDeleteFolderNode(null);
        }}
        tableName={deleteFolderNode?.name || ''}
        onConfirm={confirmDeleteFolder}
        isPending={isDeletingFolder}
        title={t('explorer.delete_all_in_folder')}
        confirmMessage={t('explorer.delete_all_in_folder_confirm', {
          name: deleteFolderNode?.name || '',
          count: deleteFolderNode?.children?.filter(c => c.type !== 'empty').length || 0
        })}
      />

      <DeleteTableDialog
        open={deleteDatabaseDialogOpen}
        onOpenChange={(open) => {
          setDeleteDatabaseDialogOpen(open);
          if (!open) setDeleteDatabaseNode(null);
        }}
        tableName={deleteDatabaseNode?.name || ''}
        onConfirm={confirmDeleteDatabase}
        isPending={isDeletingDatabase}
        title={t('explorer.delete_database')}
        confirmMessage={t('explorer.delete_database_confirm', { name: deleteDatabaseNode?.name || '' })}
      />
    </div>
  );
}
