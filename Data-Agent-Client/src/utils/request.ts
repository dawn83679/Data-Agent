/**
 * HTTP request utilities that wrap fetch behind a shared interface.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

// Token存储键名
const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 获取存储的Access Token
 */
export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

/**
 * 获取存储的Refresh Token
 */
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

/**
 * 设置Token
 */
export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

/**
 * 清除Token
 */
export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

/**
 * 刷新Token
 */
let isRefreshing = false
let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  if (isRefreshing && refreshPromise) {
    return refreshPromise
  }

  isRefreshing = true
  refreshPromise = (async () => {
    try {
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        throw new Error('No refresh token available')
      }

      const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
      })

      if (!response.ok) {
        throw new Error('Token refresh failed')
      }

      const data: ApiResponse<{ accessToken: string; refreshToken: string }> = await response.json()
      
      if (data.code !== 200) {
        throw new Error(data.message || 'Token refresh failed')
      }

      setTokens(data.data.accessToken, data.data.refreshToken)
      return data.data.accessToken
    } finally {
      isRefreshing = false
      refreshPromise = null
    }
  })()

  return refreshPromise
}

/**
 * Dispatch an HTTP request and return the parsed payload.
 */
async function request<T>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const defaultHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  // 添加Access Token到请求头
  const accessToken = getAccessToken()
  if (accessToken) {
    defaultHeaders['Authorization'] = `Bearer ${accessToken}`
  }

  let response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...(options.headers as Record<string, string>),
    },
  })

  // 如果返回401，尝试刷新Token
  if (response.status === 401 && accessToken && url !== '/api/auth/refresh') {
    try {
      const newAccessToken = await refreshAccessToken()
      // 使用新Token重试请求
      const retryHeaders: Record<string, string> = {
        ...defaultHeaders,
        'Authorization': `Bearer ${newAccessToken}`,
        ...(options.headers as Record<string, string>),
      }
      response = await fetch(`${API_BASE_URL}${url}`, {
        ...options,
        headers: retryHeaders,
      })
    } catch (error) {
      // 刷新失败，清除Token并跳转到登录页
      clearTokens()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      throw error
    }
  }

  // 尝试解析响应体（无论HTTP状态码如何，后端可能返回JSON格式的错误）
  let data: ApiResponse<T>
  try {
    data = await response.json()
  } catch (e) {
    // 如果响应不是JSON格式，根据HTTP状态码抛出错误
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    throw new Error('响应格式错误')
  }
  
  // 检查业务状态码（后端统一使用ApiResponse格式，即使HTTP状态码是200也可能业务失败）
  if (data.code !== 200) {
    throw new Error(data.message || '请求失败')
  }

  // 如果HTTP状态码不是200但业务状态码是200，也抛出错误（这种情况不应该发生）
  if (!response.ok) {
    throw new Error(data.message || `HTTP error! status: ${response.status}`)
  }

  return data
}

/**
 * Convenience wrapper for GET requests.
 */
export function get<T>(url: string, params?: Record<string, any>): Promise<ApiResponse<T>> {
  let queryString = ''
  if (params) {
    queryString = '?' + new URLSearchParams(params).toString()
  }
  return request<T>(url + queryString, { method: 'GET' })
}

/**
 * Convenience wrapper for POST requests.
 */
export function post<T>(url: string, data?: any): Promise<ApiResponse<T>> {
  return request<T>(url, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  })
}

/**
 * Convenience wrapper for PUT requests.
 */
export function put<T>(url: string, data?: any): Promise<ApiResponse<T>> {
  return request<T>(url, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
  })
}

/**
 * Convenience wrapper for DELETE requests.
 */
export function del<T>(url: string): Promise<ApiResponse<T>> {
  return request<T>(url, { method: 'DELETE' })
}

