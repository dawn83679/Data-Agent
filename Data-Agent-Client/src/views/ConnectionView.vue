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
  padding: var(--spacing-xl);
  max-width: 1400px;
  margin: 0 auto;
}

.connection-view h2 {
  font-size: 32px;
  margin-bottom: var(--spacing-lg);
  color: var(--color-text);
}

.connection-container {
  margin-top: var(--spacing-lg);
}

.action-bar {
  margin-bottom: var(--spacing-lg);
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.btn-create {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-success);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  transition: var(--transition);
}

.btn-create:hover {
  background: var(--color-success-dark);
}

.btn-refresh {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  transition: var(--transition);
}

.btn-refresh:hover {
  background: var(--color-primary-dark);
}
</style>

