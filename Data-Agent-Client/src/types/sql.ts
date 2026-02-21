/**
 * Response of executing a single SQL statement (matches backend ExecuteSqlResponse).
 */
export interface ExecuteSqlResponse {
  success: boolean;
  errorMessage?: string | null;
  executionTimeMs: number;
  query: boolean;
  headers?: string[] | null;
  rows?: (unknown[])[];
  affectedRows: number;
}

export interface ExecuteSqlParams {
  connectionId: number;
  databaseName?: string | null;
  schemaName?: string | null;
  sql: string;
}
