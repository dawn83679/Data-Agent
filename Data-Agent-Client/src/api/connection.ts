/**
 * Connection management API helpers.
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
 * Test a connection without persisting it.
 */
export function testConnection(request: ConnectRequest) {
  return post<ConnectionTestResponse>('/api/connections/test', request)
}

/**
 * Open a reusable connection session.
 */
export function openConnection(request: ConnectRequest) {
  return post<OpenConnectionResponse>('/api/connections/open', request)
}

/**
 * Create a saved connection profile.
 */
export function createConnection(request: ConnectionCreateRequest) {
  return post<ConnectionResponse>('/api/connections/create', request)
}

/**
 * Fetch every saved connection profile.
 */
export function getConnections() {
  return get<ConnectionResponse[]>('/api/connections')
}

/**
 * Fetch a single connection profile by id.
 */
export function getConnection(id: number) {
  return get<ConnectionResponse>(`/api/connections/${id}`)
}

/**
 * Update an existing connection profile.
 */
export function updateConnection(id: number, request: ConnectionCreateRequest) {
  return put<ConnectionResponse>(`/api/connections/${id}`, request)
}

/**
 * Remove a connection profile.
 */
export function deleteConnection(id: number) {
  return del<void>(`/api/connections/${id}`)
}

/**
 * Close an active connection session.
 */
export function closeConnection(connectionId: string) {
  return del<void>(`/api/connections/active/${connectionId}`)
}

