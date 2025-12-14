import { ref } from 'vue'
import { chatApi } from '@/api/chat'

/**
 * AI助手组合式函数
 */
export function useAI() {
  const aiMessages = ref<any[]>([])
  const aiAgent = ref('Agent')
  const aiModel = ref('Gemini 3 Pro')

  /**
   * 发送AI消息
   */
  const handleAISendMessage = async (message: string, conversationId?: number) => {
    try {
      // 这里需要根据实际的后端API调整
      // 目前使用chatApi.sendMessage，但需要确认是否支持SSE
      const response = await chatApi.sendMessage({
        conversationId: conversationId || 0,
        message: message,
        agent: aiAgent.value,
        model: aiModel.value
      })

      // 处理响应
      aiMessages.value.push({
        role: 'user',
        content: message
      })
      
      aiMessages.value.push({
        role: 'assistant',
        content: response.data?.message || '响应处理中...'
      })
    } catch (error) {
      console.error('Failed to send AI message:', error)
    }
  }

  return {
    aiMessages,
    aiAgent,
    aiModel,
    handleAISendMessage
  }
}

