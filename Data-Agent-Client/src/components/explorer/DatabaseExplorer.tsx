import * as React from 'react';
import { useState } from 'react';
import { Database, Folder, Table, ChevronRight, ChevronDown, Plus, Search, RefreshCw, MoreVertical, Pencil, Trash2, Plug, Settings, FileText } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { useTranslation } from 'react-i18next';
import { Tree as ArboristTree, NodeApi } from 'react-arborist';
import { ConnectionFormModal, type ConnectionFormMode } from '../common/ConnectionFormModal';
import { DriverManageModal } from '../common/DriverManageModal';
import { DeleteConnectionDialog } from './DeleteConnectionDialog';
import { TableDdlDialog } from './TableDdlDialog';
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
import { Button } from '../ui/Button';
import { useConnectionTree, type ExplorerNode } from '../../hooks/useConnectionTree';

export function DatabaseExplorer() {
  const { t } = useTranslation();
  const { supportedDbTypes } = useWorkspaceStore();
  const {
    treeDataState,
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
  const [selectedTableNode, setSelectedTableNode] = useState<ExplorerNode | null>(null);
  
  const [contextMenuOpen, setContextMenuOpen] = useState(false);
  const [contextMenuNode, setContextMenuNode] = useState<NodeApi<ExplorerNode> | null>(null);

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
    setSelectedTableNode(node);
    setDdlDialogOpen(true);
    setContextMenuOpen(false);
  };

  const renderNode = ({ node, style, dragHandle }: { node: NodeApi<ExplorerNode>; style: React.CSSProperties; dragHandle?: unknown }) => {
    const isConnected = !!node.data.connectionId;
    const isLoading = node.isInternal && node.isOpen && (!node.data.children || node.data.children.length === 0);

    return (
      <div
        style={style}
        ref={dragHandle as React.RefObject<HTMLDivElement>}
        className={cn(
          'flex items-center py-1 px-2 hover:bg-accent/50 cursor-pointer group select-none text-xs',
          node.isSelected && 'bg-accent/30'
        )}
        onClick={() => node.select()}
        onDoubleClick={() => {
          if (node.isInternal) {
            if (!node.isOpen && (!node.data.children || node.data.children.length === 0)) {
              loadNodeData(node);
            }
            node.toggle();
          }
        }}
        onContextMenu={(e) => {
          e.preventDefault();
          node.select();
          // Show context menu for table nodes
          if (node.data.type === 'table') {
            setContextMenuNode(node);
            setContextMenuOpen(true);
          }
        }}
      >
        <span 
          className="w-4 flex items-center justify-center mr-1 hover:bg-accent rounded-sm"
          onClick={(e) => {
            e.stopPropagation();
            if (node.isInternal) {
              if (!node.isOpen && (!node.data.children || node.data.children.length === 0)) {
                loadNodeData(node);
              }
              node.toggle();
            }
          }}
        >
          {node.isInternal && (node.isOpen ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />)}
        </span>
        <span className="mr-2">{renderIcon(node.data.type, node.isOpen)}</span>
        <span className="flex-1 truncate theme-text-primary">{node.data.name}</span>

        {node.data.type === 'root' && (
          <div className="opacity-0 group-hover:opacity-100 flex items-center space-x-1" onClick={(e) => e.stopPropagation()}>
            {isLoading ? (
              <RefreshCw className="w-3.5 h-3.5 animate-spin theme-text-secondary" />
            ) : (
              <>
                <Button 
                    variant="ghost" 
                    size="icon" 
                    className="h-6 w-6"
                    onClick={(e) => {
                        e.stopPropagation();
                        loadNodeData(node);
                    }}
                    title={t('common.refresh')}
                >
                    <RefreshCw className="w-3 h-3 theme-text-secondary" />
                </Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-6 w-6">
                      <MoreVertical className="w-3 h-3 theme-text-secondary" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {isConnected && (
                      <DropdownMenuItem onClick={() => handleDisconnect(node)}>
                        <Plug className="w-3.5 h-3.5 mr-2" />
                        {t('explorer.disconnect')}
                      </DropdownMenuItem>
                    )}
                    <DropdownMenuItem onClick={() => openEditModal(Number(node.id.replace('conn-', '')))}>
                      <Pencil className="w-3.5 h-3.5 mr-2" />
                      {t('explorer.edit')}
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      onClick={() => setDeleteConfirmId(Number(node.id.replace('conn-', '')))}
                      className="text-destructive focus:text-destructive"
                    >
                      <Trash2 className="w-3.5 h-3.5 mr-2" />
                      {t('explorer.delete_connection')}
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </>
            )}
          </div>
        )}

        {node.data.type === 'table' && (
          <div className="opacity-0 group-hover:opacity-100 flex items-center" onClick={(e) => e.stopPropagation()}>
            <DropdownMenu open={contextMenuOpen && contextMenuNode?.id === node.id} onOpenChange={(open) => {
              setContextMenuOpen(open);
              if (!open) setContextMenuNode(null);
            }}>
              <DropdownMenuTrigger asChild>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  className="h-6 w-6"
                  onClick={(e) => {
                    e.stopPropagation();
                    setContextMenuNode(node);
                    setContextMenuOpen(true);
                  }}
                >
                  <MoreVertical className="w-3 h-3 theme-text-secondary" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => handleViewDdl(node.data)}>
                  <FileText className="w-3.5 h-3.5 mr-2" />
                  {t('explorer.view_ddl')}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="flex flex-col h-full overflow-hidden">
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
            height={800}
            indent={12}
            rowHeight={28}
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
      
      {selectedTableNode && (
        <TableDdlDialog
          open={ddlDialogOpen}
          onOpenChange={setDdlDialogOpen}
          connectionId={selectedTableNode.connectionId || ''}
          tableName={selectedTableNode.name}
          catalog={selectedTableNode.catalog}
          schema={selectedTableNode.schema}
        />
      )}
    </div>
  );
}

function renderIcon(type: string, expanded?: boolean) {
  switch (type) {
    case 'root':
      return <Database className="w-3.5 h-3.5 text-blue-400" />;
    case 'db':
      return <Database className="w-3.5 h-3.5 text-teal-400" />;
    case 'folder':
    case 'schema':
      return expanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />;
    case 'table':
      return <Table className="w-3.5 h-3.5 text-green-400" />;
    default:
      return <Folder className="w-3.5 h-3.5 text-gray-400" />;
  }
}
