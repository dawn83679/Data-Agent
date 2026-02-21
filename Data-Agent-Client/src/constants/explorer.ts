/**
 * Constants and enums for Database Explorer.
 */

/** Explorer tree node types. */
export const ExplorerNodeType = {
  ROOT: 'root',
  DB: 'db',
  SCHEMA: 'schema',
  FOLDER: 'folder',
  TABLE: 'table',
  VIEW: 'view',
  FUNCTION: 'function',
  PROCEDURE: 'procedure',
  TRIGGER: 'trigger',
  COLUMN: 'column',
  INDEX: 'index',
  KEY: 'key',
  EMPTY: 'empty',
} as const;

export type ExplorerNodeType = (typeof ExplorerNodeType)[keyof typeof ExplorerNodeType];

/** Virtual folder names for db/schema/table children. */
export const FolderName = {
  TABLES: 'tables',
  VIEWS: 'views',
  ROUTINES: 'routines',
  TRIGGERS: 'triggers',
  COLUMNS: 'columns',
  KEYS: 'keys',
  INDEXES: 'indexes',
} as const;

export type FolderName = (typeof FolderName)[keyof typeof FolderName];

/** i18n keys for explorer. */
export const ExplorerI18nKeys = {
  FOLDER_TABLES: 'explorer.folder_tables',
  FOLDER_VIEWS: 'explorer.folder_views',
  FOLDER_ROUTINES: 'explorer.folder_routines',
  FOLDER_TRIGGERS: 'explorer.folder_triggers',
  FOLDER_COLUMNS: 'explorer.folder_columns',
  FOLDER_KEYS: 'explorer.folder_keys',
  FOLDER_INDEXES: 'explorer.folder_indexes',
  NO_ITEMS: 'explorer.no_items',
} as const;

/** ID prefix for tree node IDs. */
export const ExplorerIdPrefix = {
  CONNECTION: 'conn-',
  DB: '-db-',
  SCHEMA: '-schema-',
  FOLDER: '-folder-',
  TABLE: '-table-',
  VIEW: '-view-',
  FUNCTION: '-function-',
  PROCEDURE: '-procedure-',
  TRIGGER: '-trigger-',
  COLUMN: '-column-',
  KEY: '-key-',
  INDEX: '-index-',
} as const;

/** Tree layout constants. */
export const ExplorerTreeConfig = {
  INDENT: 12,
  ROW_HEIGHT: 28,
  HEIGHT: 800,
} as const;

/** Empty node ID suffix. */
export const ExplorerEmptySuffix = '-empty';

/** Display format for trigger with table. */
export const TRIGGER_ON_TABLE_FORMAT = (triggerName: string, tableName: string) =>
  `${triggerName} (on ${tableName})`;

/** Display suffix for unique index. */
export const INDEX_UNIQUE_SUFFIX = ' UNIQUE';

/** Query key for connections. */
export const QUERY_KEY_CONNECTIONS = ['connections'] as const;

/** DDL viewer constants. */
export const DdlViewerConfig = {
  SYNTAX_LANGUAGE: 'sql' as const,
  COPY_FEEDBACK_MS: 2000,
};

/** Routine icon letter (F=Function, P=Procedure). */
export const RoutineIconLetter = {
  FUNCTION: 'F',
  PROCEDURE: 'P',
} as const;
