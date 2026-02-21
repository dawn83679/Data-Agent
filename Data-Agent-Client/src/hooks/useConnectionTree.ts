import { useState, useMemo, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { NodeApi } from 'react-arborist';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { connectionService } from '../services/connection.service';
import { databaseService } from '../services/database.service';
import { schemaService } from '../services/schema.service';
import { useWorkspaceStore } from '../store/workspaceStore';
import { useToast } from './useToast';
import { ExplorerNodeType, FolderName, ExplorerIdPrefix, QUERY_KEY_CONNECTIONS } from '../constants/explorer';
import type { ExplorerNode } from '../types/explorer';
import {
  createFolderNode,
  loadDbSchemaFolders,
  loadFolderContents,
  toChildrenOrEmpty,
} from './connectionTreeLoader';

export type { ExplorerNode };
export { ExplorerNodeType, FolderName };

export function useConnectionTree() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { supportedDbTypes } = useWorkspaceStore();

  const { data, isLoading: isConnectionsLoading, refetch: refetchConnections } = useQuery({
    queryKey: QUERY_KEY_CONNECTIONS,
    queryFn: () => connectionService.getConnections(),
  });

  const connections = useMemo(() => data || [], [data]);

  const treeData = useMemo<ExplorerNode[]>(() => {
    return connections.map((conn) => ({
      id: `${ExplorerIdPrefix.CONNECTION}${conn.id}`,
      name: conn.name,
      type: ExplorerNodeType.ROOT,
      dbConnection: conn,
      children: [],
    }));
  }, [connections]);

  const [treeDataState, setTreeDataState] = useState<ExplorerNode[]>([]);

  useEffect(() => {
    setTreeDataState((prev) => {
      if (prev.length === 0) return treeData;
      return treeData.map((newNode) => {
        const existingNode = prev.find((p) => p.id === newNode.id);
        if (existingNode) {
          return {
            ...newNode,
            connectionId: existingNode.connectionId,
            children: existingNode.children,
          };
        }
        return newNode;
      });
    });
  }, [treeData]);

  const deleteMutation = useMutation({
    mutationFn: (id: number) => connectionService.deleteConnection(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY_CONNECTIONS });
    },
  });

  const disconnectMutation = useMutation({
    mutationFn: (connectionId: number) => connectionService.closeConnection(connectionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY_CONNECTIONS });
    },
  });

  const updateNodeChildren = (nodeId: string, children: ExplorerNode[], connectionId?: string) => {
    setTreeDataState((prev) => {
      const update = (list: ExplorerNode[]): ExplorerNode[] =>
        list.map((n) => {
          if (n.id === nodeId) {
            return { ...n, children, ...(connectionId ? { connectionId } : {}) };
          }
          if (n.children) return { ...n, children: update(n.children) };
          return n;
        });
      return update(prev);
    });
  };

  const loadNodeData = async (node: NodeApi<ExplorerNode>) => {
    if (!node.data.connectionId && node.data.type !== ExplorerNodeType.ROOT) return;

    const connId =
      node.data.connectionId ||
      (node.data.dbConnection ? String(node.data.dbConnection.id) : undefined);
    if (!connId) return;

    try {
      if (node.data.type === ExplorerNodeType.ROOT && node.data.dbConnection) {
        const dbNames = await databaseService.listDatabases(String(node.data.dbConnection.id));
        const childrenNodes: ExplorerNode[] = dbNames.map((name) => ({
          id: `${node.id}${ExplorerIdPrefix.DB}${name}`,
          name,
          type: ExplorerNodeType.DB,
          connectionId: String(node.data.dbConnection!.id),
          children: [],
        }));
        updateNodeChildren(node.id, childrenNodes, String(node.data.dbConnection.id));
        return;
      }

      if (node.data.type === ExplorerNodeType.DB) {
        const dbName = node.data.name;
        let rootNode = node;
        while (rootNode.parent && rootNode.level > 0) rootNode = rootNode.parent;
        const typeOption = supportedDbTypes.find((opt) => opt.code === rootNode.data.dbConnection?.dbType);
        const supportSchema = typeOption?.supportSchema ?? false;

        if (supportSchema) {
          const schemas = await schemaService.listSchemas(connId, dbName);
          const childrenNodes: ExplorerNode[] = schemas.map((schemaName) => ({
            id: `${node.id}${ExplorerIdPrefix.SCHEMA}${schemaName}`,
            name: schemaName,
            type: ExplorerNodeType.SCHEMA,
            connectionId: connId,
            catalog: dbName,
            schema: schemaName,
            children: [],
          }));
          updateNodeChildren(node.id, childrenNodes);
        } else {
          const folders = await loadDbSchemaFolders({
            connId,
            parentId: node.id,
            catalog: dbName,
            schema: undefined,
            t,
          });
          updateNodeChildren(node.id, folders);
        }
        return;
      }

      if (node.data.type === ExplorerNodeType.SCHEMA) {
        const dbNode = node.parent;
        const dbName = dbNode?.data.name ?? node.data.catalog ?? '';
        const schemaName = node.data.name;
        const folders = await loadDbSchemaFolders({
          connId,
          parentId: node.id,
          catalog: dbName,
          schema: schemaName,
          t,
        });
        updateNodeChildren(node.id, folders);
        return;
      }

      if (node.data.type === ExplorerNodeType.FOLDER && node.data.folderName) {
        const catalog = node.data.catalog ?? '';
        const schema = node.data.schema;
        const folderName = node.data.folderName;
        const children = await loadFolderContents(
          {
            connId,
            parentId: node.id,
            catalog,
            schema,
            folderId: node.id,
            tableName: node.data.tableName,
            t,
          },
          folderName
        );
        updateNodeChildren(node.id, toChildrenOrEmpty(children, node.id, t));
        return;
      }

      if (node.data.type === ExplorerNodeType.TABLE || node.data.type === ExplorerNodeType.VIEW) {
        const catalog = node.data.catalog ?? '';
        const schema = node.data.schema;
        const tableName = node.data.name;
        const columnsFolder = createFolderNode(node.id, FolderName.COLUMNS, connId, catalog, schema, t, tableName);
        const keysFolder = createFolderNode(node.id, FolderName.KEYS, connId, catalog, schema, t, tableName);
        const indexesFolder = createFolderNode(node.id, FolderName.INDEXES, connId, catalog, schema, t, tableName);
        updateNodeChildren(node.id, [columnsFolder, keysFolder, indexesFolder]);
      }
    } catch (err) {
      console.error('Failed to load node data:', err);
      toast.error(t('explorer.load_failed'));
    }
  };

  const handleDisconnect = (node: NodeApi<ExplorerNode>) => {
    if (!node.data.connectionId) return;
    const connId = Number(node.data.connectionId);
    if (isNaN(connId)) return;
    disconnectMutation.mutate(connId);
    setTreeDataState((prev) => {
      const update = (list: ExplorerNode[]): ExplorerNode[] =>
        list.map((n) => {
          if (n.id === node.id) return { ...n, connectionId: undefined, children: [] };
          if (n.children) return { ...n, children: update(n.children) };
          return n;
        });
      return update(prev);
    });
  };

  return {
    connections,
    treeDataState,
    setTreeDataState,
    loadNodeData,
    handleDisconnect,
    isConnectionsLoading,
    refetchConnections,
    deleteMutation,
  };
}
