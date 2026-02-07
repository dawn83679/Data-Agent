export interface ConnectRequest {
  dbType: string;
  host: string;
  port: number;
  database?: string;
  username: string;
  password?: string;
  driverJarPath: string;
  timeout?: number;
  properties?: Record<string, string>;
}

export interface ConnectionCreateRequest {
  connectionId?: number;
  name: string;
  dbType: string;
  host: string;
  port: number;
  database?: string;
  username?: string;
  password?: string;
  driverJarPath: string;
  timeout?: number;
  properties?: Record<string, string>;
}

export interface DbConnection {
  id: number;
  name: string;
  dbType: string;
  host: string;
  port: number;
  database?: string;
  username?: string;
  driverJarPath: string;
  timeout?: number;
  properties?: Record<string, string>;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConnectionTestResponse {
  status: 'SUCCEEDED' | 'FAILED';
  dbmsInfo: string;
  driverInfo: string;
  ping: number;
}

/** @deprecated Use ConnectionCreateRequest */
export type CreateConnectionRequest = ConnectionCreateRequest;

export type TestConnectionRequest = ConnectRequest;
