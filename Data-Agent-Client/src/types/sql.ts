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
  originalSql?: string | null;
  executedSql?: string | null;
  databaseName?: string | null;
  schemaName?: string | null;
  query: boolean;  // true = SELECT-like, false = DML (INSERT/UPDATE/DELETE)
  headers?: string[] | null;
  rows?: (unknown[])[];
  affectedRows: number;

  // New structured fields (DataGrip-style)
  type?: ExecuteSqlResponseType | null;
  resultSet?: ExecuteSqlResultSet | null;
  executionInfo?: ExecuteSqlExecutionInfo | null;
  messages?: ExecuteSqlMessage[] | null;
  results?: ExecuteSqlSubResult[] | null;
}

export type ExecuteSqlResponseType = 'QUERY' | 'UPDATE' | 'DDL' | 'ERROR';

export interface ExecuteSqlExecutionInfo {
  executionId?: string | null;
  startTime?: number | null;
  endTime?: number | null;
  durationMs?: number | null;
  executionMs?: number | null;
  fetchingMs?: number | null;
  affectedRows?: number | null;
  fetchRows?: number | null;
  truncated?: boolean | null;
  limitApplied?: boolean | null;
}

export interface ExecuteSqlColumn {
  name?: string | null;
  label?: string | null;
  typeName?: string | null;
  jdbcType?: number | null;
  precision?: number | null;
  scale?: number | null;
  nullable?: boolean | null;
  tableName?: string | null;
}

export interface ExecuteSqlResultSet {
  columns?: ExecuteSqlColumn[] | null;
  rows?: (unknown[])[];
  fetchRows?: number | null;
  truncated?: boolean | null;
}

export type ExecuteSqlMessageLevel = 'INFO' | 'WARN' | 'ERROR';

export interface ExecuteSqlMessage {
  level?: ExecuteSqlMessageLevel | null;
  code?: string | null;
  sqlState?: string | null;
  message?: string | null;
  detail?: string | null;
}

export interface ExecuteSqlSubResult {
  type?: ExecuteSqlResponseType | null;
  resultSet?: ExecuteSqlResultSet | null;
  headers?: string[] | null;
  rows?: (unknown[])[];
  affectedRows?: number | null;
  messages?: ExecuteSqlMessage[] | null;
}
