/**
 * 认证相关的类型定义
 */

/**
 * 登录请求
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 注册请求
 */
export interface RegisterRequest {
  username: string
  password: string
  email?: string
  confirmPassword: string
}

/**
 * Token响应
 */
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType?: string
  expiresIn?: number
}

/**
 * 刷新Token请求
 */
export interface RefreshTokenRequest {
  refreshToken: string
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  email?: string
  avatar?: string
  roles?: string[]
}

/**
 * 登录响应
 */
export interface LoginResponse {
  user: UserInfo
  tokens: TokenResponse
}

/**
 * OAuth登录请求（Google/GitHub）
 */
export interface OAuthLoginRequest {
  code: string
  provider: 'google' | 'github'
}

