import * as React from 'react';
import { ChevronRight, ChevronDown, RefreshCw, MoreVertical, Pencil, Trash2, Plug, FileText, Table, Download, Upload } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useTranslation } from 'react-i18next';
import { NodeApi } from 'react-arborist';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuTrigger,
} from '../ui/ContextMenu';
import { Button } from '../ui/Button';
import { ExplorerNodeIcon } from './ExplorerNodeIcon';
import { ExplorerNodeType, ExplorerIdPrefix } from '../../constants/explorer';
import type { ExplorerNode } from '../../types/explorer';

export interface ExplorerTreeNodeProps {
  node: NodeApi<ExplorerNode>;
  style: React.CSSProperties;
  dragHandle?: React.RefObject<HTMLDivElement>;
  isLoading: boolean;
  onLoadData: (node: NodeApi<ExplorerNode>) => void;
  onDisconnect: (node: NodeApi<ExplorerNode>) => void;
  onEditConnection: (connId: number) => void;
  onDeleteConnection: (connId: number) => void;
  onViewDdl: (node: ExplorerNode) => void;
  onViewData: (node: ExplorerNode, highlightColumn?: string) => void;
  onDeleteTable: (node: ExplorerNode) => void;
  onDeleteView: (node: ExplorerNode) => void;
  onDeleteFunction: (node: ExplorerNode) => void;
  onDeleteProcedure: (node: ExplorerNode) => void;
  onDeleteTrigger: (node: ExplorerNode) => void;
  onDeleteAllInFolder: (node: ExplorerNode) => void;
  onDeleteDatabase: (node: ExplorerNode) => void;
  onExportDatabase: (node: ExplorerNode) => void;
  onImportDatabase: (node: ExplorerNode) => void;
}

