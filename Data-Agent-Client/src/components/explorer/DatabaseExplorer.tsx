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
import { DeleteEntityDialog } from './DeleteEntityDialog';
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
import { getDeleteService } from '../../utils/deleteHandlers';
import { removeNodeById, clearFolderChildren } from '../../utils/treeOperations';
import { DELETE_DIALOG_CONFIG } from '../../constants/deleteConfig';
import { tableService } from '../../services/table.service';
import { viewService } from '../../services/view.service';
import { functionService } from '../../services/function.service';
import { procedureService } from '../../services/procedure.service';
import { triggerService } from '../../services/trigger.service';

export function DatabaseExplorer() {
  const { t } = useTranslation();
  const { supportedDbTypes } = useWorkspaceStore();
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
  const [connectionModalOpen, setConnectionModalOpen] = useState(false);
  const [connectionModalMode, setConnectionModalMode] = useState<ConnectionFormMode>('create');
  const [connectionEditId, setConnectionEditId] = useState<number | undefined>(undefined);
  const [initialDbType, setInitialDbType] = useState<string | undefined>(undefined);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [driverModalOpen, setDriverModalOpen] = useState(false);
  const [selectedDriverDbType, setSelectedDriverDbType] = useState<string>('');

  const [ddlDialogOpen, setDdlDialogOpen] = useState(false);
  const [selectedDdlNode, setSelectedDdlNode] = useState<ExplorerNode | null>(null);

  // Unified delete state for all entity types
  interface DeleteState {
    type: ExplorerNodeType | null;
    node: ExplorerNode | null;
    isOpen: boolean;
    isPending: boolean;
  }

  const [deleteState, setDeleteState] = useState<DeleteState>({
    type: null,
    node: null,
    isOpen: false,
    isPending: false,
  });

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

  // Unified delete handler for all entity types
  const handleDelete = (node: ExplorerNode, type: ExplorerNodeType) => {
    if (!node.connectionId && type !== ExplorerNodeType.FOLDER) return;
    setDeleteState({ type, node, isOpen: true, isPending: false });
  };

  // Helper function to delete all items in a folder
  const handleFolderDelete = async (folderNode: ExplorerNode) => {
    if (!folderNode.connectionId || !folderNode.children) return;

    for (const child of folderNode.children.filter(c => c.type !== 'empty')) {
      const deleteService = getDeleteService(child.type as ExplorerNodeType);
      if (deleteService) {
        try {
          await deleteService({
            connectionId: folderNode.connectionId,
            name: child.name,
            catalog: folderNode.catalog,
            schema: folderNode.schema,
          });
        } catch (err) {
          console.error(`Failed to delete ${child.type} ${child.name}:`, err);
        }
      }
    }

    setTreeDataState(prev => clearFolderChildren(prev, folderNode.id));
  };

  // Unified confirm delete function for all entity types
  const confirmDelete = async () => {
    const { type, node } = deleteState;
    if (!node || !type) return;

    setDeleteState(prev => ({ ...prev, isPending: true }));

    try {
      if (type === ExplorerNodeType.FOLDER) {
        await handleFolderDelete(node);
      } else {
        const deleteService = getDeleteService(type);
        if (!deleteService) throw new Error(`No delete service for ${type}`);

        await deleteService({
          connectionId: node.connectionId!,
          name: node.name,
          catalog: node.catalog,
          schema: node.schema,
        });

        setTreeDataState(prev => removeNodeById(prev, node.id));
      }
    } catch (error) {
      console.error(`Failed to delete ${type}:`, error);
      const config = DELETE_DIALOG_CONFIG[type];
      if (config) alert(t(config.errorKey));
    } finally {
      setDeleteState({ type: null, node: null, isOpen: false, isPending: false });
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
        onDelete={handleDelete}
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
          connectionId={Number(selectedTableDataNode.connectionId!)}
          objectName={selectedTableDataNode.tableName || selectedTableDataNode.objectName || selectedTableDataNode.name}
          objectType={selectedTableDataNode.type === ExplorerNodeType.TABLE ? 'table' : selectedTableDataNode.type === ExplorerNodeType.VIEW ? 'view' : 'table'}
          catalog={selectedTableDataNode.catalog}
          schema={selectedTableDataNode.schema}
          highlightColumn={highlightColumn}
        />
      )}

      {deleteState.type && deleteState.node && (
        <DeleteEntityDialog
          open={deleteState.isOpen}
          onOpenChange={(open) => {
            setDeleteState(prev => ({ ...prev, isOpen: open }));
            if (!open) {
              setDeleteState({ type: null, node: null, isOpen: false, isPending: false });
            }
          }}
          entityName={deleteState.node.name}
          entityType={deleteState.type}
          onConfirm={confirmDelete}
          isPending={deleteState.isPending}
          itemCount={
            deleteState.type === ExplorerNodeType.FOLDER
              ? deleteState.node.children?.filter(c => c.type !== 'empty').length
              : undefined
          }
        />
      )}
    </div>
  );
}
