import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { ExplorerNodeType } from '../constants/explorer';
import { tableService } from '../services/table.service';
import { viewService } from '../services/view.service';
import { functionService } from '../services/function.service';
import { procedureService } from '../services/procedure.service';
import { triggerService } from '../services/trigger.service';
import type { ExplorerNode } from '../types/explorer';

interface DataViewActionsProps {
  setSelectedDdlNode: (node: ExplorerNode | null) => void;
  setDdlDialogOpen: (open: boolean) => void;
  setTableDataDialogOpen: (open: boolean) => void;
  setSelectedTableDataNode: (node: ExplorerNode | null) => void;
  setHighlightColumn: (col: string | undefined) => void;
  openTab: (tab: any) => void;
  selectedDdlNode: ExplorerNode | null;
}

export function useDataViewActions({
  setSelectedDdlNode,
  setDdlDialogOpen,
  setTableDataDialogOpen,
  setSelectedTableDataNode,
  setHighlightColumn,
  openTab,
  selectedDdlNode,
}: DataViewActionsProps) {
  const { t } = useTranslation();

  const handleViewDdl = useCallback((node: ExplorerNode) => {
    setSelectedDdlNode(node);
    setDdlDialogOpen(true);
  }, [setSelectedDdlNode, setDdlDialogOpen]);

  const handleViewData = useCallback((node: ExplorerNode, highlightCol?: string) => {
    if (!node.connectionId) return;
    setHighlightColumn(highlightCol);
    setSelectedTableDataNode(node);
    setTableDataDialogOpen(true);
  }, [setHighlightColumn, setSelectedTableDataNode, setTableDataDialogOpen]);

  const handleOpenQueryConsole = useCallback((node: ExplorerNode) => {
    if (!node.connectionId) {
      return;
    }

    const id = `console-${Date.now()}`;
    const connectionName = node.dbConnection?.name || 'Unknown';
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
        connectionId: Number(node.connectionId),
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

  return { handleViewDdl, handleViewData, handleOpenQueryConsole, getDdlConfig };
}
