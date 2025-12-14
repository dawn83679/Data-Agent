<template>
  <div
    :class="[
      'px-4 py-2 rounded shadow-lg text-xs font-medium animate-fade-in',
      typeClasses[type]
    ]"
  >
    <div class="flex items-center space-x-2">
      <i :class="iconClasses[type]"></i>
      <span>{{ message }}</span>
      <button @click="$emit('close')" class="ml-2 hover:opacity-70">
        <i class="fa-solid fa-xmark text-[10px]"></i>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ToastType } from '@/composables/useToast'

defineProps<{
  type: ToastType
  message: string
}>()

defineEmits<{
  'close': []
}>()

const typeClasses = {
  success: 'bg-green-600 text-white',
  error: 'bg-red-600 text-white',
  warning: 'bg-yellow-600 text-white',
  info: 'bg-blue-600 text-white'
}

const iconClasses = {
  success: 'fa-solid fa-check-circle',
  error: 'fa-solid fa-exclamation-circle',
  warning: 'fa-solid fa-exclamation-triangle',
  info: 'fa-solid fa-info-circle'
}
</script>

<style scoped>
@import '@/styles/database/theme.css';
</style>

