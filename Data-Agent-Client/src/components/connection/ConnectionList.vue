<template>
  <div class="connection-list">
    <h3>连接列表</h3>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="connections.length === 0" class="empty">暂无连接配置</div>
    <table v-else class="connection-table">
      <thead>
        <tr>
          <th>名称</th>
          <th>数据库类型</th>
          <th>主机</th>
          <th>端口</th>
          <th>数据库名</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="connection in connections" :key="connection.id">
          <td>{{ connection.name }}</td>
          <td>{{ connection.dbType }}</td>
          <td>{{ connection.host }}</td>
          <td>{{ connection.port }}</td>
          <td>{{ connection.database || '-' }}</td>
          <td>{{ formatDate(connection.createdAt) }}</td>
          <td>
            <button @click="handleEdit(connection)" class="btn-edit">编辑</button>
            <button @click="handleDelete(connection.id)" class="btn-delete">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { ConnectionResponse } from '@/types/connection'

interface Props {
  connections: ConnectionResponse[]
  loading?: boolean
  error?: string | null
}

withDefaults(defineProps<Props>(), {
  loading: false,
  error: null,
})

const emit = defineEmits<{
  edit: [connection: ConnectionResponse]
  delete: [id: number]
}>()

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleString('zh-CN')
}

function handleEdit(connection: ConnectionResponse) {
  emit('edit', connection)
}

function handleDelete(id: number) {
  if (confirm('确定要删除这个连接配置吗？')) {
    emit('delete', id)
  }
}
</script>

<style scoped>
.connection-list {
  margin-top: var(--spacing-lg);
}

.connection-list h3 {
  font-size: 20px;
  margin-bottom: var(--spacing-md);
  color: var(--color-text);
}

.connection-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: var(--spacing-md);
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.connection-table th,
.connection-table td {
  padding: var(--spacing-md);
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.connection-table th {
  background: var(--color-bg-secondary);
  font-weight: 600;
  color: var(--color-text);
}

.connection-table tbody tr:hover {
  background: var(--color-bg-hover);
}

.btn-edit {
  padding: var(--spacing-xs) var(--spacing-md);
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  margin-right: var(--spacing-xs);
  font-size: 14px;
  transition: var(--transition);
}

.btn-edit:hover {
  background: var(--color-primary-dark);
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

