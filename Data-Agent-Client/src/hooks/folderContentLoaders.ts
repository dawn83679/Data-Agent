import { tableService } from '../services/table.service';
import { viewService } from '../services/view.service';
import { functionService } from '../services/function.service';
import { procedureService } from '../services/procedure.service';
import { triggerService } from '../services/trigger.service';
import { columnService } from '../services/column.service';
import { indexService } from '../services/index.service';
import { primaryKeyService } from '../services/primaryKey.service';
import { ExplorerNodeType, ExplorerIdPrefix, TRIGGER_ON_TABLE_FORMAT, INDEX_UNIQUE_SUFFIX } from '../constants/explorer';
import type { ExplorerNode } from '../types/explorer';
import type { ParameterInfo } from '../services/function.service';

export function formatRoutineSignature(
  parameters: ParameterInfo[] | undefined | null,
  returnType?: string | null
): string {
  const params = (parameters ?? [])
    .filter((p) => p.name)
    .map((p) => `${p.name}: ${p.dataType}`)
    .join(', ');
  const sig = `(${params})`;
  return returnType ? `${sig}: ${returnType}` : sig;
}
import { getColumnDisplayType } from '../utils/columnDisplay';

export type FolderLoadContext = {
  connId: string;
  catalog: string;
  schema: string | undefined;
  folderId: string;
  tableName?: string;
};

async function loadTables(ctx: FolderLoadContext): Promise<ExplorerNode[]> {
  const { connId, catalog, schema, folderId: fid } = ctx;
  const tables = await tableService.listTables(connId, catalog, schema);
  return tables.map((name) => ({
    id: `${fid}${ExplorerIdPrefix.TABLE}${name}`,
    name,
    type: ExplorerNodeType.TABLE,
    connectionId: connId,
    catalog,
    schema,
    children: [],
  }));
}

async function loadViews(ctx: FolderLoadContext): Promise<ExplorerNode[]> {
  const { connId, catalog, schema, folderId: fid } = ctx;
  const views = await viewService.listViews(connId, catalog, schema);
  return views.map((name) => ({
    id: `${fid}${ExplorerIdPrefix.VIEW}${name}`,
    name,
    type: ExplorerNodeType.VIEW,
    connectionId: connId,
    catalog,
    schema,
    children: [],
  }));
}

async function loadRoutines(ctx: FolderLoadContext): Promise<ExplorerNode[]> {
  const { connId, catalog, schema, folderId: fid } = ctx;
  const [functions, procedures] = await Promise.all([
    functionService.listFunctions(connId, catalog, schema),
    procedureService.listProcedures(connId, catalog, schema),
  ]);
  return [
    ...functions.map((fn) => ({
      id: `${fid}${ExplorerIdPrefix.FUNCTION}${fn.name}`,
      name: fn.name,
      type: ExplorerNodeType.FUNCTION,
      connectionId: connId,
      catalog,
      schema,
      signature: formatRoutineSignature(fn.parameters, fn.returnType),
    })),
    ...procedures.map((proc) => ({
      id: `${fid}${ExplorerIdPrefix.PROCEDURE}${proc.name}`,
      name: proc.name,
      type: ExplorerNodeType.PROCEDURE,
      connectionId: connId,
      catalog,
      schema,
      signature: formatRoutineSignature(proc.parameters),
    })),
  ];
}

async function loadTriggers(ctx: FolderLoadContext): Promise<ExplorerNode[]> {
  const { connId, catalog, schema, folderId: fid } = ctx;
  const triggers = await triggerService.listTriggers(connId, catalog, schema);
  return triggers.map((tr) => ({
    id: `${fid}${ExplorerIdPrefix.TRIGGER}${tr.name}`,
    name: tr.tableName ? TRIGGER_ON_TABLE_FORMAT(tr.name, tr.tableName) : tr.name,
    type: ExplorerNodeType.TRIGGER,
    connectionId: connId,
    catalog,
    schema,
    objectName: tr.name,
  }));
}

async function loadColumns(ctx: FolderLoadContext): Promise<ExplorerNode[]> {
  const { connId, catalog, schema, folderId: fid, tableName } = ctx;
  if (!tableName) return [];
  const columns = await columnService.listColumns(connId, tableName, catalog, schema);
  return columns.map((col) => ({
    id: `${fid}${ExplorerIdPrefix.COLUMN}${col.name}`,
    name: col.name,
    type: ExplorerNodeType.COLUMN,
    connectionId: connId,
    catalog,
    schema,
    tableName,
    signature: getColumnDisplayType(col) || undefined,
    isPrimaryKey: col.isPrimaryKeyPart ?? false,
  }));
}

async function loadKeys(ctx: FolderLoadContext): Promise<ExplorerNode[]> {
  const { connId, catalog, schema, folderId: fid, tableName } = ctx;
  if (!tableName) return [];
  const primaryKeys = await primaryKeyService.listPrimaryKeys(connId, tableName, catalog, schema);
  return primaryKeys.map((pk) => {
    const colList = pk.columnNames?.length ? pk.columnNames.join(', ') : '';
    const displayName = colList ? `${pk.name} (${colList})` : pk.name;
    return {
      id: `${fid}${ExplorerIdPrefix.KEY}${pk.name}`,
      name: displayName,
      type: ExplorerNodeType.KEY,
      connectionId: connId,
      catalog,
      schema,
      tableName,
    };
  });
}

async function loadIndexes(ctx: FolderLoadContext): Promise<ExplorerNode[]> {
  const { connId, catalog, schema, folderId: fid, tableName } = ctx;
  if (!tableName) return [];
  const indexes = await indexService.listIndexes(connId, tableName, catalog, schema);
  return indexes.map((idx) => {
    const colList = idx.columns?.length ? idx.columns.join(', ') : '';
    const displayName = colList ? `${idx.name} (${colList})` : idx.name;
    const suffix = idx.unique ? INDEX_UNIQUE_SUFFIX : '';
    return {
      id: `${fid}${ExplorerIdPrefix.INDEX}${idx.name}`,
      name: displayName + suffix,
      type: ExplorerNodeType.INDEX,
      connectionId: connId,
      catalog,
      schema,
      tableName,
      isPrimaryKey: idx.isPrimaryKey ?? false,
    };
  });
}

const FOLDER_LOADERS: Record<
  string,
  (ctx: FolderLoadContext) => Promise<ExplorerNode[]>
> = {
  tables: loadTables,
  views: loadViews,
  routines: loadRoutines,
  triggers: loadTriggers,
  columns: loadColumns,
  keys: loadKeys,
  indexes: loadIndexes,
};

export function loadFolderContentsByType(
  ctx: FolderLoadContext,
  folderName: string
): Promise<ExplorerNode[]> {
  const loader = FOLDER_LOADERS[folderName];
  return loader ? loader(ctx) : Promise.resolve([]);
}
