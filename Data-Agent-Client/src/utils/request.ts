/**
 * HTTP 请求工具
 * 封装 fetch，提供统一的请求方法
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 发送 HTTP 请求
 */
async function request<T>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const defaultHeaders: HeadersInit = {
    'Content-Type': 'application/json',
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  const data: ApiResponse<T> = await response.json()
  
  if (data.code !== 200) {
    throw new Error(data.message || '请求失败')
  }

  return data
}

/**
 * GET 请求
 */
export function get<T>(url: string, params?: Record<string, any>): Promise<ApiResponse<T>> {
  let queryString = ''
  if (params) {
    queryString = '?' + new URLSearchParams(params).toString()
  }
  return request<T>(url + queryString, { method: 'GET' })
}

/**
 * POST 请求
 */
export function post<T>(url: string, data?: any): Promise<ApiResponse<T>> {
  return request<T>(url, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  })
}

/**
 * PUT 请求
 */
export function put<T>(url: string, data?: any): Promise<ApiResponse<T>> {
  return request<T>(url, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
  })
}

/**
 * DELETE 请求
 */
export function del<T>(url: string): Promise<ApiResponse<T>> {
  return request<T>(url, { method: 'DELETE' })
}

