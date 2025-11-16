<template>
  <div class="connection-form">
    <h3>{{ isEdit ? '编辑连接' : '新建连接' }}</h3>
    <form @submit.prevent="handleSubmit" class="form">
      <div class="form-group">
        <label for="name">连接名称：</label>
        <input id="name" v-model="form.name" type="text" required />
      </div>
      <div class="form-group">
        <label for="dbType">数据库类型：</label>
        <select id="dbType" v-model="form.dbType" required>
          <option value="">请选择</option>
          <option value="MYSQL">MySQL</option>
          <option value="POSTGRESQL">PostgreSQL</option>
          <option value="ORACLE">Oracle</option>
        </select>
      </div>
      <div class="form-group">
        <label for="host">主机地址：</label>
        <input id="host" v-model="form.host" type="text" required />
      </div>
      <div class="form-group">
        <label for="port">端口：</label>
        <input id="port" v-model.number="form.port" type="number" required />
      </div>
      <div class="form-group">
        <label for="database">数据库名：</label>
        <input id="database" v-model="form.database" type="text" />
      </div>
      <div class="form-group">
        <label for="username">用户名：</label>
        <input id="username" v-model="form.username" type="text" />
      </div>
      <div class="form-group">
        <label for="password">密码：</label>
        <input id="password" v-model="form.password" type="password" />
      </div>
      <div class="form-group">
        <label for="driverJarPath">驱动 JAR 路径：</label>
        <input id="driverJarPath" v-model="form.driverJarPath" type="text" required />
      </div>
      <div class="form-group">
        <label for="timeout">超时时间（秒）：</label>
        <input id="timeout" v-model.number="form.timeout" type="number" min="1" max="300" />
      </div>
      <div class="form-actions">
        <button type="submit" :disabled="loading" class="btn-submit">
          {{ loading ? '提交中...' : '提交' }}
        </button>
        <button type="button" @click="handleCancel" class="btn-cancel">取消</button>
      </div>
    </form>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { ConnectionCreateRequest, ConnectionResponse } from '@/types/connection'

interface Props {
  connection?: ConnectionResponse | null
  loading?: boolean
  error?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  connection: null,
  loading: false,
  error: null,
})

const emit = defineEmits<{
  submit: [data: ConnectionCreateRequest]
  cancel: []
}>()

const isEdit = computed(() => !!props.connection)

const form = ref<ConnectionCreateRequest>({
  name: '',
  dbType: '',
  host: '',
  port: 3306,
  database: '',
  username: '',
  password: '',
  driverJarPath: '',
  timeout: 30,
})

watch(
  () => props.connection,
  (newConnection) => {
    if (newConnection) {
      form.value = {
        name: newConnection.name,
        dbType: newConnection.dbType,
        host: newConnection.host,
        port: newConnection.port,
        database: newConnection.database || '',
        username: newConnection.username || '',
        password: '',
        driverJarPath: newConnection.driverJarPath,
        timeout: newConnection.timeout || 30,
      }
    } else {
      form.value = {
        name: '',
        dbType: '',
        host: '',
        port: 3306,
        database: '',
        username: '',
        password: '',
        driverJarPath: '',
        timeout: 30,
      }
    }
  },
  { immediate: true }
)

function handleSubmit() {
  emit('submit', { ...form.value })
}

function handleCancel() {
  emit('cancel')
}
</script>

<style scoped>
.connection-form {
  margin-top: var(--spacing-lg);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
}

.connection-form h3 {
  font-size: 20px;
  margin-bottom: var(--spacing-lg);
  color: var(--color-text);
}

.form {
  max-width: 700px;
  margin-top: var(--spacing-md);
}

.form-group {
  margin-bottom: var(--spacing-md);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.form-group label {
  min-width: 150px;
  font-weight: 500;
  color: var(--color-text);
}

.form-group input,
.form-group select {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  transition: var(--transition);
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-actions {
  margin-top: var(--spacing-xl);
  display: flex;
  gap: var(--spacing-md);
}

.btn-submit {
  padding: var(--spacing-sm) var(--spacing-xl);
  background: var(--color-success);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  transition: var(--transition);
}

.btn-submit:hover:not(:disabled) {
  background: var(--color-success-dark);
}

.btn-submit:disabled {
  background: var(--color-text-muted);
  cursor: not-allowed;
}

.btn-cancel {
  padding: var(--spacing-sm) var(--spacing-xl);
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  transition: var(--transition);
}

.btn-cancel:hover {
  background: var(--color-bg-hover);
}

.error {
  color: var(--color-danger);
  margin-top: var(--spacing-md);
  padding: var(--spacing-sm);
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-md);
}
</style>

