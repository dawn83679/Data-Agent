import http from '../lib/http';
import {
  DbConnection,
  ConnectionCreateRequest,
  ConnectRequest,
  ConnectionTestResponse,
} from '../types/connection';

export const connectionService = {
  /**
   * List all connection configs
   */
  getConnections: async (): Promise<DbConnection[]> => {
    const response = await http.get<DbConnection[]>('/connections');
    return response.data;
  },

  /**
   * Get connection config by ID
   */
  getConnectionById: async (id: number): Promise<DbConnection> => {
    const response = await http.get<DbConnection>(`/connections/${id}`);
    return response.data;
  },

  /**
   * Create a new connection config
   */
  createConnection: async (data: ConnectionCreateRequest): Promise<DbConnection> => {
    const response = await http.post<DbConnection>('/connections/create', data);
    return response.data;
  },

  /**
   * Update connection config
   */
  updateConnection: async (data: ConnectionCreateRequest): Promise<DbConnection> => {
    const response = await http.put<DbConnection>('/connections', data);
    return response.data;
  },

  /**
   * Delete connection config
   */
  deleteConnection: async (id: number): Promise<void> => {
    await http.delete(`/connections/${id}`);
  },

  /**
   * Test connection without persisting. Returns detailed test result.
   */
  testConnection: async (data: ConnectRequest): Promise<ConnectionTestResponse> => {
    const response = await http.post<ConnectionTestResponse>('/connections/test', data);
    return response.data;
  },

  /**
   * Open a persistent connection by saved config ID.
   */
  openConnectionById: async (id: number): Promise<boolean> => {
    const response = await http.post<boolean>('/connections/open', null, {
      params: { connectionId: id },
    });
    return response.data;
  },

  /**
   * Close an active connection by its connectionId.
   */
  closeConnection: async (connectionId: number): Promise<void> => {
    await http.delete(`/connections/active/${connectionId}`);
  },
};
