/**
 * Pinia store that orchestrates connection profiles and sessions.
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as connectionApi from '@/api/connection'
import type {
  ConnectionResponse,
  ConnectionCreateRequest,
  ConnectRequest,
} from '@/types/connection'

export const useConnectionStore = defineStore('connection', () => {
  // Reactive state for connection data
  const connections = ref<ConnectionResponse[]>([])
  const currentConnection = ref<ConnectionResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Fetch every saved connection profile
  async function fetchConnections() {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.getConnections()
      connections.value = response.data
    } catch (err: any) {
      error.value = err.message || '获取连接列表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Fetch a single connection profile by id
  async function fetchConnection(id: number) {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.getConnection(id)
      currentConnection.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || '获取连接详情失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Create and persist a new connection profile
  async function createConnection(request: ConnectionCreateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.createConnection(request)
      // Refresh list after creation so cached state stays current
      await fetchConnections()
      return response.data
    } catch (err: any) {
      error.value = err.message || '创建连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Update an existing connection profile
  async function updateConnection(id: number, request: ConnectionCreateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.updateConnection(id, request)
      // Refresh list after updates to keep state in sync
      await fetchConnections()
      return response.data
    } catch (err: any) {
      error.value = err.message || '更新连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Remove a saved connection profile
  async function removeConnection(id: number) {
    loading.value = true
    error.value = null
    try {
      await connectionApi.deleteConnection(id)
      // Refresh list once the profile is deleted
      await fetchConnections()
    } catch (err: any) {
      error.value = err.message || '删除连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Test connectivity without creating a persistent session
  async function testConnection(request: ConnectRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.testConnection(request)
      return response.data
    } catch (err: any) {
      error.value = err.message || '测试连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Open a persistent connection session
  async function openConnection(request: ConnectRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.openConnection(request)
      return response.data
    } catch (err: any) {
      error.value = err.message || '打开连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Close an existing persistent session
  async function closeConnection(connectionId: string) {
    loading.value = true
    error.value = null
    try {
      await connectionApi.closeConnection(connectionId)
    } catch (err: any) {
      error.value = err.message || '关闭连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    // Expose state
    connections,
    currentConnection,
    loading,
    error,
    // Expose actions
    fetchConnections,
    fetchConnection,
    createConnection,
    updateConnection,
    removeConnection,
    testConnection,
    openConnection,
    closeConnection,
  }
})

