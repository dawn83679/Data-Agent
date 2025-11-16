/**
 * Connection-related DTO definitions.
 */

/**
 * Connection request payload sent to the backend.
 */
export interface ConnectRequest {
  dbType: string
  host: string
  port: number
  database?: string
  username: string
  password?: string
  driverJarPath: string
  timeout?: number
  properties?: Record<string, string>
}

/**
 * Payload for creating or updating a stored connection profile.
 */
export interface ConnectionCreateRequest {
  name: string
  dbType: string
  host: string
  port: number
  database?: string
  username?: string
  password?: string
  driverJarPath: string
  timeout?: number
  properties?: Record<string, string>
}

/**
 * Response describing a stored connection profile.
 */
export interface ConnectionResponse {
  id: number
  name: string
  dbType: string
  host: string
  port: number
  database?: string
  username?: string
  driverJarPath: string
  timeout?: number
  properties?: Record<string, string>
  createdAt: string
  updatedAt: string
}

/**
 * Result returned by the connection test endpoint.
 */
export interface ConnectionTestResponse {
  success: boolean
  message?: string
  dbmsVersion?: string
  driverVersion?: string
  latency?: number
}

/**
 * Response returned after successfully opening a connection.
 */
export interface OpenConnectionResponse {
  connectionId: string
  dbmsVersion?: string
  driverVersion?: string
  latency?: number
}

