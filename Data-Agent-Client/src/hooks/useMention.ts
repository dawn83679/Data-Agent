import { useState, useCallback, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { connectionService } from '../services/connection.service';
import { useWorkspaceStore } from '../store/workspaceStore';
import { useAuthStore } from '../store/authStore';
import { MentionIdPrefix } from '../constants/explorer';
import type { ChatUserMention } from '../types/chat';
import type { ChatContext } from '../types/chat';
import type { DbConnection } from '../types/connection';
import type { MentionItem, MentionLevel, MentionObjectType } from '../components/ai/mentionTypes';
import {
  fetchExplorerDatabases,
  fetchExplorerFunctions,
  fetchExplorerSchemas,
  fetchExplorerProcedures,
  fetchExplorerTables,
  fetchExplorerTriggers,
  fetchExplorerViews,
  getCachedExplorerDatabases,
  getCachedExplorerFunctions,
  getCachedExplorerProcedures,
  getCachedExplorerSchemas,
  getCachedExplorerTables,
  getCachedExplorerTriggers,
  getCachedExplorerViews,
} from '../lib/explorerMetadataCache';

export interface UseMentionReturn {
  mentionOpen: boolean;
  mentionLevel: MentionLevel;
  mentionItems: MentionItem[];
  mentionLoading: boolean;
  mentionError: string | null;
  mentionHighlightedIndex: number;
  mentionLevelLabel: string;
  openMention: () => void;
  closeMention: () => void;
  setMentionHighlightedIndex: (index: number | ((prev: number) => number)) => void;
  handleMentionSelect: (item: MentionItem) => void;
  handleMentionKeyDown: (e: React.KeyboardEvent) => boolean;
}

export interface MentionDisplayPayload {
  /** Short label (last segment) for display. */
  short: string;
  /** Full path for disambiguation when names collide. */
  full: string;
  /** Structured mention metadata for runtime prompt rendering. */
  mention?: Omit<ChatUserMention, 'token'>;
}

export interface UseMentionOptions {
  setChatContext: React.Dispatch<React.SetStateAction<ChatContext>>;
  /** Called after Enter confirms selection; inserts short or full path into input based on disambiguation. */
  onConfirmDisplay?: (payload: MentionDisplayPayload) => void;
}

export function useMention(options: UseMentionOptions): UseMentionReturn {
  const { setChatContext, onConfirmDisplay } = options;
  const { t } = useTranslation();
  const { supportedDbTypes } = useWorkspaceStore();
  const queryClient = useQueryClient();

  const [mentionOpen, setMentionOpen] = useState(false);
  const [mentionLevel, setMentionLevel] = useState<MentionLevel>('connection');
  const [mentionConnectionId, setMentionConnectionId] = useState<number | null>(null);
  const [mentionConnection, setMentionConnection] = useState<DbConnection | null>(null);
  const [mentionCatalogName, setMentionCatalogName] = useState<string | null>(null);
  const [mentionSchemaName, setMentionSchemaName] = useState<string | null>(null);
  const [mentionItems, setMentionItems] = useState<MentionItem[]>([]);
  const [mentionLoading, setMentionLoading] = useState(false);
  const [mentionError, setMentionError] = useState<string | null>(null);
  const [mentionHighlightedIndex, setMentionHighlightedIndex] = useState(0);

  const accessToken = useAuthStore((s) => s.accessToken);
  const { data: connections = [] } = useQuery({
    queryKey: ['connections'],
    queryFn: () => connectionService.getConnections(),
    enabled: mentionOpen && !!accessToken,
  });

  const supportSchema = mentionConnection
    ? (supportedDbTypes.find((opt) => opt.code === mentionConnection.dbType)?.supportSchema ?? false)
    : false;

  const loadDatabases = useCallback(async (connectionId: number) => {
    setMentionError(null);
    try {
      const connId = String(connectionId);
      const cached = getCachedExplorerDatabases(queryClient, connId);
      const names = cached ?? await (async () => {
        setMentionLoading(true);
        return fetchExplorerDatabases(queryClient, connId);
      })();
      setMentionItems(
        names.map((name) => ({
          id: `${MentionIdPrefix.DB}${name}`,
          label: name,
          payload: { connectionId, catalogName: name },
        }))
      );
    } catch (err) {
      setMentionError(err instanceof Error ? err.message : t('ai.mention_error_load'));
      setMentionItems([]);
    } finally {
      setMentionLoading(false);
    }
  }, [queryClient, t]);

  const loadSchemas = useCallback(
    async (connId: number, catalog: string) => {
      setMentionError(null);
      try {
        const connectionId = String(connId);
        const cached = getCachedExplorerSchemas(queryClient, connectionId, catalog);
        const names = cached ?? await (async () => {
          setMentionLoading(true);
          return fetchExplorerSchemas(queryClient, connectionId, catalog);
        })();
        setMentionItems(
          names.map((name) => ({
            id: `${MentionIdPrefix.SCHEMA}${name}`,
            label: name,
            payload: { connectionId: connId, catalogName: catalog, schemaName: name },
          }))
        );
      } catch (err) {
        setMentionError(err instanceof Error ? err.message : t('ai.mention_error_load'));
        setMentionItems([]);
      } finally {
        setMentionLoading(false);
      }
    },
    [queryClient, t]
  );

  const loadObjects = useCallback(
    async (connId: number, catalog: string, schema?: string) => {
      setMentionError(null);
      try {
        const connectionId = String(connId);
        const cachedTables = getCachedExplorerTables(queryClient, connectionId, catalog, schema);
        const cachedViews = getCachedExplorerViews(queryClient, connectionId, catalog, schema);
        const cachedFunctions = getCachedExplorerFunctions(queryClient, connectionId, catalog, schema);
        const cachedProcedures = getCachedExplorerProcedures(queryClient, connectionId, catalog, schema);
        const cachedTriggers = getCachedExplorerTriggers(queryClient, connectionId, catalog, schema);

        let loadingStarted = false;
        const startLoading = () => {
          if (!loadingStarted) {
            setMentionLoading(true);
            loadingStarted = true;
          }
        };

        const [tableResult, viewResult, functionResult, procedureResult, triggerResult] = await Promise.allSettled([
          cachedTables
            ? Promise.resolve(cachedTables)
            : (async () => {
                startLoading();
                return fetchExplorerTables(queryClient, connectionId, catalog, schema);
              })(),
          cachedViews
            ? Promise.resolve(cachedViews)
            : (async () => {
                startLoading();
                return fetchExplorerViews(queryClient, connectionId, catalog, schema);
              })(),
          cachedFunctions
            ? Promise.resolve(cachedFunctions)
            : (async () => {
                startLoading();
                return fetchExplorerFunctions(queryClient, connectionId, catalog, schema);
              })(),
          cachedProcedures
            ? Promise.resolve(cachedProcedures)
            : (async () => {
                startLoading();
                return fetchExplorerProcedures(queryClient, connectionId, catalog, schema);
              })(),
          cachedTriggers
            ? Promise.resolve(cachedTriggers)
            : (async () => {
                startLoading();
                return fetchExplorerTriggers(queryClient, connectionId, catalog, schema);
              })(),
        ]);

        const tables = tableResult.status === 'fulfilled' ? tableResult.value : [];
        const views = viewResult.status === 'fulfilled' ? viewResult.value : [];
        const functions = functionResult.status === 'fulfilled' ? functionResult.value : [];
        const procedures = procedureResult.status === 'fulfilled' ? procedureResult.value : [];
        const triggers = triggerResult.status === 'fulfilled' ? triggerResult.value : [];

        if (
          tableResult.status === 'rejected'
          && viewResult.status === 'rejected'
          && functionResult.status === 'rejected'
          && procedureResult.status === 'rejected'
          && triggerResult.status === 'rejected'
        ) {
          throw tableResult.reason;
        }

        const toMentionItem = (name: string, objectType: MentionObjectType, detail?: string): MentionItem => ({
          id: `${objectType}:${name}`,
          label: name,
          detail,
          payload: {
            connectionId: connId,
            connectionName: mentionConnection?.name,
            catalogName: catalog,
            schemaName: schema,
            objectName: name,
            objectType,
          },
        });

        setMentionItems([
          ...tables.map((name) => toMentionItem(name, 'TABLE')),
          ...views.map((name) => toMentionItem(name, 'VIEW')),
          ...functions.map((item) => toMentionItem(item.name, 'FUNCTION')),
          ...procedures.map((item) => toMentionItem(item.name, 'PROCEDURE')),
          ...triggers.map((item) => toMentionItem(item.name, 'TRIGGER')),
        ]);
      } catch (err) {
        setMentionError(err instanceof Error ? err.message : t('ai.mention_error_load'));
        setMentionItems([]);
      } finally {
        setMentionLoading(false);
      }
    },
    [mentionConnection?.name, queryClient, t]
  );

  useEffect(() => {
    if (!mentionOpen) return;
    if (mentionLevel === 'connection') {
      setMentionItems(
        connections.map((c) => ({
          id: `${MentionIdPrefix.CONNECTION}${c.id}`,
          label: c.name,
          payload: { connectionId: c.id },
        }))
      );
      setMentionHighlightedIndex(0);
      setMentionError(null);
    }
  }, [mentionOpen, mentionLevel, connections]);

  const openMention = useCallback(() => {
    setMentionLevel('connection');
    setMentionConnectionId(null);
    setMentionConnection(null);
    setMentionCatalogName(null);
    setMentionSchemaName(null);
    setMentionOpen(true);
    setMentionItems(
      connections.map((c) => ({
        id: `${MentionIdPrefix.CONNECTION}${c.id}`,
        label: c.name,
        payload: { connectionId: c.id },
      }))
    );
    setMentionHighlightedIndex(0);
    setMentionError(null);
  }, [connections]);

  const closeMention = useCallback(() => {
    setMentionOpen(false);
  }, []);

  const selectConnection = useCallback(
    (conn: DbConnection) => {
      setChatContext({ connectionId: conn.id });
      setMentionConnectionId(conn.id);
      setMentionConnection(conn);
      setMentionLevel('database');
      loadDatabases(conn.id);
      setMentionHighlightedIndex(0);
    },
    [setChatContext, loadDatabases]
  );

  const selectDatabase = useCallback(
    (name: string) => {
      if (mentionConnectionId == null) return;
      setChatContext((prev) => ({ ...prev, catalogName: name }));
      setMentionCatalogName(name);
      if (supportSchema) {
        setMentionLevel('schema');
        loadSchemas(mentionConnectionId, name);
      } else {
        setMentionLevel('object');
        loadObjects(mentionConnectionId, name);
      }
      setMentionHighlightedIndex(0);
    },
    [mentionConnectionId, supportSchema, setChatContext, loadSchemas, loadObjects]
  );

  const selectSchema = useCallback(
    (name: string) => {
      if (mentionConnectionId == null || mentionCatalogName == null) return;
      setChatContext((prev) => ({ ...prev, schemaName: name }));
      setMentionSchemaName(name);
      setMentionLevel('object');
      loadObjects(mentionConnectionId, mentionCatalogName, name);
      setMentionHighlightedIndex(0);
    },
    [mentionConnectionId, mentionCatalogName, setChatContext, loadObjects]
  );

  const confirmMentionItem = useCallback((item: MentionItem | undefined) => {
    if (!item) {
      closeMention();
      return;
    }
    let shortName = '';
    let fullPath = '';

    if (mentionLevel === 'connection') {
      const conn = connections.find((c) => c.id === Number(item.payload?.connectionId));
      if (conn) {
        setChatContext({ connectionId: conn.id });
        shortName = conn.name;
        fullPath = `@${conn.name}`;
      }
    } else if (mentionLevel === 'database' && mentionConnection) {
      setChatContext((prev) => ({ ...prev, catalogName: item.label }));
      shortName = item.label;
      fullPath = `@${mentionConnection.name}/${item.label}`;
    } else if (mentionLevel === 'schema' && mentionConnection && mentionCatalogName) {
      setChatContext((prev) => ({ ...prev, schemaName: item.label }));
      shortName = item.label;
      fullPath = `@${mentionConnection.name}/${mentionCatalogName}/${item.label}`;
    } else if (mentionLevel === 'object' && mentionConnection && mentionCatalogName) {
      const schemaPart = mentionSchemaName ? `/${mentionSchemaName}` : '';
      shortName = item.label;
      fullPath = `@${mentionConnection.name}/${mentionCatalogName}${schemaPart}/${item.label}`;
    }

    closeMention();
    if (shortName && fullPath && onConfirmDisplay) {
      onConfirmDisplay({
        short: shortName,
        full: fullPath,
        mention: item.payload?.objectType && item.payload?.objectName
          ? {
              objectType: item.payload.objectType,
              connectionId: item.payload.connectionId,
              connectionName: item.payload.connectionName ?? mentionConnection?.name ?? '',
              catalogName: item.payload.catalogName,
              schemaName: item.payload.schemaName,
              objectName: item.payload.objectName,
            }
          : undefined,
      });
    }
  }, [
    mentionLevel,
    mentionConnection,
    mentionCatalogName,
    mentionSchemaName,
    connections,
    setChatContext,
    closeMention,
    onConfirmDisplay,
  ]);

  /** Enter: confirm selection, update context, close popup, write path to input (short or full). */
  const handleMentionConfirm = useCallback(() => {
    if (mentionItems.length === 0) {
      closeMention();
      return;
    }
    confirmMentionItem(mentionItems[mentionHighlightedIndex]);
  }, [mentionItems, mentionHighlightedIndex, closeMention, confirmMentionItem]);

  /** ArrowLeft: go back one level. */
  const handleMentionBack = useCallback(() => {
    if (mentionLevel === 'connection') {
      closeMention();
      return;
    }
    if (mentionLevel === 'database') {
      setChatContext({});
      setMentionConnectionId(null);
      setMentionConnection(null);
      setMentionCatalogName(null);
      setMentionSchemaName(null);
      setMentionLevel('connection');
      setMentionItems(
        connections.map((c) => ({
          id: `${MentionIdPrefix.CONNECTION}${c.id}`,
          label: c.name,
          payload: { connectionId: c.id },
        }))
      );
      setMentionHighlightedIndex(0);
      setMentionError(null);
      return;
    }
    if (mentionLevel === 'schema') {
      setChatContext((prev) => ({ connectionId: prev.connectionId }));
      setMentionCatalogName(null);
      setMentionSchemaName(null);
      setMentionLevel('database');
      if (mentionConnectionId != null) loadDatabases(mentionConnectionId);
      setMentionHighlightedIndex(0);
      return;
    }
    if (mentionLevel === 'object') {
      if (supportSchema && mentionCatalogName) {
        setChatContext((prev) => ({ connectionId: prev.connectionId, catalogName: mentionCatalogName }));
        setMentionSchemaName(null);
        setMentionLevel('schema');
        if (mentionConnectionId != null) loadSchemas(mentionConnectionId, mentionCatalogName);
      } else {
        setChatContext((prev) => ({ connectionId: prev.connectionId }));
        setMentionCatalogName(null);
        setMentionLevel('database');
        if (mentionConnectionId != null) loadDatabases(mentionConnectionId);
      }
      setMentionHighlightedIndex(0);
    }
  }, [
    mentionLevel,
    mentionConnectionId,
    mentionCatalogName,
    supportSchema,
    connections,
    closeMention,
    setChatContext,
    loadDatabases,
    loadSchemas,
  ]);

  const handleMentionSelect = useCallback(
    (item: MentionItem) => {
      if (mentionLevel === 'connection') {
        const conn = connections.find((c) => c.id === Number(item.payload?.connectionId));
        if (conn) selectConnection(conn);
      } else if (mentionLevel === 'database') {
        selectDatabase(item.label);
      } else if (mentionLevel === 'schema') {
        selectSchema(item.label);
      } else if (mentionLevel === 'object') {
        confirmMentionItem(item);
      }
    },
    [mentionLevel, connections, selectConnection, selectDatabase, selectSchema, confirmMentionItem]
  );

  const handleMentionKeyDown = useCallback(
    (e: React.KeyboardEvent): boolean => {
      if (!mentionOpen) return false;
      if (e.key === 'Escape') {
        e.preventDefault();
        closeMention();
        return true;
      }
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setMentionHighlightedIndex((i) => (i + 1) % Math.max(1, mentionItems.length));
        return true;
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        setMentionHighlightedIndex((i) => (i - 1 + mentionItems.length) % Math.max(1, mentionItems.length));
        return true;
      }
      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        handleMentionBack();
        return true;
      }
      // Enter: confirm selection, write to input and close (do not drill down)
      if (e.key === 'Enter') {
        e.preventDefault();
        if (mentionLevel === 'object') {
          handleMentionConfirm();
        } else if (mentionItems.length > 0) {
          const item = mentionItems[mentionHighlightedIndex];
          if (item) handleMentionSelect(item);
        }
        return true;
      }
      // ArrowRight: drill down; no-op at object level, popup stays open
      if (e.key === 'ArrowRight') {
        e.preventDefault();
        if (mentionLevel === 'object') return true;
        if (mentionItems.length > 0) {
          const item = mentionItems[mentionHighlightedIndex];
          if (item) handleMentionSelect(item);
        }
        return true;
      }
      return false;
    },
    [mentionOpen, mentionLevel, mentionItems, mentionHighlightedIndex, handleMentionConfirm, handleMentionBack, handleMentionSelect]
  );

  const mentionLevelLabel =
    mentionLevel === 'connection'
      ? t('ai.mention_data_source')
      : mentionLevel === 'database'
        ? t('ai.mention_database')
        : mentionLevel === 'schema'
          ? t('ai.mention_schema')
          : t('ai.mention_object');

  return {
    mentionOpen,
    mentionLevel,
    mentionItems,
    mentionLoading,
    mentionError,
    mentionHighlightedIndex,
    mentionLevelLabel,
    openMention,
    closeMention,
    setMentionHighlightedIndex,
    handleMentionSelect,
    handleMentionKeyDown,
  };
}
