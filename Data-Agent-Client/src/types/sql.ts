/**
 * SQL Execution Request
 * Sent from frontend to /api/db/sql/execute
 */
export interface ExecuteSqlParams {
  connectionId: number;
  databaseName?: string | null;
  schemaName?: string | null;
  sql: string;
}

/**
 * SQL Execution Response
 * Returned from backend /api/db/sql/execute
 */
export interface ExecuteSqlResponse {
  success: boolean;
  errorMessage?: string | null;
  executionTimeMs: number;
  query: boolean;  // true = SELECT-like, false = DML (INSERT/UPDATE/DELETE)
  headers?: string[] | null;
  rows?: (unknown[])[];
  affectedRows: number;
}
