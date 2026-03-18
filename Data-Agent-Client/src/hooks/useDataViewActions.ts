import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { ExplorerNodeType } from '../constants/explorer';
import { tableService } from '../services/table.service';
import { viewService } from '../services/view.service';
import { functionService } from '../services/function.service';
import { procedureService } from '../services/procedure.service';
import { triggerService } from '../services/trigger.service';
import { useWorkspaceStore } from '../store/workspaceStore';
import { TableDblClickConsoleTargetEnum } from '../constants/workspacePreferences';
import { I18N_KEYS } from '../constants/i18nKeys';
import type { ExplorerNode } from '../types/explorer';
import type { ConsoleTabMetadata } from '../types/tab';

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
}: DataViewActionsProps) {
  const { t } = useTranslation();
  const { tableDblClickConsoleTarget, tabs, updateTabContent, switchTab } = useWorkspaceStore();

  const handleViewDdl = useCallback(
    async (node: ExplorerNode) => {
      const connId = node.connectionId || (node.type === ExplorerNodeType.ROOT ? node.dbConnection?.id : undefined);
      if (!connId) return;

      const catalog = node.catalog ?? '';
      const schema = node.schema ?? '';
      const objectName = node.objectName ?? node.name;
      const connectionName = node.dbConnection?.name || 'Unknown';
      let loadDdl: () => Promise<string>;

      switch (node.type) {
        case ExplorerNodeType.TABLE:
          loadDdl = () => tableService.getTableDdl(String(connId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.VIEW:
          loadDdl = () => viewService.getViewDdl(String(connId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.FUNCTION:
          loadDdl = () => functionService.getFunctionDdl(String(connId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.PROCEDURE:
          loadDdl = () => procedureService.getProcedureDdl(String(connId), objectName, catalog, schema);
          break;
        case ExplorerNodeType.TRIGGER:
          loadDdl = () => triggerService.getTriggerDdl(String(connId), objectName, catalog, schema);
          break;
        default:
          return;
      }

      // Open or reuse console tab
      const dbName = node.catalog || null;
      const schemaName = node.schema || null;
      const consoleTabs = tabs.filter((tab) =>
        tab.type === 'file'
        && (tab.metadata as ConsoleTabMetadata | undefined)?.connectionId === Number(connId)
      );
      const reuseTab =
        tableDblClickConsoleTarget === TableDblClickConsoleTargetEnum.REUSE && consoleTabs.length > 0;
      const tabId = reuseTab ? consoleTabs[0].id : `console-${Date.now()}`;
      const nameParts = [connectionName, dbName, schemaName].filter(Boolean);
      const tabName = nameParts.join('_') || 'console';

      if (!reuseTab) {
        openTab({
          id: tabId,
          name: tabName,
          type: 'file',
          content: '',
          metadata: {
            connectionId: Number(connId),
            connectionName,
            databaseName: dbName,
            schemaName: schemaName,
          },
        });
      } else {
        switchTab(tabId);
      }

      try {
        const ddl = await loadDdl();
        updateTabContent(tabId, ddl);
      } catch (err) {
        const errMsg = (err as Error).message || t(I18N_KEYS.EXPLORER.LOAD_DDL_FAILED);
        updateTabContent(tabId, `-- ${errMsg}\n`);
      }
    },
    [openTab, tabs, tableDblClickConsoleTarget, updateTabContent, switchTab, t]
  );

  const handleViewData = useCallback(
    (node: ExplorerNode, highlightCol?: string) => {
      if (!node.connectionId) return;

      const isTableOrView = node.type === ExplorerNodeType.TABLE || node.type === ExplorerNodeType.VIEW;
      const isColumnIndexKey = node.type === ExplorerNodeType.COLUMN || node.type === ExplorerNodeType.INDEX || node.type === ExplorerNodeType.KEY;

      let objectName: string;
      let objectType: 'table' | 'view';
      let connId = node.connectionId;
      let connectionName = node.dbConnection?.name || 'Unknown';
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
    [openTab, setHighlightColumn, setSelectedTableDataNode, setTableDataDialogOpen]
  );

  const handleOpenQueryConsole = useCallback((node: ExplorerNode) => {
    // For ROOT nodes (connections), get connectionId from dbConnection.id
    const connId = node.connectionId || (node.type === ExplorerNodeType.ROOT ? node.dbConnection?.id : undefined);
    if (!connId) {
      return;
    }

    const id = `console-${Date.now()}`;
    const connectionName = node.dbConnection?.name || 'Unknown';
    let databaseName: string | null = null;
    let schemaName: string | null = null;

    switch (node.type) {
      case ExplorerNodeType.ROOT:
        // Connection node - no database/schema selected
        break;
      case ExplorerNodeType.DB:
        databaseName = node.name;
        break;
      case ExplorerNodeType.SCHEMA:
        databaseName = node.catalog || null;
        schemaName = node.name;
        break;
      case ExplorerNodeType.TABLE:
      case ExplorerNodeType.VIEW:
      case ExplorerNodeType.COLUMN:
      case ExplorerNodeType.INDEX:
      case ExplorerNodeType.KEY:
      case ExplorerNodeType.FUNCTION:
      case ExplorerNodeType.PROCEDURE:
      case ExplorerNodeType.TRIGGER:
      case ExplorerNodeType.FOLDER:
        databaseName = node.catalog || null;
        schemaName = node.schema || null;
        break;
    }

    const nameParts = [connectionName, databaseName, schemaName].filter(Boolean);
    const tabName = nameParts.join('_') || 'console';

    openTab({
      id,
      name: tabName,
      type: 'file',
      content: '',
      metadata: {
        connectionId: Number(connId),
        connectionName,
        databaseName,
        schemaName,
      },
    });
  }, [openTab]);

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

  return { handleViewDdl, handleViewData, handleOpenQueryConsole, handleCreateTable, getDdlConfig };
}