export function ExplorerTreeNode({
  node,
  style,
  dragHandle,
  isLoading,
  onLoadData,
  onDisconnect,
  onEditConnection,
  onDeleteConnection,
  onViewDdl,
  onViewData,
  onDeleteTable,
  onDeleteView,
  onDeleteFunction,
  onDeleteProcedure,
  onDeleteTrigger,
  onDeleteAllInFolder,
  onDeleteDatabase,
  onExportDatabase,
  onImportDatabase,
}: ExplorerTreeNodeProps) {
  const { t } = useTranslation();
  const isConnected = !!node.data.connectionId;
  const isDdlNode =
    node.data.type === ExplorerNodeType.TABLE ||
    node.data.type === ExplorerNodeType.VIEW ||
    node.data.type === ExplorerNodeType.FUNCTION ||
    node.data.type === ExplorerNodeType.PROCEDURE ||
    node.data.type === ExplorerNodeType.TRIGGER;
  const isTableOrView = node.data.type === ExplorerNodeType.TABLE || node.data.type === ExplorerNodeType.VIEW;
  const isColumnOrIndexOrKey = node.data.type === ExplorerNodeType.COLUMN || node.data.type === ExplorerNodeType.INDEX || node.data.type === ExplorerNodeType.KEY;
  const isRoutineOrTrigger = node.data.type === ExplorerNodeType.FUNCTION || node.data.type === ExplorerNodeType.PROCEDURE || node.data.type === ExplorerNodeType.TRIGGER;
  const isRoutine =
    node.data.type === ExplorerNodeType.FUNCTION || node.data.type === ExplorerNodeType.PROCEDURE;
  const isFolder = node.data.type === ExplorerNodeType.FOLDER;
  const isDb = node.data.type === ExplorerNodeType.DB;
  // 文件夹中只有包含可删除对象的文件夹才显示删除按钮（Tables、Views、Routines、Triggers）
  // Columns、Keys、Indexes 是结构文件夹，不显示删除按钮
  const isDeletableFolder = isFolder && node.data.folderName &&
    ['tables', 'views', 'routines', 'triggers'].includes(node.data.folderName);
  const folderCount =
    isFolder &&
    node.data.children &&
    node.data.children.length > 0
      ? node.data.children.filter((c) => c.type !== ExplorerNodeType.EMPTY).length
      : null;

  const handleToggle = (e?: React.MouseEvent) => {
    e?.stopPropagation();
    if (node.isInternal) {
      if (!node.isOpen && (!node.data.children || node.data.children.length === 0)) {
        onLoadData(node);
      }
      node.toggle();
    }
  };

  // Extract column name from key/index name (format: "name (col1, col2)" or just "name")
  const extractColumnName = (nodeName: string): string | undefined => {
    const match = nodeName.match(/\(([^)]+)\)/);
    if (match) {
      // If multiple columns, return the first one
      return match[1].split(',')[0].trim();
    }
    return undefined;
  };

  // Only handle double-click for showing data, not single click on arrow
  const handleDoubleClick = (e?: React.MouseEvent) => {
    e?.stopPropagation();
    // For tables/views - show data
    if (isTableOrView) {
      onViewData(node.data);
    } else if (isColumnOrIndexOrKey) {
      // For columns/indexes/keys - show data with highlighted column
      const highlightCol = node.data.type === ExplorerNodeType.COLUMN
        ? node.data.name
        : extractColumnName(node.data.name);
      onViewData(node.data, highlightCol);
    } else if (isRoutineOrTrigger) {
      // For functions, procedures, triggers - show DDL
      onViewDdl(node.data);
    } else if (node.isInternal) {
      // For other nodes (folders), double-click toggles expansion
      if (!node.isOpen && (!node.data.children || node.data.children.length === 0)) {
        onLoadData(node);
      }
      node.toggle();
    }
  };

  const handleContextMenu = () => {
    node.select();
  };

  const handleDdlClick = () => {
    onViewDdl(node.data);
  };

  const connId = node.id.replace(ExplorerIdPrefix.CONNECTION, '');

  const rowContent = (
    <>
      <span
        className="w-4 flex items-center justify-center mr-1 hover:bg-accent rounded-sm"
        onClick={handleToggle}
      >
        {node.isInternal && (node.isOpen ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />)}
      </span>
      <span className="mr-2">
        <ExplorerNodeIcon node={node.data} />
      </span>
      <span
        className={cn(
          'flex-1 min-w-0 truncate flex items-baseline gap-1.5',
          node.data.type === ExplorerNodeType.EMPTY ? 'theme-text-secondary opacity-60 italic' : ''
        )}
      >
        <span
          className={cn(
            'truncate',
            node.data.type === ExplorerNodeType.EMPTY ? '' : 'theme-text-primary'
          )}
        >
          {node.data.name}
          {isFolder && folderCount != null && (
            <span className="theme-text-secondary opacity-50 ml-0.5">{folderCount}</span>
          )}
        </span>
        {isRoutine && node.data.signature && (
          <span className="theme-text-secondary text-[10px] shrink-0">{node.data.signature}</span>
        )}
        {node.data.type === ExplorerNodeType.COLUMN && node.data.signature && (
          <span className="theme-text-secondary text-[10px] shrink-0">{node.data.signature}</span>
        )}
      </span>

      {node.data.type === ExplorerNodeType.ROOT && (
        <RootNodeActions
          isLoading={isLoading}
          isConnected={isConnected}
          onRefresh={() => onLoadData(node)}
          onDisconnect={() => onDisconnect(node)}
          onEdit={() => onEditConnection(Number(connId))}
          onDelete={() => onDeleteConnection(Number(connId))}
          t={t}
        />
      )}
    </>
  );

  const rowClassName = cn(
    'flex items-center py-1 px-2 hover:bg-accent/50 cursor-pointer group select-none text-xs',
    node.isSelected && 'bg-accent/30'
  );

  // 判断是否需要显示右键菜单
  const showContextMenu = isDdlNode || isDb || isDeletableFolder || isColumnOrIndexOrKey;

  if (showContextMenu) {
    return (
      <ContextMenu>
        <ContextMenuTrigger asChild>
          <div
            style={style}
            ref={dragHandle}
            className={rowClassName}
            onClick={() => node.select()}
            onDoubleClick={handleDoubleClick}
            onContextMenu={handleContextMenu}
          >
            {rowContent}
          </div>
        </ContextMenuTrigger>
        <ContextMenuContent>
          {isDdlNode && (
            <ContextMenuItem onSelect={handleDdlClick}>
              <FileText className="w-3.5 h-3.5 mr-2" />
              {t('explorer.view_ddl')}
            </ContextMenuItem>
          )}
          {(isTableOrView || isColumnOrIndexOrKey) && (
            <ContextMenuItem onSelect={() => {
              const highlightCol = node.data.type === ExplorerNodeType.COLUMN
                ? node.data.name
                : extractColumnName(node.data.name);
              onViewData(node.data, highlightCol);
            }}>
              <Table className="w-3.5 h-3.5 mr-2" />
              {t('explorer.view_data')}
            </ContextMenuItem>
          )}
          {node.data.type === ExplorerNodeType.TABLE && (
            <ContextMenuItem onSelect={() => onDeleteTable(node.data)} className="text-destructive focus:text-destructive">
              <Trash2 className="w-3.5 h-3.5 mr-2" />
              {t('explorer.delete_table')}
            </ContextMenuItem>
          )}
          {node.data.type === ExplorerNodeType.VIEW && (
            <ContextMenuItem onSelect={() => onDeleteView(node.data)} className="text-destructive focus:text-destructive">
              <Trash2 className="w-3.5 h-3.5 mr-2" />
              {t('explorer.delete_view')}
            </ContextMenuItem>
          )}
          {node.data.type === ExplorerNodeType.FUNCTION && (
            <ContextMenuItem onSelect={() => onDeleteFunction(node.data)} className="text-destructive focus:text-destructive">
              <Trash2 className="w-3.5 h-3.5 mr-2" />
              {t('explorer.delete_function')}
            </ContextMenuItem>
          )}
          {node.data.type === ExplorerNodeType.PROCEDURE && (
            <ContextMenuItem onSelect={() => onDeleteProcedure(node.data)} className="text-destructive focus:text-destructive">
              <Trash2 className="w-3.5 h-3.5 mr-2" />
              {t('explorer.delete_procedure')}
            </ContextMenuItem>
          )}
          {node.data.type === ExplorerNodeType.TRIGGER && (
            <ContextMenuItem onSelect={() => onDeleteTrigger(node.data)} className="text-destructive focus:text-destructive">
              <Trash2 className="w-3.5 h-3.5 mr-2" />
              {t('explorer.delete_trigger')}
            </ContextMenuItem>
          )}
          {isDb && (
            <>
              <ContextMenuItem onSelect={() => onExportDatabase(node.data)}>
                <Upload className="w-3.5 h-3.5 mr-2" />
                {t('explorer.export_database')}
              </ContextMenuItem>
              <ContextMenuItem onSelect={() => onImportDatabase(node.data)}>
                <Download className="w-3.5 h-3.5 mr-2" />
                {t('explorer.import_database')}
              </ContextMenuItem>
              <ContextMenuItem onSelect={() => onDeleteDatabase(node.data)} className="text-destructive focus:text-destructive">
                <Trash2 className="w-3.5 h-3.5 mr-2" />
                {t('explorer.delete_database')}
              </ContextMenuItem>
            </>
          )}
          {isDeletableFolder && folderCount != null && folderCount > 0 && (
            <ContextMenuItem onSelect={() => onDeleteAllInFolder(node.data)} className="text-destructive focus:text-destructive">
              <Trash2 className="w-3.5 h-3.5 mr-2" />
              {t('explorer.delete_all_in_folder')}
            </ContextMenuItem>
          )}
        </ContextMenuContent>
      </ContextMenu>
    );
  }

  return (
    <div
      style={style}
      ref={dragHandle}
      className={rowClassName}
      onClick={() => node.select()}
      onDoubleClick={handleDoubleClick}
      onContextMenu={handleContextMenu}
    >
      {rowContent}
    </div>
  );
}

