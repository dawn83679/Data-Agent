import request from '@/utils/request'
import type {
  ApiResponse,
  TokenPairResponse,
  LoginRequest,
  EmailCodeLoginRequest,
  GoogleLoginRequest,
  SendVerificationCodeRequest,
  RefreshTokenRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from '@/types/api'

/**
 * 保存后端返回的 Token 到本地
 * - accessToken 存到 localStorage.satoken，供请求拦截器使用
 * - refreshToken 存到 localStorage.refresh_token，后续可用于刷新
 */
export function applyTokenPair(tokenPair: TokenPairResponse) {
  if (!tokenPair) return
  const { accessToken, refreshToken } = tokenPair
  if (accessToken) {
    localStorage.setItem('satoken', accessToken)
  }
  if (refreshToken) {
    localStorage.setItem('refresh_token', refreshToken)
  }
}

/**
 * Google OAuth 相关配置
 * 需要在 .env 中配置：
 * - VITE_GOOGLE_CLIENT_ID
 * - VITE_GOOGLE_REDIRECT_URI（可选，默认使用 /auth/google/callback）
 */
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined
const GOOGLE_REDIRECT_URI_DEFAULT =
  (import.meta.env.VITE_GOOGLE_REDIRECT_URI as string | undefined) ||
  (typeof window !== 'undefined' ? `${window.location.origin}/auth/google/callback` : '')

const GOOGLE_AUTH_BASE_URL = 'https://accounts.google.com/o/oauth2/v2/auth'

/**
 * 构造 Google OAuth 授权 URL（仅构造，不跳转）
 */
export function buildGoogleAuthUrl(options?: { redirectUri?: string; state?: string; scope?: string }) {
  if (!GOOGLE_CLIENT_ID) {
    throw new Error('VITE_GOOGLE_CLIENT_ID 未配置，无法使用 Google 登录')
  }

  const redirectUri = options?.redirectUri || GOOGLE_REDIRECT_URI_DEFAULT
  const scope = options?.scope || 'openid email profile'

  const params = new URLSearchParams({
    client_id: GOOGLE_CLIENT_ID,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope,
    access_type: 'offline',
    prompt: 'consent',
  })

  if (options?.state) {
    params.set('state', options.state)
  }

  return `${GOOGLE_AUTH_BASE_URL}?${params.toString()}`
}

/**
 * 直接跳转到 Google OAuth 登录页面
 */
export function redirectToGoogleLogin(options?: { redirectUri?: string; state?: string; scope?: string }) {
  const url = buildGoogleAuthUrl(options)
  window.location.href = url
}

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 邮箱密码登录
   * POST /api/auth/login
   */
  login(data: LoginRequest): Promise<ApiResponse<TokenPairResponse>> {
    return request.post('/auth/login', data)
  },

  /**
   * 邮箱验证码登录
   * POST /api/auth/login/email-code
   */
  loginByEmailCode(data: EmailCodeLoginRequest): Promise<ApiResponse<TokenPairResponse>> {
    return request.post('/auth/login/email-code', data)
  },

  /**
   * 发送邮箱验证码
   * POST /api/auth/send-code
   * codeType 一般为：LOGIN / REGISTER / RESET_PASSWORD
   */
  sendVerificationCode(data: SendVerificationCodeRequest): Promise<ApiResponse<boolean>> {
    return request.post('/auth/send-code', data)
  },

  /**
   * 用户注册
   * POST /api/auth/register
   */
  register(data: RegisterRequest): Promise<ApiResponse<boolean>> {
    return request.post('/auth/register', data)
  },

  /**
   * 刷新访问令牌
   * POST /api/auth/refresh
   */
  refreshToken(data: RefreshTokenRequest): Promise<ApiResponse<TokenPairResponse>> {
    return request.post('/auth/refresh', data)
  },

  /**
   * 退出登录
   * POST /api/auth/logout
   * 依赖请求头中的 Authorization（由请求拦截器自动添加）
   */
  logout(): Promise<ApiResponse<boolean>> {
    return request.post('/auth/logout')
  },

  /**
   * 重置密码
   * POST /api/auth/reset-password
   */
  resetPassword(data: ResetPasswordRequest): Promise<ApiResponse<boolean>> {
    return request.post('/auth/reset-password', data)
  },

  /**
   * Google OAuth 登录：后端交换 code 为 TokenPairResponse
   * POST /api/auth/google/login
   */
  googleLogin(data: GoogleLoginRequest): Promise<ApiResponse<TokenPairResponse>> {
    return request.post('/auth/google/login', data)
  },
}


