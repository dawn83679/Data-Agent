import http from '../lib/http';
import { ApiPaths } from '../constants/apiPaths';

export interface ParameterInfo {
  name: string;
  dataType: string;
}

export interface FunctionMetadata {
  name: string;
  parameters?: ParameterInfo[];
  returnType?: string | null;
}

export const functionService = {
  listFunctions: async (
    connectionId: string,
    catalog?: string,
    schema?: string
  ): Promise<FunctionMetadata[]> => {
    const params: Record<string, string> = { connectionId };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<FunctionMetadata[]>(ApiPaths.FUNCTIONS, { params });
    return response.data;
  },

  getFunctionDdl: async (
    connectionId: string,
    functionName: string,
    catalog?: string,
    schema?: string
  ): Promise<string> => {
    const params: Record<string, string> = { connectionId, functionName };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<string>(ApiPaths.FUNCTIONS_DDL, { params });
    return response.data;
  },

  deleteFunction: async (
    connectionId: string,
    functionName: string,
    catalog?: string,
    schema?: string
  ): Promise<void> => {
    const params: Record<string, string> = { connectionId, functionName };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    await http.post(`${ApiPaths.FUNCTIONS}/delete`, null, { params });
  },
};
