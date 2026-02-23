import http from '../lib/http';
import { ApiPaths } from '../constants/apiPaths';

export interface TriggerMetadata {
  name: string;
  tableName: string;
  timing: string;
  event: string;
}

export const triggerService = {
  listTriggers: async (
    connectionId: string,
    catalog?: string,
    schema?: string,
    tableName?: string
  ): Promise<TriggerMetadata[]> => {
    const params: Record<string, string> = { connectionId };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;
    if (tableName != null && tableName !== '') params.tableName = tableName;

    const response = await http.get<TriggerMetadata[]>(ApiPaths.TRIGGERS, { params });
    return response.data;
  },

  getTriggerDdl: async (
    connectionId: string,
    triggerName: string,
    catalog?: string,
    schema?: string
  ): Promise<string> => {
    const params: Record<string, string> = { connectionId, triggerName };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<string>(ApiPaths.TRIGGERS_DDL, { params });
    return response.data;
  },

  deleteTrigger: async (
    connectionId: string,
    triggerName: string,
    catalog?: string,
    schema?: string
  ): Promise<void> => {
    await http.delete(ApiPaths.TRIGGERS, {
      data: {
        connectionId,
        triggerName,
        catalog,
        schema
      }
    });
  },
};
