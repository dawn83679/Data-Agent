/**
 * 认证相关的API接口
 */

import { post, get } from '@/utils/request'
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  TokenResponse,
  RefreshTokenRequest,
  OAuthLoginRequest,
  UserInfo,
} from '@/types/auth'

/**
 * 用户登录
 */
export function login(request: LoginRequest) {
  return post<LoginResponse>('/api/auth/login', request)
}

/**
 * 用户注册
 */
export function register(request: RegisterRequest) {
  return post<LoginResponse>('/api/auth/register', request)
}

/**
 * 刷新Token
 */
export function refreshToken(request: RefreshTokenRequest) {
  return post<TokenResponse>('/api/auth/refresh', request)
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return get<UserInfo>('/api/auth/me')
}

/**
 * 用户登出
 */
export function logout() {
  return post<void>('/api/auth/logout')
}

/**
 * Google OAuth登录
 */
export function loginWithGoogle(request: OAuthLoginRequest) {
  return post<LoginResponse>('/api/auth/oauth/google', request)
}

/**
 * GitHub OAuth登录
 */
export function loginWithGitHub(request: OAuthLoginRequest) {
  return post<LoginResponse>('/api/auth/oauth/github', request)
}

/**
 * 获取OAuth授权URL
 */
export function getOAuthUrl(provider: 'google' | 'github') {
  return get<{ url: string }>(`/api/auth/oauth/${provider}/url`)
}

