<template>
  <div class="driver-list">
    <h3>已安装驱动</h3>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="drivers.length === 0" class="empty">暂无已安装的驱动</div>
    <table v-else class="driver-table">
      <thead>
        <tr>
          <th>数据库类型</th>
          <th>版本</th>
          <th>文件名</th>
          <th>文件大小</th>
          <th>最后修改时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="driver in drivers" :key="`${driver.databaseType}-${driver.version}`">
          <td>{{ driver.databaseType }}</td>
          <td>{{ driver.version }}</td>
          <td>{{ driver.fileName }}</td>
          <td>{{ formatFileSize(driver.fileSize) }}</td>
          <td>{{ formatDate(driver.lastModified) }}</td>
          <td>
            <button @click="handleDelete(driver.databaseType, driver.version)" class="btn-delete">
              删除
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { InstalledDriverResponse } from '@/types/driver'

interface Props {
  drivers: InstalledDriverResponse[]
  loading?: boolean
  error?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  error: null,
})

const emit = defineEmits<{
  delete: [databaseType: string, version: string]
}>()

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleString('zh-CN')
}

function handleDelete(databaseType: string, version: string) {
  if (confirm(`确定要删除 ${databaseType} ${version} 驱动吗？`)) {
    emit('delete', databaseType, version)
  }
}
</script>

<style scoped>
.driver-list {
  margin-top: var(--spacing-lg);
}

.driver-list h3 {
  font-size: 20px;
  margin-bottom: var(--spacing-md);
  color: var(--color-text);
}

.driver-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: var(--spacing-md);
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.driver-table th,
.driver-table td {
  padding: var(--spacing-md);
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.driver-table th {
  background: var(--color-bg-secondary);
  font-weight: 600;
  color: var(--color-text);
}

.driver-table tbody tr:hover {
  background: var(--color-bg-hover);
}

.btn-delete {
  padding: var(--spacing-xs) var(--spacing-md);
  background: var(--color-danger);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  transition: var(--transition);
}

.btn-delete:hover {
  background: var(--color-danger-dark);
}

.loading,
.error,
.empty {
  padding: var(--spacing-xl);
  text-align: center;
  color: var(--color-text-secondary);
}

.error {
  color: var(--color-danger);
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-md);
}
</style>

