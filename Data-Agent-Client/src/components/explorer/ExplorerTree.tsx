import { useTranslation } from 'react-i18next';
import { Tree as ArboristTree, NodeApi } from 'react-arborist';
import { ExplorerTreeConfig, ExplorerNodeType } from '../../constants/explorer';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { ExplorerTreeNode } from './ExplorerTreeNode';
import type { ExplorerNode } from '../../types/explorer';

interface ExplorerTreeProps {
  data: ExplorerNode[];
  searchTerm: string;
  isLoading: boolean;
  onLoadData: (node: NodeApi<ExplorerNode>) => void;
  onDisconnect: (node: NodeApi<ExplorerNode>) => void;
  onEditConnection: (connId: number) => void;
  onDeleteConnection: (connId: number) => void;
  onViewDdl: (node: ExplorerNode) => void;
  onViewData: (node: ExplorerNode, highlightColumn?: string) => void;
  onDelete: (node: ExplorerNode, type: ExplorerNodeType) => void;
  onOpenQueryConsole: (node: ExplorerNode) => void;
}

export function ExplorerTree({
  data,
  searchTerm,
  isLoading,
  onLoadData,
  onDisconnect,
  onEditConnection,
  onDeleteConnection,
  onViewDdl,
  onViewData,
  onDelete,
  onOpenQueryConsole,
}: ExplorerTreeProps) {
  const { t } = useTranslation();

  const renderNode = ({ node, style, dragHandle }: { node: NodeApi<ExplorerNode>; style: React.CSSProperties; dragHandle?: unknown }) => {
    const isLoadingState = node.isInternal && node.isOpen && (!node.data.children || node.data.children.length === 0);

    return (
      <ExplorerTreeNode
        node={node}
        style={style}
        dragHandle={dragHandle as React.RefObject<HTMLDivElement>}
        isLoading={isLoadingState}
        onLoadData={onLoadData}
        onDisconnect={onDisconnect}
        onEditConnection={onEditConnection}
        onDeleteConnection={onDeleteConnection}
        onViewDdl={onViewDdl}
        onViewData={onViewData}
        onDelete={onDelete}
        onOpenQueryConsole={onOpenQueryConsole}
      />
    );
  };

  return (
    <div className="flex-1 overflow-hidden py-1">
      {data.length === 0 && !isLoading ? (
        <div className="p-4 text-center text-xs theme-text-secondary opacity-50">{t(I18N_KEYS.COMMON.NO_CONNECTIONS)}</div>
      ) : (
        <ArboristTree
          data={data}
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
  );
}