interface RootNodeActionsProps {
  isLoading: boolean;
  isConnected: boolean;
  onRefresh: () => void;
  onDisconnect: () => void;
  onEdit: () => void;
  onDelete: () => void;
  t: (key: string) => string;
}

function RootNodeActions({
  isLoading,
  isConnected,
  onRefresh,
  onDisconnect,
  onEdit,
  onDelete,
  t,
}: RootNodeActionsProps) {
  return (
    <div className="opacity-0 group-hover:opacity-100 flex items-center space-x-1" onClick={(e) => e.stopPropagation()}>
      {isLoading ? (
        <RefreshCw className="w-3.5 h-3.5 animate-spin theme-text-secondary" />
      ) : (
        <>
          <Button variant="ghost" size="icon" className="h-6 w-6" onClick={onRefresh} title={t('common.refresh')}>
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
                <DropdownMenuItem onClick={onDisconnect}>
                  <Plug className="w-3.5 h-3.5 mr-2" />
                  {t('explorer.disconnect')}
                </DropdownMenuItem>
              )}
              <DropdownMenuItem onClick={onEdit}>
                <Pencil className="w-3.5 h-3.5 mr-2" />
                {t('explorer.edit')}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={onDelete} className="text-destructive focus:text-destructive">
                <Trash2 className="w-3.5 h-3.5 mr-2" />
                {t('explorer.delete_connection')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </>
      )}
    </div>
  );
}

