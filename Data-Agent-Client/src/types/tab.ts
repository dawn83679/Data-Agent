/**
 * Console Tab Metadata
 * Stored in Tab.metadata for console/file tabs
 * Tracks the execution context (connection, database, schema)
 */
export interface ConsoleTabMetadata {
  connectionId: number;
  connectionName: string;   // e.g., "MySQL-Dev", "PostgreSQL-Prod"
  databaseName: string | null;
  schemaName: string | null;
  catalog?: string | null;  // MySQL: database name
}

/**
 * Table Data Tab Metadata
 * Extends ConsoleTabMetadata for table/view data tabs
 */
export interface TableTabMetadata extends ConsoleTabMetadata {
  objectName: string;       // table or view name
  objectType: 'table' | 'view';
  catalog?: string;
  schema?: string;
}
