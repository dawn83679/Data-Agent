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

/**
 * Table data tab metadata
 * Used for table/view data console tabs
 */
export interface TableDataTabMetadata {
  connectionId: number;
  connectionName: string;
  databaseName: string | null;
  schemaName: string | null;
  objectName: string;
  objectType: 'table' | 'view';
  catalog?: string;
  highlightColumn?: string;
  currentPage?: number;
  pageSize?: number;
  whereClause?: string;
  orderBy?: string;
}

export type TabMetadata = ConsoleTabMetadata | TableDataTabMetadata;
