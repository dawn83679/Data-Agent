import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ExecuteSqlResponse } from '../types/sql';

const { postMock } = vi.hoisted(() => ({
  postMock: vi.fn(),
}));

vi.mock('../lib/http', () => ({
  default: {
    post: postMock,
  },
}));

import { sqlExecutionService } from './sqlExecution.service';
import { SqlPaths } from '../constants/apiPaths';

describe('sqlExecutionService', () => {
  beforeEach(() => {
    postMock.mockReset();
  });

  it('maps databaseName and schemaName to catalog and schema for backend execution', async () => {
    const responseData: ExecuteSqlResponse = {
      success: true,
      executionTimeMs: 12,
      query: true,
      affectedRows: 0,
      rows: [['alice']],
      headers: ['name'],
    };
    postMock.mockResolvedValue({ data: responseData });

    const result = await sqlExecutionService.executeSql({
      connectionId: 9,
      databaseName: 'enterprise_gateway_dev',
      schemaName: 'public',
      sql: 'select * from chat2db_user;',
    });

    expect(postMock).toHaveBeenCalledWith(SqlPaths.EXECUTE, {
      connectionId: 9,
      catalog: 'enterprise_gateway_dev',
      schema: 'public',
      sql: 'select * from chat2db_user;',
    });
    expect(result).toBe(responseData);
  });

  it('omits empty database and schema values from the backend payload', async () => {
    const responseData: ExecuteSqlResponse = {
      success: false,
      executionTimeMs: 0,
      query: false,
      affectedRows: 0,
      errorMessage: 'No database selected',
    };
    postMock.mockResolvedValue({ data: responseData });

    await sqlExecutionService.executeSql({
      connectionId: 9,
      databaseName: '',
      schemaName: null,
      sql: 'select * from chat2db_user;',
    });

    expect(postMock).toHaveBeenCalledWith(SqlPaths.EXECUTE, {
      connectionId: 9,
      catalog: undefined,
      schema: undefined,
      sql: 'select * from chat2db_user;',
    });
  });
});
