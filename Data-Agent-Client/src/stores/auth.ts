/**
 * 认证相关的Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as loginApi,
  register as registerApi,
  logout as logoutApi,
  getCurrentUser,
  loginWithGoogle,
  loginWithGitHub,
  getOAuthUrl,
  refreshToken,
} from '@/api/auth'
import { setTokens, clearTokens, getAccessToken } from '@/utils/request'
import type {
  LoginRequest,
  RegisterRequest,
  UserInfo,
  TokenResponse,
  OAuthLoginRequest,
} from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<UserInfo | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  const isAuthenticated = computed(() => {
    return !!user.value && !!getAccessToken()
  })

  /**
   * 用户登录
   */
  async function login(request: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await loginApi(request)
      setTokens(response.data.tokens.accessToken, response.data.tokens.refreshToken)
      user.value = response.data.user
      return response.data
    } catch (err: any) {
      error.value = err.message || '登录失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户注册
   */
  async function register(request: RegisterRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await registerApi(request)
      setTokens(response.data.tokens.accessToken, response.data.tokens.refreshToken)
      user.value = response.data.user
      return response.data
    } catch (err: any) {
      error.value = err.message || '注册失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户登出
   */
  async function logout() {
    loading.value = true
    error.value = null
    try {
      await logoutApi()
    } catch (err: any) {
      console.error('登出失败:', err)
    } finally {
      clearTokens()
      user.value = null
      loading.value = false
    }
  }

  /**
   * 获取当前用户信息
   */
  async function fetchUserInfo() {
    loading.value = true
    error.value = null
    try {
      const response = await getCurrentUser()
      user.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || '获取用户信息失败'
      clearTokens()
      user.value = null
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新Token
   */
  async function refreshAccessToken() {
    try {
      const refreshTokenValue = localStorage.getItem('refresh_token')
      if (!refreshTokenValue) {
        throw new Error('No refresh token')
      }
      const response = await refreshToken({ refreshToken: refreshTokenValue })
      setTokens(response.data.accessToken, response.data.refreshToken)
      return response.data
    } catch (err: any) {
      clearTokens()
      user.value = null
      throw err
    }
  }

  /**
   * Google OAuth登录
   */
  async function loginWithGoogleOAuth(code: string) {
    loading.value = true
    error.value = null
    try {
      const request: OAuthLoginRequest = { code, provider: 'google' }
      const response = await loginWithGoogle(request)
      setTokens(response.data.tokens.accessToken, response.data.tokens.refreshToken)
      user.value = response.data.user
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Google登录失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * GitHub OAuth登录
   */
  async function loginWithGitHubOAuth(code: string) {
    loading.value = true
    error.value = null
    try {
      const request: OAuthLoginRequest = { code, provider: 'github' }
      const response = await loginWithGitHub(request)
      setTokens(response.data.tokens.accessToken, response.data.tokens.refreshToken)
      user.value = response.data.user
      return response.data
    } catch (err: any) {
      error.value = err.message || 'GitHub登录失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取OAuth授权URL
   */
  async function getOAuthAuthUrl(provider: 'google' | 'github'): Promise<string> {
    try {
      const response = await getOAuthUrl(provider)
      return response.data.url
    } catch (err: any) {
      error.value = err.message || '获取OAuth URL失败'
      throw err
    }
  }

  /**
   * 初始化：如果已有token，尝试获取用户信息
   */
  async function init() {
    if (getAccessToken()) {
      try {
        await fetchUserInfo()
      } catch (err) {
        // 如果获取失败，清除token
        clearTokens()
      }
    }
  }

  return {
    // 状态
    user,
    loading,
    error,
    // 计算属性
    isAuthenticated,
    // 方法
    login,
    register,
    logout,
    fetchUserInfo,
    refreshAccessToken,
    loginWithGoogleOAuth,
    loginWithGitHubOAuth,
    getOAuthAuthUrl,
    init,
  }
})

