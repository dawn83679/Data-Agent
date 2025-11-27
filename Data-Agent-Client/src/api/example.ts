/**
 * API 使用示例
 * 这些示例可以直接在 Vue 组件中使用
 */

import { connectionApi, driverApi, chatApi } from '@/api'
import { createSSEConnection } from '@/utils/sse'

/**
 * 示例 1: 测试数据库连接
 */
export async function exampleTestConnection() {
  try {
    const result = await connectionApi.testConnection({
      dbType: 'MYSQL',
      host: 'localhost',
      port: 3306,
      database: 'test',
      username: 'root',
      password: 'password',
      driverJarPath: '/path/to/mysql-connector.jar',
      timeout: 30,
    })
    console.log('连接测试成功:', result.data)
    return result.data
  } catch (error: any) {
    console.error('连接测试失败:', error.message)
    throw error
  }
}

/**
 * 示例 2: 获取所有连接配置
 */
export async function exampleGetConnections() {
  try {
    const result = await connectionApi.getConnections()
    console.log('连接列表:', result.data)
    return result.data
  } catch (error: any) {
    console.error('获取连接列表失败:', error.message)
    throw error
  }
}

/**
 * 示例 3: 获取可用驱动列表
 */
export async function exampleGetAvailableDrivers() {
  try {
    const result = await driverApi.getAvailableDrivers('MySQL')
    console.log('可用驱动:', result.data)
    return result.data
  } catch (error: any) {
    console.error('获取可用驱动失败:', error.message)
    throw error
  }
}

/**
 * 示例 4: 获取已安装驱动列表
 */
export async function exampleGetInstalledDrivers() {
  try {
    const result = await driverApi.getInstalledDrivers('MySQL')
    console.log('已安装驱动:', result.data)
    return result.data
  } catch (error: any) {
    console.error('获取已安装驱动失败:', error.message)
    throw error
  }
}

/**
 * 示例 5: 创建对话
 */
export async function exampleCreateConversation() {
  try {
    const result = await chatApi.createConversation({
      title: '测试对话',
    })
    console.log('对话创建成功:', result.data)
    return result.data
  } catch (error: any) {
    console.error('创建对话失败:', error.message)
    throw error
  }
}

/**
 * 示例 6: 获取对话列表
 */
export async function exampleGetConversationList() {
  try {
    const result = await chatApi.getConversationList({
      current: 1,
      size: 10,
    })
    console.log('对话列表:', result.data.records)
    return result.data
  } catch (error: any) {
    console.error('获取对话列表失败:', error.message)
    throw error
  }
}

/**
 * 示例 7: 发送聊天消息（流式响应）
 */
export function exampleSendChatMessage(conversationId?: number) {
  const closeConnection = createSSEConnection('/api/chat/send', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${localStorage.getItem('satoken')}`,
    },
    body: {
      conversationId,
      message: '你好，请介绍一下你自己',
      model: 'qwen3-coder-plus',
    },
    onMessage: (message) => {
      console.log('收到消息:', message)
      // 在这里处理接收到的消息
      // message.type: 消息类型（如 'text', 'tool_call' 等）
      // message.data: 消息数据
    },
    onError: (error) => {
      console.error('聊天错误:', error)
    },
    onOpen: () => {
      console.log('聊天连接已建立')
    },
    onClose: () => {
      console.log('聊天连接已关闭')
    },
  })

  // 返回关闭函数，可以在需要时调用 closeConnection() 来关闭连接
  return closeConnection
}

/**
 * 在 Vue 组件中使用示例：
 * 
 * <script setup lang="ts">
 * import { onMounted } from 'vue'
 * import { exampleGetConnections, exampleSendChatMessage } from '@/api/example'
 * 
 * onMounted(async () => {
 *   // 测试获取连接列表
 *   try {
 *     const connections = await exampleGetConnections()
 *     console.log('连接数量:', connections.length)
 *   } catch (error) {
 *     console.error('操作失败:', error)
 *   }
 * })
 * 
 * // 发送聊天消息
 * function handleSendMessage() {
 *   const closeConnection = exampleSendChatMessage(1)
 *   // 5秒后自动关闭连接（示例）
 *   setTimeout(() => {
 *     closeConnection()
 *   }, 5000)
 * }
 * </script>
 */

