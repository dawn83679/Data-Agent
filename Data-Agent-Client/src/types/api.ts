/**
 * API 统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  current: number
  size: number
  total: number
  pages: number
  records: T[]
}

/**
 * 分页请求参数
 */
export interface PageRequest {
  current?: number
  size?: number
}

// ==================== 认证相关 ====================

/**
 * 访问令牌对
 * 对应后端的 TokenPairResponse
 */
export interface TokenPairResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

/**
 * 邮箱密码登录请求
 * 对应 LoginRequest
 */
export interface LoginRequest {
  email: string
  password: string
  rememberMe?: boolean
}

/**
 * 邮箱验证码登录请求
 * 对应 EmailCodeLoginRequest
 */
export interface EmailCodeLoginRequest {
  email: string
  code: string
}

/**
 * Google 登录请求
 * 对应 GoogleLoginRequest
 */
export interface GoogleLoginRequest {
  code: string
  redirectUri?: string
}

/**
 * 发送验证码请求
 * 对应 SendVerificationCodeRequest
 */
export interface SendVerificationCodeRequest {
  email: string
  codeType: string
}

/**
 * 刷新令牌请求
 * 对应 RefreshTokenRequest
 */
export interface RefreshTokenRequest {
  refreshToken: string
}

/**
 * 注册请求
 * 对应 RegisterRequest
 */
export interface RegisterRequest {
  email: string
  password: string
  username: string
}

/**
 * 重置密码请求
 * 对应 ResetPasswordRequest
 */
export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

// ==================== 数据库连接相关 ====================

/**
 * 连接测试/打开连接请求
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
 * 连接测试状态
 */
export type ConnectionTestStatus = 'SUCCESS' | 'FAILED' | 'TIMEOUT'

/**
 * 连接测试响应
 */
export interface ConnectionTestResponse {
  status: ConnectionTestStatus
  dbmsInfo?: string
  driverInfo?: string
  ping?: number
}

/**
 * 打开连接响应
 */
export interface OpenConnectionResponse {
  connectionId: string
  dbType: string
  host: string
  port: number
  database?: string
  username?: string
  connected: boolean
  createdAt: string
}

// ==================== 驱动管理相关 ====================

/**
 * 下载驱动请求
 */
export interface DownloadDriverRequest {
  databaseType: string
  version?: string
}

/**
 * 可用驱动响应
 */
export interface AvailableDriverResponse {
  databaseType: string
  version: string
  installed?: boolean
  groupId?: string
  artifactId?: string
  mavenCoordinates?: string
}

/**
 * 已安装驱动响应
 */
export interface InstalledDriverResponse {
  databaseType: string
  fileName: string
  version: string
  filePath: string
  fileSize?: number
  lastModified?: string
}

/**
 * 下载驱动响应
 */
export interface DownloadDriverResponse {
  driverPath: string
  databaseType: string
  fileName: string
  version: string
}

// ==================== AI 对话相关 ====================

/**
 * 聊天请求
 */
export interface ChatRequest {
  conversationId?: number
  message: string
  model?: string
}

/**
 * 创建对话请求
 */
export interface CreateConversationRequest {
  title?: string
}

/**
 * 对话列表请求
 */
export interface ConversationListRequest extends PageRequest {
  title?: string
}

/**
 * 对话响应
 */
export interface ConversationResponse {
  id: number
  userId: number
  title?: string
  tokenCount: number
  createdAt: string
  updatedAt: string
}

/**
 * 获取对话请求
 */
export interface GetConversationRequest {
  id: number
}

/**
 * 更新对话请求
 */
export interface UpdateConversationRequest {
  id: number
  title?: string
}

/**
 * 删除对话请求
 */
export interface DeleteConversationRequest {
  id: number
}

