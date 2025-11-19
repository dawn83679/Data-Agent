<template>
  <Transition name="message">
    <div v-if="visible" :class="['message', `message-${type}`]">
      <span class="message-icon">
        <span v-if="type === 'success'">✓</span>
        <span v-else-if="type === 'error'">✕</span>
        <span v-else-if="type === 'warning'">⚠</span>
        <span v-else>ℹ</span>
      </span>
      <span class="message-text">{{ message }}</span>
      <button v-if="closable" @click="close" class="message-close">×</button>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  message: string
  type?: 'success' | 'error' | 'warning' | 'info'
  duration?: number
  closable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  duration: 3000,
  closable: true
})

const emit = defineEmits<{
  close: []
}>()

const visible = ref(true)

let timer: ReturnType<typeof setTimeout> | null = null

watch(() => props.message, () => {
  visible.value = true
  if (timer) clearTimeout(timer)
  if (props.duration > 0) {
    timer = setTimeout(() => {
      close()
    }, props.duration)
  }
}, { immediate: true })

function close() {
  visible.value = false
  emit('close')
}
</script>

<style scoped>
.message {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 10000;
  min-width: 300px;
  max-width: 500px;
}

.message-success {
  border-left: 4px solid var(--color-success);
}

.message-error {
  border-left: 4px solid var(--color-danger);
}

.message-warning {
  border-left: 4px solid var(--color-warning);
}

.message-info {
  border-left: 4px solid var(--color-info);
}

.message-icon {
  font-size: 18px;
  font-weight: bold;
}

.message-success .message-icon {
  color: var(--color-success);
}

.message-error .message-icon {
  color: var(--color-danger);
}

.message-warning .message-icon {
  color: var(--color-warning);
}

.message-info .message-icon {
  color: var(--color-info);
}

.message-text {
  flex: 1;
  color: var(--color-text);
}

.message-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--color-text-muted);
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-close:hover {
  color: var(--color-text);
}

.message-enter-active,
.message-leave-active {
  transition: var(--transition);
}

.message-enter-from,
.message-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>

