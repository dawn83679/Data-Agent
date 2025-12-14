<template>
  <div class="ai-sidebar h-full flex flex-col">
    <div class="flex items-center justify-between px-3 py-2 theme-bg-panel border-b theme-border">
      <span class="theme-text-primary text-xs font-bold">
        <i class="fa-solid fa-wand-magic-sparkles text-purple-400 mr-2"></i>AI Assistant
      </span>
      <i @click="$emit('close')" class="fa-solid fa-xmark theme-text-secondary cursor-pointer hover:text-blue-500 text-xs"></i>
    </div>
    
    <div class="flex-1 overflow-y-auto p-3 space-y-4 theme-bg-main">
      <div v-for="(msg, index) in messages" :key="index" class="message">
        <div :class="msg.role === 'user' ? 'text-right' : 'text-left'">
          <div :class="[
            'inline-block p-2 rounded text-xs',
            msg.role === 'user' ? 'theme-bg-panel theme-text-primary' : 'theme-bg-hover theme-text-secondary'
          ]">
            {{ msg.content }}
          </div>
        </div>
      </div>
    </div>
    
    <div class="p-2 theme-bg-panel border-t theme-border">
      <textarea
        v-model="inputMessage"
        @keydown.enter.prevent="handleSend"
        placeholder="Ask AI..."
        class="w-full p-2 text-xs theme-bg-main theme-text-primary border theme-border rounded resize-none focus:outline-none focus:border-purple-500"
        rows="3"
      ></textarea>
      <div class="flex items-center justify-between mt-2">
        <div class="flex items-center space-x-2 text-xs theme-text-secondary">
          <span>{{ agent }}</span>
          <span>{{ model }}</span>
        </div>
        <button
          @click="handleSend"
          class="px-3 py-1 text-xs theme-bg-main theme-text-primary border theme-border rounded hover:bg-white/5"
        >
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  messages: any[]
  agent: string
  model: string
}>()

const emit = defineEmits<{
  'send-message': [message: string]
  'close': []
}>()

const inputMessage = ref('')

const handleSend = () => {
  if (inputMessage.value.trim()) {
    emit('send-message', inputMessage.value.trim())
    inputMessage.value = ''
  }
}
</script>

<style scoped>
@import '@/styles/database/theme.css';
</style>

