/**
 * API 通用类型定义
 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

