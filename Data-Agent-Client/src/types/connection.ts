/**
 * 连接相关类型定义
 */

/**
 * 连接请求
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
 * 创建连接请求
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
 * 连接响应
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
 * 连接测试响应
 */
export interface ConnectionTestResponse {
  success: boolean
  message?: string
  dbmsVersion?: string
  driverVersion?: string
  latency?: number
}

/**
 * 打开连接响应
 */
export interface OpenConnectionResponse {
  connectionId: string
  dbmsVersion?: string
  driverVersion?: string
  latency?: number
}

