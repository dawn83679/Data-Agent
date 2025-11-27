import request from '@/utils/request'
import type {
  ApiResponse,
  ConnectRequest,
  ConnectionCreateRequest,
  ConnectionResponse,
  ConnectionTestResponse,
  OpenConnectionResponse,
} from '@/types/api'

/**
 * 数据库连接 API
 */
export const connectionApi = {
  /**
   * 测试数据库连接
   * POST /api/connections/test
   */
  testConnection(data: ConnectRequest): Promise<ApiResponse<ConnectionTestResponse>> {
    return request.post('/connections/test', data)
  },

  /**
   * 打开数据库连接
   * POST /api/connections/open
   */
  openConnection(data: ConnectRequest): Promise<ApiResponse<OpenConnectionResponse>> {
    return request.post('/connections/open', data)
  },

  /**
   * 创建数据库连接配置
   * POST /api/connections/create
   */
  createConnection(data: ConnectionCreateRequest): Promise<ApiResponse<ConnectionResponse>> {
    return request.post('/connections/create', data)
  },

  /**
   * 获取所有连接配置
   * GET /api/connections
   */
  getConnections(): Promise<ApiResponse<ConnectionResponse[]>> {
    return request.get('/connections')
  },

  /**
   * 根据 ID 获取连接配置
   * GET /api/connections/{id}
   */
  getConnectionById(id: number): Promise<ApiResponse<ConnectionResponse>> {
    return request.get(`/connections/${id}`)
  },

  /**
   * 更新连接配置
   * PUT /api/connections/{id}
   */
  updateConnection(id: number, data: ConnectionCreateRequest): Promise<ApiResponse<ConnectionResponse>> {
    return request.put(`/connections/${id}`, data)
  },

  /**
   * 删除连接配置
   * DELETE /api/connections/{id}
   */
  deleteConnection(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/connections/${id}`)
  },

  /**
   * 关闭活动连接
   * DELETE /api/connections/active/{connectionId}
   */
  closeConnection(connectionId: string): Promise<ApiResponse<void>> {
    return request.delete(`/connections/active/${connectionId}`)
  },
}

