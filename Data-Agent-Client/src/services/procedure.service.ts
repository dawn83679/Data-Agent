import http from '../lib/http';
import { ApiPaths } from '../constants/apiPaths';

export interface ParameterInfo {
  name: string;
  dataType: string;
}

export interface ProcedureMetadata {
  name: string;
  parameters?: ParameterInfo[];
}

export const procedureService = {
  listProcedures: async (
    connectionId: string,
    catalog?: string,
    schema?: string
  ): Promise<ProcedureMetadata[]> => {
    const params: Record<string, string> = { connectionId };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<ProcedureMetadata[]>(ApiPaths.PROCEDURES, { params });
    return response.data;
  },

  getProcedureDdl: async (
    connectionId: string,
    procedureName: string,
    catalog?: string,
    schema?: string
  ): Promise<string> => {
    const params: Record<string, string> = { connectionId, procedureName };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<string>(ApiPaths.PROCEDURES_DDL, { params });
    return response.data;
  },

  deleteProcedure: async (
    connectionId: string,
    procedureName: string,
    catalog?: string,
    schema?: string
  ): Promise<void> => {
    const params: Record<string, string> = { connectionId, procedureName };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    await http.post(`${ApiPaths.PROCEDURES}/delete`, null, { params });
  },
};
