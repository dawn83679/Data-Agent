/**
 * Shared API response typings.
 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

