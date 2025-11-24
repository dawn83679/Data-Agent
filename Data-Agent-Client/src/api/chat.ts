import request from '@/utils/request'
import type {
  ApiResponse,
  ChatRequest,
  CreateConversationRequest,
  ConversationListRequest,
  ConversationResponse,
  GetConversationRequest,
  UpdateConversationRequest,
  DeleteConversationRequest,
  PageResponse,
} from '@/types/api'

/**
 * AI 对话 API
 */
export const chatApi = {
  /**
   * 发送聊天消息（流式响应 - Server-Sent Events）
   * POST /api/chat/send
   * 
   * 注意：这是一个 SSE 接口，需要使用 EventSource 或 fetch 来处理流式响应
   * 返回的是 Response 对象，不是 ApiResponse
   */
  sendMessage(data: ChatRequest): Promise<Response> {
    const token = localStorage.getItem('satoken')
    const baseURL = import.meta.env.VITE_API_BASE_URL || ''
    const url = baseURL ? `${baseURL}/api/chat/send` : '/api/chat/send'
    
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
    })
  },

  /**
   * 创建对话
   * POST /api/ai/conversation/create
   */
  createConversation(data: CreateConversationRequest): Promise<ApiResponse<ConversationResponse>> {
    return request.post('/ai/conversation/create', data)
  },

  /**
   * 获取对话列表（分页）
   * GET /api/ai/conversation/list
   */
  getConversationList(params: ConversationListRequest): Promise<ApiResponse<PageResponse<ConversationResponse>>> {
    return request.get('/ai/conversation/list', { params })
  },

  /**
   * 获取对话详情
   * POST /api/ai/conversation/get
   */
  getConversationById(data: GetConversationRequest): Promise<ApiResponse<ConversationResponse>> {
    return request.post('/ai/conversation/get', data)
  },

  /**
   * 更新对话
   * PUT /api/ai/conversation/update
   */
  updateConversation(data: UpdateConversationRequest): Promise<ApiResponse<ConversationResponse>> {
    return request.put('/ai/conversation/update', data)
  },

  /**
   * 删除对话（软删除）
   * DELETE /api/ai/conversation/delete
   */
  deleteConversation(data: DeleteConversationRequest): Promise<ApiResponse<void>> {
    return request.delete('/ai/conversation/delete', { data })
  },
}

