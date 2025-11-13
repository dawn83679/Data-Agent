<template>
  <div class="connection-view">
    <h2>连接管理</h2>
    
    <div class="connection-container">
      <div class="action-bar">
        <button @click="showForm = true" class="btn-create">新建连接</button>
        <button @click="handleRefresh" class="btn-refresh">刷新</button>
      </div>
      
      <ConnectionForm
        v-if="showForm"
        :connection="editingConnection"
        :loading="connectionStore.loading"
        :error="connectionStore.error"
        @submit="handleSubmit"
        @cancel="handleCancel"
      />
      
      <ConnectionList
        :connections="connectionStore.connections"
        :loading="connectionStore.loading"
        :error="connectionStore.error"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import ConnectionList from '@/components/connection/ConnectionList.vue'
import ConnectionForm from '@/components/connection/ConnectionForm.vue'
import type { ConnectionResponse, ConnectionCreateRequest } from '@/types/connection'

const connectionStore = useConnectionStore()
const showForm = ref(false)
const editingConnection = ref<ConnectionResponse | null>(null)

async function handleSubmit(data: ConnectionCreateRequest) {
  try {
    if (editingConnection.value) {
      await connectionStore.updateConnection(editingConnection.value.id, data)
      alert('连接更新成功！')
    } else {
      await connectionStore.createConnection(data)
      alert('连接创建成功！')
    }
    showForm.value = false
    editingConnection.value = null
  } catch (err: any) {
    alert('操作失败：' + (err.message || '未知错误'))
  }
}

function handleCancel() {
  showForm.value = false
  editingConnection.value = null
}

function handleEdit(connection: ConnectionResponse) {
  editingConnection.value = connection
  showForm.value = true
}

async function handleDelete(id: number) {
  try {
    await connectionStore.removeConnection(id)
    alert('连接删除成功！')
  } catch (err: any) {
    alert('删除失败：' + (err.message || '未知错误'))
  }
}

async function handleRefresh() {
  await connectionStore.fetchConnections()
}

onMounted(async () => {
  await connectionStore.fetchConnections()
})
</script>

<style scoped>
.connection-view {
  padding: 20px;
}

.connection-container {
  margin-top: 20px;
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.btn-create {
  padding: 8px 20px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-create:hover {
  background-color: #45a049;
}

.btn-refresh {
  padding: 8px 20px;
  background-color: #2196f3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-refresh:hover {
  background-color: #1976d2;
}
</style>

