import { tableService } from '../services/table.service';
import { viewService } from '../services/view.service';
import { functionService } from '../services/function.service';
import { procedureService } from '../services/procedure.service';
import { triggerService } from '../services/trigger.service';
import {
  ExplorerNodeType,
  FolderName,
  ExplorerI18nKeys,
  ExplorerIdPrefix,
  ExplorerEmptySuffix,
  TRIGGER_ON_TABLE_FORMAT,
} from '../constants/explorer';
import type { ExplorerNode } from '../types/explorer';
import type { FunctionMetadata } from '../services/function.service';
import type { ProcedureMetadata } from '../services/procedure.service';
import type { TriggerMetadata } from '../services/trigger.service';
import { formatRoutineSignature, loadFolderContentsByType } from './folderContentLoaders';

export { formatRoutineSignature } from './folderContentLoaders';

const FOLDER_I18N: Record<FolderName, string> = {
  [FolderName.TABLES]: ExplorerI18nKeys.FOLDER_TABLES,
  [FolderName.VIEWS]: ExplorerI18nKeys.FOLDER_VIEWS,
  [FolderName.ROUTINES]: ExplorerI18nKeys.FOLDER_ROUTINES,
  [FolderName.TRIGGERS]: ExplorerI18nKeys.FOLDER_TRIGGERS,
  [FolderName.COLUMNS]: ExplorerI18nKeys.FOLDER_COLUMNS,
  [FolderName.KEYS]: ExplorerI18nKeys.FOLDER_KEYS,
  [FolderName.INDEXES]: ExplorerI18nKeys.FOLDER_INDEXES,
};

export function createFolderNode(
  parentId: string,
  folderName: FolderName,
  connId: string,
  catalog: string,
  schema: string | undefined,
  t: (key: string) => string,
  tableName?: string
): ExplorerNode {
  return {
    id: `${parentId}${ExplorerIdPrefix.FOLDER}${folderName}`,
    name: t(FOLDER_I18N[folderName]),
    type: ExplorerNodeType.FOLDER,
    folderName,
    connectionId: connId,
    catalog,
    schema,
    tableName,
    children: [],
  };
}

export function createFolderWithChildren(
  parentId: string,
  folderName: FolderName,
  connId: string,
  catalog: string,
  schema: string | undefined,
  t: (key: string) => string,
  children: ExplorerNode[],
  tableName?: string
): ExplorerNode {
  const base = createFolderNode(parentId, folderName, connId, catalog, schema, t, tableName);
  return { ...base, children };
}

export function toChildrenOrEmpty(
  children: ExplorerNode[],
  parentId: string,
  t: (key: string) => string
): ExplorerNode[] {
  if (children.length === 0) {
    return [{
      id: `${parentId}${ExplorerEmptySuffix}`,
      name: t(ExplorerI18nKeys.NO_ITEMS),
      type: ExplorerNodeType.EMPTY,
      children: undefined,
    }];
  }
  return children;
}

type DbSchemaContext = {
  connId: string;
  parentId: string;
  catalog: string;
  schema: string | undefined;
  t: (key: string) => string;
};

function folderId(ctx: DbSchemaContext, fn: FolderName) {
  return `${ctx.parentId}${ExplorerIdPrefix.FOLDER}${fn}`;
}

function toTableNodes(ctx: DbSchemaContext, names: string[]): ExplorerNode[] {
  const prefix = folderId(ctx, FolderName.TABLES);
  return names.map((name) => ({
    id: `${prefix}${ExplorerIdPrefix.TABLE}${name}`,
    name,
    type: ExplorerNodeType.TABLE,
    connectionId: ctx.connId,
    catalog: ctx.catalog,
    schema: ctx.schema,
    children: [],
  }));
}

function toViewNodes(ctx: DbSchemaContext, names: string[]): ExplorerNode[] {
  const prefix = folderId(ctx, FolderName.VIEWS);
  return names.map((name) => ({
    id: `${prefix}${ExplorerIdPrefix.VIEW}${name}`,
    name,
    type: ExplorerNodeType.VIEW,
    connectionId: ctx.connId,
    catalog: ctx.catalog,
    schema: ctx.schema,
    children: [],
  }));
}

