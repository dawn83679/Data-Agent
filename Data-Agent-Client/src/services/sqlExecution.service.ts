import http from '../lib/http';
import type { ExecuteSqlResponse, ExecuteSqlParams } from '../types/sql';
import { SqlPaths } from '../constants/apiPaths';

interface ExecuteSqlApiPayload {
  connectionId: number;
  catalog?: string;
  schema?: string;
  sql: string;
}

function toOptionalScope(value?: string | null): string | undefined {
  return value != null && value !== '' ? value : undefined;
}

export const sqlExecutionService = {
  /**
   * Execute SQL on a specific connection and database
   * @param params - ExecuteSqlParams containing connectionId, database, schema, and SQL
   * @returns ExecuteSqlResponse with results or error
   */
  executeSql: async (params: ExecuteSqlParams): Promise<ExecuteSqlResponse> => {
    const payload: ExecuteSqlApiPayload = {
      connectionId: params.connectionId,
      catalog: toOptionalScope(params.databaseName),
      schema: toOptionalScope(params.schemaName),
      sql: params.sql,
    };
    const response = await http.post<ExecuteSqlResponse>(SqlPaths.EXECUTE, payload);
    return response.data;
  },
};
