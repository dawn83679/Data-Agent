import { useState, useCallback, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { connectionService } from '../services/connection.service';
import { databaseService } from '../services/database.service';
import { schemaService } from '../services/schema.service';
import { tableService } from '../services/table.service';
import { useWorkspaceStore } from '../store/workspaceStore';
import type { ChatContext } from '../types/chat';
import type { DbConnection } from '../types/connection';
import type { MentionItem, MentionLevel } from '../components/ai/mentionTypes';

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

  const [mentionOpen, setMentionOpen] = useState(false);
  const [mentionLevel, setMentionLevel] = useState<MentionLevel>('connection');
  const [mentionConnectionId, setMentionConnectionId] = useState<number | null>(null);
  const [mentionConnection, setMentionConnection] = useState<DbConnection | null>(null);
  const [mentionDatabaseName, setMentionDatabaseName] = useState<string | null>(null);
  const [mentionSchemaName, setMentionSchemaName] = useState<string | null>(null);
  const [mentionItems, setMentionItems] = useState<MentionItem[]>([]);
  const [mentionLoading, setMentionLoading] = useState(false);
  const [mentionError, setMentionError] = useState<string | null>(null);
  const [mentionHighlightedIndex, setMentionHighlightedIndex] = useState(0);

  const { data: connections = [] } = useQuery({
    queryKey: ['connections'],
    queryFn: () => connectionService.getConnections(),
    enabled: mentionOpen,
  });

  const supportSchema = mentionConnection
    ? (supportedDbTypes.find((opt) => opt.code === mentionConnection.dbType)?.supportSchema ?? false)
    : false;

  const loadDatabases = useCallback(async (connectionId: number) => {
    setMentionLoading(true);
    setMentionError(null);
    try {
      const names = await databaseService.listDatabases(String(connectionId));
      setMentionItems(
        names.map((name) => ({
          id: `db-${name}`,
          label: name,
          payload: { connectionId, databaseName: name },
        }))
      );
    } catch (err) {
      setMentionError(err instanceof Error ? err.message : t('ai.mention_error_load'));
      setMentionItems([]);
    } finally {
      setMentionLoading(false);
    }
  }, [t]);

  const loadSchemas = useCallback(
    async (connId: number, catalog: string) => {
      setMentionLoading(true);
      setMentionError(null);
      try {
        const names = await schemaService.listSchemas(String(connId), catalog);
        setMentionItems(
          names.map((name) => ({
            id: `schema-${name}`,
            label: name,
            payload: { connectionId: connId, databaseName: catalog, schemaName: name },
          }))
        );
      } catch (err) {
        setMentionError(err instanceof Error ? err.message : t('ai.mention_error_load'));
        setMentionItems([]);
      } finally {
        setMentionLoading(false);
      }
    },
    [t]
  );

  const loadTables = useCallback(
    async (connId: number, catalog: string, schema?: string) => {
      setMentionLoading(true);
      setMentionError(null);
      try {
        const names = await tableService.listTables(String(connId), catalog, schema);
        setMentionItems(
          names.map((name) => ({
            id: `table-${name}`,
            label: name,
          }))
        );
      } catch (err) {
        setMentionError(err instanceof Error ? err.message : t('ai.mention_error_load'));
        setMentionItems([]);
      } finally {
        setMentionLoading(false);
      }
    },
    [t]
  );

  useEffect(() => {
    if (!mentionOpen) return;
    if (mentionLevel === 'connection') {
      setMentionItems(
        connections.map((c) => ({
          id: `conn-${c.id}`,
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
    setMentionDatabaseName(null);
    setMentionSchemaName(null);
    setMentionOpen(true);
    setMentionItems(
      connections.map((c) => ({
        id: `conn-${c.id}`,
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
      setChatContext((prev) => ({ ...prev, databaseName: name }));
      setMentionDatabaseName(name);
      if (supportSchema) {
        setMentionLevel('schema');
        loadSchemas(mentionConnectionId, name);
      } else {
        setMentionLevel('table');
        loadTables(mentionConnectionId, name);
      }
      setMentionHighlightedIndex(0);
    },
    [mentionConnectionId, supportSchema, setChatContext, loadSchemas, loadTables]
  );

  const selectSchema = useCallback(
    (name: string) => {
      if (mentionConnectionId == null || mentionDatabaseName == null) return;
      setChatContext((prev) => ({ ...prev, schemaName: name }));
      setMentionSchemaName(name);
      setMentionLevel('table');
      loadTables(mentionConnectionId, mentionDatabaseName, name);
      setMentionHighlightedIndex(0);
    },
    [mentionConnectionId, mentionDatabaseName, setChatContext, loadTables]
  );

  const selectTable = useCallback(() => {
    setMentionOpen(false);
  }, []);

  /** Enter: confirm selection, update context, close popup, write path to input (short or full). */
  const handleMentionConfirm = useCallback(() => {
    if (mentionItems.length === 0) {
      closeMention();
      return;
    }
    const item = mentionItems[mentionHighlightedIndex];
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
      setChatContext((prev) => ({ ...prev, databaseName: item.label }));
      shortName = item.label;
      fullPath = `@${mentionConnection.name}/${item.label}`;
    } else if (mentionLevel === 'schema' && mentionConnection && mentionDatabaseName) {
      setChatContext((prev) => ({ ...prev, schemaName: item.label }));
      shortName = item.label;
      fullPath = `@${mentionConnection.name}/${mentionDatabaseName}/${item.label}`;
    } else if (mentionLevel === 'table' && mentionConnection && mentionDatabaseName) {
      const schemaPart = mentionSchemaName ? `/${mentionSchemaName}` : '';
      shortName = item.label;
      fullPath = `@${mentionConnection.name}/${mentionDatabaseName}${schemaPart}/${item.label}`;
    }

    closeMention();
    if (shortName && fullPath && onConfirmDisplay) onConfirmDisplay({ short: shortName, full: fullPath });
  }, [
    mentionItems,
    mentionHighlightedIndex,
    mentionLevel,
    mentionConnection,
    mentionDatabaseName,
    mentionSchemaName,
    connections,
    setChatContext,
    closeMention,
    onConfirmDisplay,
  ]);

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
      setMentionDatabaseName(null);
      setMentionSchemaName(null);
      setMentionLevel('connection');
      setMentionItems(
        connections.map((c) => ({
          id: `conn-${c.id}`,
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
      setMentionDatabaseName(null);
      setMentionSchemaName(null);
      setMentionLevel('database');
      if (mentionConnectionId != null) loadDatabases(mentionConnectionId);
      setMentionHighlightedIndex(0);
      return;
    }
    if (mentionLevel === 'table') {
      if (supportSchema && mentionDatabaseName) {
        setChatContext((prev) => ({ connectionId: prev.connectionId, databaseName: mentionDatabaseName }));
        setMentionSchemaName(null);
        setMentionLevel('schema');
        if (mentionConnectionId != null) loadSchemas(mentionConnectionId, mentionDatabaseName);
      } else {
        setChatContext((prev) => ({ connectionId: prev.connectionId }));
        setMentionDatabaseName(null);
        setMentionLevel('database');
        if (mentionConnectionId != null) loadDatabases(mentionConnectionId);
      }
      setMentionHighlightedIndex(0);
    }
  }, [
    mentionLevel,
    mentionConnectionId,
    mentionDatabaseName,
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
      } else if (mentionLevel === 'table') {
        selectTable();
      }
    },
    [mentionLevel, connections, selectConnection, selectDatabase, selectSchema, selectTable]
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
        handleMentionConfirm();
        return true;
      }
      // ArrowRight: drill down; no-op at table level, popup stays open
      if (e.key === 'ArrowRight') {
        e.preventDefault();
        if (mentionLevel === 'table') return true;
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
          : t('ai.mention_table');

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
