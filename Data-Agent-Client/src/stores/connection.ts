/**
 * 连接管理 Store
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
  // 状态
  const connections = ref<ConnectionResponse[]>([])
  const currentConnection = ref<ConnectionResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 获取所有连接
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

  // 根据 ID 获取连接
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

  // 创建连接
  async function createConnection(request: ConnectionCreateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.createConnection(request)
      // 创建成功后刷新列表
      await fetchConnections()
      return response.data
    } catch (err: any) {
      error.value = err.message || '创建连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 更新连接
  async function updateConnection(id: number, request: ConnectionCreateRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await connectionApi.updateConnection(id, request)
      // 更新成功后刷新列表
      await fetchConnections()
      return response.data
    } catch (err: any) {
      error.value = err.message || '更新连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 删除连接
  async function removeConnection(id: number) {
    loading.value = true
    error.value = null
    try {
      await connectionApi.deleteConnection(id)
      // 删除成功后刷新列表
      await fetchConnections()
    } catch (err: any) {
      error.value = err.message || '删除连接失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 测试连接
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

  // 打开连接
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

  // 关闭连接
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
    // 状态
    connections,
    currentConnection,
    loading,
    error,
    // 方法
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

