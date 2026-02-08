import { useState, useMemo, useEffect } from 'react';
import { NodeApi } from 'react-arborist';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { connectionService } from '../services/connection.service';
import { databaseService } from '../services/database.service';
import { schemaService } from '../services/schema.service';
import { tableService } from '../services/table.service';
import { useWorkspaceStore } from '../store/workspaceStore';
import type { DbConnection } from '../types/connection';

export interface ExplorerNode {
  id: string;
  name: string;
  type: 'root' | 'db' | 'schema' | 'table' | 'folder';
  connectionId?: string;
  dbConnection?: DbConnection;
  children?: ExplorerNode[];
  catalog?: string;
  schema?: string;
}

export function useConnectionTree() {
  const queryClient = useQueryClient();
  const { supportedDbTypes } = useWorkspaceStore();

  const { data, isLoading: isConnectionsLoading, refetch: refetchConnections } = useQuery({
    queryKey: ['connections'],
    queryFn: () => connectionService.getConnections(),
  });

  const connections = useMemo(() => data || [], [data]);

  const treeData = useMemo<ExplorerNode[]>(() => {
    return connections.map((conn) => ({
      id: `conn-${conn.id}`,
      name: conn.name,
      type: 'root' as const,
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
      queryClient.invalidateQueries({ queryKey: ['connections'] });
    },
  });

  const disconnectMutation = useMutation({
    mutationFn: (connectionId: number) => connectionService.closeConnection(connectionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connections'] });
    },
  });

  const loadNodeData = async (node: NodeApi<ExplorerNode>) => {
    if (!node.data.connectionId && node.data.type !== 'root') return;
    
    const connId = node.data.connectionId || (node.data.dbConnection ? String(node.data.dbConnection.id) : undefined);
    
    if (!connId) return;

    try {
      if (node.data.type === 'root' && node.data.dbConnection) {
        const dbNames = await databaseService.listDatabases(String(node.data.dbConnection.id));
        const childrenNodes: ExplorerNode[] = dbNames.map((name) => ({
          id: `${node.id}-db-${name}`,
          name,
          type: 'db',
          connectionId: String(node.data.dbConnection!.id),
          children: [],
        }));

        updateNodeChildren(node.id, childrenNodes, String(node.data.dbConnection.id));
      } else if (node.data.type === 'db') {
        const dbName = node.data.name;
        
        let rootNode = node;
        while (rootNode.parent && rootNode.level > 0) {
            rootNode = rootNode.parent;
        }
        
        const dbConnection = rootNode.data.dbConnection;
        const dbType = dbConnection?.dbType;
        const typeOption = supportedDbTypes.find(t => t.code === dbType);
        const supportSchema = typeOption?.supportSchema ?? false;

        if (supportSchema) {
             const schemas = await schemaService.listSchemas(connId, dbName);
             const childrenNodes: ExplorerNode[] = schemas.map((schema) => ({
               id: `${node.id}-schema-${schema}`,
               name: schema,
               type: 'schema',
               connectionId: connId,
               children: [],
             }));
             updateNodeChildren(node.id, childrenNodes);
        } else {
             const tables = await tableService.listTables(connId, dbName);
             const childrenNodes: ExplorerNode[] = tables.map((table) => ({
               id: `${node.id}-table-${table}`,
               name: table,
               type: 'table',
               connectionId: connId,
               catalog: dbName,
             }));
             updateNodeChildren(node.id, childrenNodes);
        }
      } else if (node.data.type === 'schema') {
        const dbNode = node.parent;
        const dbName = dbNode?.data.name;
        const schemaName = node.data.name;
        
        const tables = await tableService.listTables(connId, dbName, schemaName);
        const childrenNodes: ExplorerNode[] = tables.map((table) => ({
          id: `${node.id}-table-${table}`,
          name: table,
          type: 'table',
          connectionId: connId,
          catalog: dbName,
          schema: schemaName,
        }));
        updateNodeChildren(node.id, childrenNodes);
      }
    } catch (err) {
      console.error('Failed to load node data:', err);
    }
  };

  const updateNodeChildren = (nodeId: string, children: ExplorerNode[], connectionId?: string) => {
    setTreeDataState((prev) => {
      const update = (list: ExplorerNode[]): ExplorerNode[] =>
        list.map((n) => {
          if (n.id === nodeId) {
            return { 
              ...n, 
              children,
              ...(connectionId ? { connectionId } : {})
            };
          }
          if (n.children) return { ...n, children: update(n.children) };
          return n;
        });
      return update(prev);
    });
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
