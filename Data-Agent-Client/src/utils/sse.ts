/**
 * Server-Sent Events (SSE) 工具函数
 * 用于处理流式响应，特别是聊天接口
 */

export interface SSEMessage {
  type?: string
  data: any
}

/**
 * 解析 SSE 消息
 */
export function parseSSEMessage(message: string): SSEMessage | null {
  const lines = message.split('\n')
  const result: Partial<SSEMessage> = {}

  for (const line of lines) {
    if (!line.trim()) continue
    
    const colonIndex = line.indexOf(':')
    if (colonIndex === -1) continue

    const field = line.slice(0, colonIndex).trim()
    const value = line.slice(colonIndex + 1).trim()

    if (field === 'event') {
      result.type = value
    } else if (field === 'data') {
      try {
        result.data = JSON.parse(value)
      } catch {
        result.data = value
      }
    }
  }

  if (result.data !== undefined) {
    return {
      type: result.type || 'message',
      data: result.data,
    }
  }

  return null
}

/**
 * 创建 SSE 连接并处理流式响应
 * 
 * @param url 请求 URL
 * @param options 配置选项
 * @returns 关闭连接的函数
 */
export function createSSEConnection(
  url: string,
  options: {
    method?: string
    headers?: Record<string, string>
    body?: any
    onMessage?: (message: SSEMessage) => void
    onError?: (error: Error) => void
    onOpen?: () => void
    onClose?: () => void
  }
): () => void {
  const {
    method = 'POST',
    headers = {},
    body,
    onMessage,
    onError,
    onOpen,
    onClose,
  } = options

  let abortController: AbortController | null = null

  const connect = async () => {
    abortController = new AbortController()

    try {
      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...headers,
        },
        body: body ? JSON.stringify(body) : undefined,
        signal: abortController.signal,
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      if (!response.body) {
        throw new Error('Response body is null')
      }

      onOpen?.()

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          onClose?.()
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const chunks = buffer.split('\n\n')
        buffer = chunks.pop() || ''

        for (const chunk of chunks) {
          if (chunk.trim()) {
            const message = parseSSEMessage(chunk)
            if (message && onMessage) {
              onMessage(message)
            }
          }
        }
      }
    } catch (error: any) {
      if (error.name === 'AbortError') {
        // 正常取消，不触发错误
        return
      }
      onError?.(error)
    }
  }

  connect()

  // 返回关闭函数
  return () => {
    abortController?.abort()
  }
}

