import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { ExplorerNodeType } from '../constants/explorer';
import { tableService } from '../services/table.service';
import { viewService } from '../services/view.service';
import { functionService } from '../services/function.service';
import { procedureService } from '../services/procedure.service';
import { triggerService } from '../services/trigger.service';
import { useWorkspaceStore } from '../store/workspaceStore';
import { TableDblClickConsoleTargetEnum, TableDblClickModeEnum } from '../constants/workspacePreferences';
import { I18N_KEYS } from '../constants/i18nKeys';
import type { ExplorerNode } from '../types/explorer';
import type { ConsoleTabMetadata } from '../types/tab';
import type { DbConnection } from '../types/connection';

interface DataViewActionsProps {
  setSelectedDdlNode: (node: ExplorerNode | null) => void;
  setDdlDialogOpen: (open: boolean) => void;
  setTableDataDialogOpen: (open: boolean) => void;
  setSelectedTableDataNode: (node: ExplorerNode | null) => void;
  setHighlightColumn: (col: string | undefined) => void;
  setCreateTableDialogOpen: (open: boolean) => void;
  setSelectedCreateTableNode: (node: ExplorerNode | null) => void;
  openTab: (tab: any) => void;
  selectedDdlNode: ExplorerNode | null;
  connections: DbConnection[];
}

interface ConsoleContext {
  connectionId: number;
  connectionName: string;
  databaseName: string | null;
  schemaName: string | null;
  dbType?: string;
}

function quoteIdentifier(name: string, dbType?: string): string {
  if (!name.trim()) return name;
  const isMysql = dbType?.toLowerCase().includes('mysql');
  return isMysql ? `\`${name}\`` : `"${name.replace(/"/g, '""')}"`;
}

function appendSql(existingContent: string, sql: string): string {
  if (!existingContent.trim()) return sql;
  return `${existingContent.replace(/\s*$/, '')}\n\n${sql}`;
}

