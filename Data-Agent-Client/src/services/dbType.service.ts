import http from '../lib/http';
import type { DbTypeOption } from '../types/dbType';

/**
 * Fetch supported database types for connection form dropdown.
 */
export const dbTypeService = {
  getSupportedDbTypes: async (): Promise<DbTypeOption[]> => {
    const response = await http.get<DbTypeOption[]>('/db-types');
    return response.data;
  },
};
