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
  margin-top: 20px;
}

.connection-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

.connection-table th,
.connection-table td {
  padding: 10px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.connection-table th {
  background-color: #f5f5f5;
  font-weight: bold;
}

.btn-edit {
  padding: 5px 10px;
  background-color: #2196f3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 5px;
}

.btn-edit:hover {
  background-color: #1976d2;
}

.btn-delete {
  padding: 5px 10px;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-delete:hover {
  background-color: #d32f2f;
}

.loading,
.error,
.empty {
  padding: 20px;
  text-align: center;
}

.error {
  color: #f44336;
}
</style>