export function useDataViewActions({
  setSelectedDdlNode: _setSelectedDdlNode,
  setDdlDialogOpen: _setDdlDialogOpen,
  setTableDataDialogOpen,
  setSelectedTableDataNode,
  setHighlightColumn,
  setCreateTableDialogOpen,
  setSelectedCreateTableNode,
  openTab,
  selectedDdlNode,
  connections,
}: DataViewActionsProps) {
  const { t } = useTranslation();
  const {
    tableDblClickMode,
    tableDblClickConsoleTarget,
    tabs,
    updateTabContent,
    updateTabMetadata,
    switchTab,
  } = useWorkspaceStore();

  const getConnection = useCallback(
    (connectionId: number) => connections.find((connection) => connection.id === connectionId),
    [connections]
  );

  const resolveConsoleContext = useCallback(
    (node: ExplorerNode): ConsoleContext | null => {
      const rawConnectionId =
        node.connectionId || (node.type === ExplorerNodeType.ROOT ? node.dbConnection?.id : undefined);
      if (!rawConnectionId) return null;

      const connectionId = Number(rawConnectionId);
      const connection = node.dbConnection ?? getConnection(connectionId);
      let databaseName: string | null = null;
      let schemaName: string | null = null;

      switch (node.type) {
        case ExplorerNodeType.DB:
          databaseName = node.name;
          break;
        case ExplorerNodeType.SCHEMA:
          databaseName = node.catalog || null;
          schemaName = node.name;
          break;
        case ExplorerNodeType.ROOT:
          break;
        default:
          databaseName = node.catalog || null;
          schemaName = node.schema || null;
          break;
      }

      return {
        connectionId,
        connectionName: connection?.name || node.dbConnection?.name || 'Unknown',
        databaseName,
        schemaName,
        dbType: connection?.dbType,
      };
    },
    [getConnection]
  );

  const ensureConsoleTab = useCallback(
    (node: ExplorerNode, options?: { forceNew?: boolean }) => {
      const context = resolveConsoleContext(node);
      if (!context) return null;

      const consoleTabs = tabs.filter((tab) =>
        tab.type === 'file'
        && (tab.metadata as ConsoleTabMetadata | undefined)?.connectionId === context.connectionId
      );
      const shouldReuse =
        !options?.forceNew
        && tableDblClickConsoleTarget === TableDblClickConsoleTargetEnum.REUSE
        && consoleTabs.length > 0;
      const tabId = shouldReuse ? consoleTabs[0].id : `console-${Date.now()}`;
      const tabName = [context.connectionName, context.databaseName, context.schemaName].filter(Boolean).join('_') || 'console';
      const metadata = {
        connectionId: context.connectionId,
        connectionName: context.connectionName,
        databaseName: context.databaseName,
        schemaName: context.schemaName,
      };

      if (shouldReuse) {
        updateTabMetadata(tabId, metadata);
        switchTab(tabId);
      } else {
        openTab({
          id: tabId,
          name: tabName,
          type: 'file',
          content: '',
          metadata,
        });
      }

      return { ...context, tabId };
    },
    [openTab, resolveConsoleContext, switchTab, tableDblClickConsoleTarget, tabs, updateTabMetadata]
  );

  const buildSelectSql = useCallback((node: ExplorerNode, dbType?: string) => {
    const objectName = node.tableName || node.objectName || node.name;
    const qualifiedName = [node.catalog, node.schema, objectName]
      .filter((part): part is string => typeof part === 'string' && part.length > 0)
      .map((part) => quoteIdentifier(part, dbType))
      .join('.');
    return `SELECT * FROM ${qualifiedName};`;
  }, []);

  const handleViewDdl = useCallback(
    async (node: ExplorerNode) => {
      const consoleContext = ensureConsoleTab(node);
      if (!consoleContext) return;

      const catalog = node.catalog ?? '';
      const schema = node.schema ?? '';
      const objectName = node.objectName ?? node.name;
      let loadDdl: () => Promise<string>;

      switch (node.type) {
        case ExplorerNodeType.TABLE:
          loadDdl = () => tableService.getTableDdl(String(consoleContext.connectionId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.VIEW:
          loadDdl = () => viewService.getViewDdl(String(consoleContext.connectionId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.FUNCTION:
          loadDdl = () => functionService.getFunctionDdl(String(consoleContext.connectionId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.PROCEDURE:
          loadDdl = () => procedureService.getProcedureDdl(String(consoleContext.connectionId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.TRIGGER:
          loadDdl = () => triggerService.getTriggerDdl(String(consoleContext.connectionId), objectName, catalog, schema);
          break;
        default:
          return;
      }

      try {
        const ddl = await loadDdl();
        updateTabContent(consoleContext.tabId, ddl);
      } catch (err) {
        const errMsg = (err as Error).message || t(I18N_KEYS.EXPLORER.LOAD_DDL_FAILED);
        updateTabContent(consoleContext.tabId, `-- ${errMsg}\n`);
      }
    },
    [ensureConsoleTab, updateTabContent, t]
  );

  const handleViewData = useCallback(
    (node: ExplorerNode, highlightCol?: string) => {
      if (!node.connectionId) return;

      const isTableOrView = node.type === ExplorerNodeType.TABLE || node.type === ExplorerNodeType.VIEW;
      const isColumnIndexKey = node.type === ExplorerNodeType.COLUMN || node.type === ExplorerNodeType.INDEX || node.type === ExplorerNodeType.KEY;

      let objectName: string;
      let objectType: 'table' | 'view';
      let connId = node.connectionId;
      const connection = connId ? getConnection(Number(connId)) : undefined;
      let connectionName = node.dbConnection?.name || connection?.name || 'Unknown';
      let databaseName = node.catalog || node.schema || null;
      let schemaName = node.schema || null;

      if (isTableOrView) {
        objectName = node.tableName || node.objectName || node.name;
        objectType = node.type === ExplorerNodeType.TABLE ? 'table' : 'view';
      } else if (isColumnIndexKey && node.tableName) {
        objectName = node.tableName;
        objectType = node.id?.includes('folder-views') ? 'view' : 'table';
      } else {
        setHighlightColumn(highlightCol);
        setSelectedTableDataNode(node);
        setTableDataDialogOpen(true);
        return;
      }

      const tabId = `table-${connId}-${objectName}-${Date.now()}`;

      openTab({
        id: tabId,
        name: objectName,
        type: 'table',
        content: '',
        metadata: {
          connectionId: Number(connId),
          connectionName,
          databaseName,
          schemaName,
          objectName,
          objectType,
          catalog: node.catalog,
          schema: node.schema,
        },
      });
    },
    [getConnection, openTab, setHighlightColumn, setSelectedTableDataNode, setTableDataDialogOpen]
  );

  const handleTableOrViewDoubleClick = useCallback(
    (node: ExplorerNode) => {
      if (tableDblClickMode === TableDblClickModeEnum.TABLE) {
        handleViewData(node);
        return;
      }

      const consoleContext = ensureConsoleTab(node);
      if (!consoleContext) return;

      const sql = buildSelectSql(node, consoleContext.dbType);
      const existingContent = tabs.find((tab) => tab.id === consoleContext.tabId)?.content ?? '';
      updateTabContent(consoleContext.tabId, appendSql(existingContent, sql));
      switchTab(consoleContext.tabId);
    },
    [buildSelectSql, ensureConsoleTab, handleViewData, tableDblClickMode, tabs, updateTabContent, switchTab]
  );

  const handleOpenQueryConsole = useCallback((node: ExplorerNode) => {
    ensureConsoleTab(node, { forceNew: true });
  }, [ensureConsoleTab]);

  const getDdlConfig = useCallback(() => {
    if (!selectedDdlNode?.connectionId) return null;
    const node = selectedDdlNode;
    const connId = String(node.connectionId);
    const catalog = node.catalog ?? '';
    const schema = node.schema ?? '';
    const displayName = [node.catalog, node.schema, node.name].filter(Boolean).join('.');
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
  }, [t, selectedDdlNode]);

  const handleCreateTable = useCallback((node: ExplorerNode) => {
    setSelectedCreateTableNode(node);
    setCreateTableDialogOpen(true);
  }, [setSelectedCreateTableNode, setCreateTableDialogOpen]);

  return {
    handleViewDdl,
    handleViewData,
    handleTableOrViewDoubleClick,
    handleOpenQueryConsole,
    handleCreateTable,
    getDdlConfig,
  };
}