function toRoutineNodes(
  ctx: DbSchemaContext,
  functions: FunctionMetadata[],
  procedures: ProcedureMetadata[]
): ExplorerNode[] {
  const prefix = folderId(ctx, FolderName.ROUTINES);
  return [
    ...functions.map((fn) => ({
      id: `${prefix}${ExplorerIdPrefix.FUNCTION}${fn.name}`,
      name: fn.name,
      type: ExplorerNodeType.FUNCTION,
      connectionId: ctx.connId,
      catalog: ctx.catalog,
      schema: ctx.schema,
      signature: formatRoutineSignature(fn.parameters, fn.returnType),
    })),
    ...procedures.map((proc) => ({
      id: `${prefix}${ExplorerIdPrefix.PROCEDURE}${proc.name}`,
      name: proc.name,
      type: ExplorerNodeType.PROCEDURE,
      connectionId: ctx.connId,
      catalog: ctx.catalog,
      schema: ctx.schema,
      signature: formatRoutineSignature(proc.parameters),
    })),
  ];
}

function toTriggerNodes(ctx: DbSchemaContext, triggers: TriggerMetadata[]): ExplorerNode[] {
  const prefix = folderId(ctx, FolderName.TRIGGERS);
  return triggers.map((tr) => ({
    id: `${prefix}${ExplorerIdPrefix.TRIGGER}${tr.name}`,
    name: tr.tableName ? TRIGGER_ON_TABLE_FORMAT(tr.name, tr.tableName) : tr.name,
    type: ExplorerNodeType.TRIGGER,
    connectionId: ctx.connId,
    catalog: ctx.catalog,
    schema: ctx.schema,
    objectName: tr.name,
  }));
}

/** Load all object folders for DB/Schema. Only returns non-empty folders. */
export async function loadDbSchemaFolders(ctx: DbSchemaContext): Promise<ExplorerNode[]> {
  const [tables, views, functions, procedures, triggers] = await Promise.all([
    tableService.listTables(ctx.connId, ctx.catalog, ctx.schema),
    viewService.listViews(ctx.connId, ctx.catalog, ctx.schema),
    functionService.listFunctions(ctx.connId, ctx.catalog, ctx.schema),
    procedureService.listProcedures(ctx.connId, ctx.catalog, ctx.schema),
    triggerService.listTriggers(ctx.connId, ctx.catalog, ctx.schema),
  ]);

  const folders: ExplorerNode[] = [];
  if (tables.length > 0) {
    folders.push(createFolderWithChildren(ctx.parentId, FolderName.TABLES, ctx.connId, ctx.catalog, ctx.schema, ctx.t, toTableNodes(ctx, tables)));
  }
  if (views.length > 0) {
    folders.push(createFolderWithChildren(ctx.parentId, FolderName.VIEWS, ctx.connId, ctx.catalog, ctx.schema, ctx.t, toViewNodes(ctx, views)));
  }
  if (functions.length > 0 || procedures.length > 0) {
    folders.push(createFolderWithChildren(ctx.parentId, FolderName.ROUTINES, ctx.connId, ctx.catalog, ctx.schema, ctx.t, toRoutineNodes(ctx, functions, procedures)));
  }
  if (triggers.length > 0) {
    folders.push(createFolderWithChildren(ctx.parentId, FolderName.TRIGGERS, ctx.connId, ctx.catalog, ctx.schema, ctx.t, toTriggerNodes(ctx, triggers)));
  }
  return folders;
}

/** Load folder contents (lazy load when expanding a pre-created folder). */
export async function loadFolderContents(
  ctx: DbSchemaContext & { folderId: string; tableName?: string },
  folderName: FolderName
): Promise<ExplorerNode[]> {
  return loadFolderContentsByType(ctx, folderName);
}
