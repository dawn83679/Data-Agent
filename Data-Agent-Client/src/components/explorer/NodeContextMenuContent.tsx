import { FileText, Table, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import {
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuSeparator,
} from '../ui/ContextMenu';
import { ExplorerNodeType } from '../../constants/explorer';
import type { ExplorerNode } from '../../types/explorer';

interface NodeContextMenuContentProps {
  node: ExplorerNode;
  isConnected: boolean;
  onOpenQueryConsole: (node: ExplorerNode) => void;
  onViewDdl: (node: ExplorerNode) => void;
  onViewData: (node: ExplorerNode, highlightColumn?: string) => void;
  onDelete: (node: ExplorerNode, type: ExplorerNodeType) => void;
}

export function NodeContextMenuContent({
  node,
  isConnected,
  onOpenQueryConsole,
  onViewDdl,
  onViewData,
  onDelete,
}: NodeContextMenuContentProps) {
  const { t } = useTranslation();

  // Determine node characteristics
  const isDdlNode =
    node.type === ExplorerNodeType.TABLE ||
    node.type === ExplorerNodeType.VIEW ||
    node.type === ExplorerNodeType.FUNCTION ||
    node.type === ExplorerNodeType.PROCEDURE ||
    node.type === ExplorerNodeType.TRIGGER;

  const isTableOrView = node.type === ExplorerNodeType.TABLE || node.type === ExplorerNodeType.VIEW;

  const isColumnOrIndexOrKey =
    node.type === ExplorerNodeType.COLUMN ||
    node.type === ExplorerNodeType.INDEX ||
    node.type === ExplorerNodeType.KEY;

  const isDb = node.type === ExplorerNodeType.DB;

  const isDeletableFolder =
    node.type === ExplorerNodeType.FOLDER &&
    node.folderName &&
    ['tables', 'views', 'routines', 'triggers'].includes(node.folderName);

  // Extract column name from key/index name (format: "name (col1, col2)" or just "name")
  const extractColumnName = (nodeName: string): string | undefined => {
    const match = nodeName.match(/\(([^)]+)\)/);
    if (match) {
      return match[1].split(',')[0].trim();
    }
    return undefined;
  };

  // Track if any data operation item was shown for separator logic
  let hasDataOperations = false;
  let hasDeleteOperation = false;

  return (
    <ContextMenuContent>
      {/* 1. Open Query Console - available for all connected nodes */}
      {isConnected && node.type !== ExplorerNodeType.EMPTY && (
        <>
          <ContextMenuItem onSelect={() => onOpenQueryConsole(node)}>
            <FileText className="w-3.5 h-3.5 mr-2" />
            {t('explorer.open_query_console')}
          </ContextMenuItem>
          <ContextMenuSeparator />
        </>
      )}

      {/* 2. View DDL - for DDL objects */}
      {isDdlNode && (
        <>
          <ContextMenuItem onSelect={() => onViewDdl(node)}>
            <FileText className="w-3.5 h-3.5 mr-2" />
            {t('explorer.view_ddl')}
          </ContextMenuItem>
          {hasDataOperations && <ContextMenuSeparator />}
        </>
      )}

      {/* 3. View Data - for tables/views and their children */}
      {(isTableOrView || isColumnOrIndexOrKey) && (
        <>
          <ContextMenuItem
            onSelect={() => {
              const highlightCol =
                node.type === ExplorerNodeType.COLUMN
                  ? node.name
                  : extractColumnName(node.name);
              onViewData(node, highlightCol);
            }}
          >
            <Table className="w-3.5 h-3.5 mr-2" />
            {t('explorer.view_data')}
          </ContextMenuItem>
          {hasDeleteOperation && <ContextMenuSeparator />}
        </>
      )}
      {isTableOrView || isColumnOrIndexOrKey ? (hasDataOperations = true) : null}

      {/* 4. Delete Operations */}
      {node.type === ExplorerNodeType.TABLE && (
        <ContextMenuItem
          onSelect={() => onDelete(node, ExplorerNodeType.TABLE)}
          className="text-destructive focus:text-destructive"
        >
          <Trash2 className="w-3.5 h-3.5 mr-2" />
          {t('explorer.delete_table')}
        </ContextMenuItem>
      )}

      {node.type === ExplorerNodeType.VIEW && (
        <ContextMenuItem
          onSelect={() => onDelete(node, ExplorerNodeType.VIEW)}
          className="text-destructive focus:text-destructive"
        >
          <Trash2 className="w-3.5 h-3.5 mr-2" />
          {t('explorer.delete_view')}
        </ContextMenuItem>
      )}

      {node.type === ExplorerNodeType.FUNCTION && (
        <ContextMenuItem
          onSelect={() => onDelete(node, ExplorerNodeType.FUNCTION)}
          className="text-destructive focus:text-destructive"
        >
          <Trash2 className="w-3.5 h-3.5 mr-2" />
          {t('explorer.delete_function')}
        </ContextMenuItem>
      )}

      {node.type === ExplorerNodeType.PROCEDURE && (
        <ContextMenuItem
          onSelect={() => onDelete(node, ExplorerNodeType.PROCEDURE)}
          className="text-destructive focus:text-destructive"
        >
          <Trash2 className="w-3.5 h-3.5 mr-2" />
          {t('explorer.delete_procedure')}
        </ContextMenuItem>
      )}

      {node.type === ExplorerNodeType.TRIGGER && (
        <ContextMenuItem
          onSelect={() => onDelete(node, ExplorerNodeType.TRIGGER)}
          className="text-destructive focus:text-destructive"
        >
          <Trash2 className="w-3.5 h-3.5 mr-2" />
          {t('explorer.delete_trigger')}
        </ContextMenuItem>
      )}

      {isDb && (
        <ContextMenuItem
          onSelect={() => onDelete(node, ExplorerNodeType.DB)}
          className="text-destructive focus:text-destructive"
        >
          <Trash2 className="w-3.5 h-3.5 mr-2" />
          {t('explorer.delete_database')}
        </ContextMenuItem>
      )}

      {isDeletableFolder && node.children && node.children.filter((c) => c.type !== 'empty').length > 0 && (
        <ContextMenuItem
          onSelect={() => onDelete(node, ExplorerNodeType.FOLDER)}
          className="text-destructive focus:text-destructive"
        >
          <Trash2 className="w-3.5 h-3.5 mr-2" />
          {t('explorer.delete_all_in_folder')}
        </ContextMenuItem>
      )}
    </ContextMenuContent>
  );
}
