/**
 * HTTP request utilities that wrap fetch behind a shared interface.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * Dispatch an HTTP request and return the parsed payload.
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

