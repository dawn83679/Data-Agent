/**
 * 连接管理 API
 */
import { get, post, put, del } from '@/utils/request'
import type {
  ConnectRequest,
  ConnectionCreateRequest,
  ConnectionResponse,
  ConnectionTestResponse,
  OpenConnectionResponse,
} from '@/types/connection'

/**
 * 测试连接
 */
export function testConnection(request: ConnectRequest) {
  return post<ConnectionTestResponse>('/api/connections/test', request)
}

/**
 * 打开连接
 */
export function openConnection(request: ConnectRequest) {
  return post<OpenConnectionResponse>('/api/connections/open', request)
}

/**
 * 创建连接配置
 */
export function createConnection(request: ConnectionCreateRequest) {
  return post<ConnectionResponse>('/api/connections/create', request)
}

/**
 * 获取所有连接配置
 */
export function getConnections() {
  return get<ConnectionResponse[]>('/api/connections')
}

/**
 * 根据 ID 获取连接配置
 */
export function getConnection(id: number) {
  return get<ConnectionResponse>(`/api/connections/${id}`)
}

/**
 * 更新连接配置
 */
export function updateConnection(id: number, request: ConnectionCreateRequest) {
  return put<ConnectionResponse>(`/api/connections/${id}`, request)
}

/**
 * 删除连接配置
 */
export function deleteConnection(id: number) {
  return del<void>(`/api/connections/${id}`)
}

/**
 * 关闭活动连接
 */
export function closeConnection(connectionId: string) {
  return del<void>(`/api/connections/active/${connectionId}`)
}

