import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { ExplorerNodeType } from '../constants/explorer';
import { DELETE_DIALOG_CONFIG } from '../constants/deleteConfig';
import { getDeleteService } from '../utils/deleteHandlers';
import { removeNodeById, clearFolderChildren } from '../utils/treeOperations';
import type { ExplorerNode } from '../types/explorer';

interface DeleteActionsProps {
  setDeleteState: (state: any) => void;
  deleteState: any;
  setTreeDataState: (cb: (prev: ExplorerNode[]) => ExplorerNode[]) => void;
}

export function useDeleteActions({
  setDeleteState,
  deleteState,
  setTreeDataState,
}: DeleteActionsProps) {
  const { t } = useTranslation();

  const handleDelete = useCallback((node: ExplorerNode, type: ExplorerNodeType) => {
    if (!node.connectionId && type !== ExplorerNodeType.FOLDER) return;
    setDeleteState({ type, node, isOpen: true, isPending: false });
  }, [setDeleteState]);

  const handleFolderDelete = useCallback(async (folderNode: ExplorerNode) => {
    if (!folderNode.connectionId || !folderNode.children) return;

    for (const child of folderNode.children.filter((c: any) => c.type !== 'empty')) {
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

    setTreeDataState((prev: ExplorerNode[]) => clearFolderChildren(prev, folderNode.id));
  }, [setTreeDataState]);

  const confirmDelete = useCallback(async () => {
    const { type, node } = deleteState;
    if (!node || !type) return;

    setDeleteState((prev: any) => ({ ...prev, isPending: true }));

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

        setTreeDataState((prev: ExplorerNode[]) => removeNodeById(prev, node.id));
      }
    } catch (error) {
      console.error(`Failed to delete ${type}:`, error);
      const config = DELETE_DIALOG_CONFIG[type as ExplorerNodeType];
      if (config) alert(t(config.errorKey));
    } finally {
      setDeleteState({ type: null, node: null, isOpen: false, isPending: false });
    }
  }, [deleteState, handleFolderDelete, setTreeDataState, setDeleteState, t]);

  return { handleDelete, confirmDelete };
}
