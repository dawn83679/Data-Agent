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
}
