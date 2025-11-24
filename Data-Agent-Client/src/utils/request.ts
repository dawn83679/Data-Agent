import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import type { ApiResponse } from '@/types/api'

/**
 * API 基础 URL
 * 可以通过环境变量配置，默认使用代理
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 创建 axios 实例
 */
const request: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 请求拦截器
 */
request.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token（Sa-Token 使用 Bearer token）
    const token = localStorage.getItem('satoken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 如果 code 为 0，表示成功
    if (res.code === 0) {
      return res
    }

    // 处理业务错误
    const errorMessage = res.message || '请求失败'
    console.error('API Error:', errorMessage, res)
    
    // 处理特定的错误码
    if (res.code === 401 || res.code === 40101) {
      // Token 过期或未登录，清除本地存储
      localStorage.removeItem('satoken')
      // 可以在这里触发路由跳转到登录页
      // router.push('/login')
    }

    return Promise.reject(new Error(errorMessage))
  },
  (error: AxiosError) => {
    // 处理 HTTP 错误
    let errorMessage = '网络错误，请稍后重试'

    if (error.response) {
      // 服务器返回了错误状态码
      const status = error.response.status
      switch (status) {
        case 400:
          errorMessage = '请求参数错误'
          break
        case 401:
          errorMessage = '未授权，请重新登录'
          localStorage.removeItem('satoken')
          break
        case 403:
          errorMessage = '拒绝访问'
          break
        case 404:
          errorMessage = '请求的资源不存在'
          break
        case 500:
          errorMessage = '服务器内部错误'
          break
        default:
          errorMessage = `请求失败 (${status})`
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      errorMessage = '网络连接失败，请检查网络'
    }

    console.error('Request Error:', errorMessage, error)
    return Promise.reject(new Error(errorMessage))
  }
)

export default request

