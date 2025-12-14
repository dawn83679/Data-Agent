<template>
  <div class="transaction-menu">
    <div class="px-3 py-2 border-b theme-border">
      <div class="text-xs font-semibold theme-text-primary mb-2">Transaction Mode</div>
      <div class="space-y-1">
        <div
          class="flex items-center px-2 py-1 cursor-pointer hover:bg-white/5 rounded"
          :class="{ 'bg-blue-600/20': transaction.mode === 'auto' }"
          @click="$emit('update:transaction', { mode: 'auto' })"
        >
          <i class="fa-solid fa-check w-4 mr-2" :class="transaction.mode === 'auto' ? 'text-blue-400' : 'theme-text-secondary'"></i>
          <span class="text-xs">Auto</span>
        </div>
        <div
          class="flex items-center px-2 py-1 cursor-pointer hover:bg-white/5 rounded"
          :class="{ 'bg-blue-600/20': transaction.mode === 'manual' }"
          @click="$emit('update:transaction', { mode: 'manual' })"
        >
          <i class="fa-solid fa-check w-4 mr-2" :class="transaction.mode === 'manual' ? 'text-blue-400' : 'theme-text-secondary'"></i>
          <span class="text-xs">Manual</span>
        </div>
      </div>
    </div>
    <div class="px-3 py-2">
      <div class="text-xs font-semibold theme-text-primary mb-2">Isolation Level</div>
      <div class="space-y-1">
        <div
          v-for="(label, key) in isolationLevels"
          :key="key"
          class="flex items-center px-2 py-1 cursor-pointer hover:bg-white/5 rounded"
          :class="{ 'bg-blue-600/20': transaction.isolation === key }"
          @click="$emit('update:transaction', { isolation: key })"
        >
          <i class="fa-solid fa-check w-4 mr-2" :class="transaction.isolation === key ? 'text-blue-400' : 'theme-text-secondary'"></i>
          <span class="text-xs">{{ label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  transaction: {
    mode: 'auto' | 'manual'
    isolation: 'RU' | 'RC' | 'RR' | 'S'
    active: boolean
  }
}>()

defineEmits<{
  'update:transaction': [value: any]
}>()

const isolationLevels = {
  RU: 'Read Uncommitted',
  RC: 'Read Committed',
  RR: 'Repeatable Read',
  S: 'Serializable'
}
</script>

<style scoped>
@import '@/styles/database/theme.css';
</style>

