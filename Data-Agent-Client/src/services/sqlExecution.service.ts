import http from '../lib/http';
import type { ExecuteSqlResponse, ExecuteSqlParams } from '../types/sql';

const SQL_EXECUTE_URL = '/db/sql/execute';

export const sqlExecutionService = {
  executeSql: async (params: ExecuteSqlParams): Promise<ExecuteSqlResponse> => {
    const response = await http.post<ExecuteSqlResponse>(SQL_EXECUTE_URL, {
      connectionId: params.connectionId,
      databaseName: params.databaseName ?? undefined,
      schemaName: params.schemaName ?? undefined,
      sql: params.sql,
    });
    return response.data;
  },
};
