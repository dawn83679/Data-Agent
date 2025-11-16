<template>
  <div class="driver-download">
    <h3>下载驱动</h3>
    <form @submit.prevent="handleDownload" class="download-form">
      <div class="form-group">
        <label for="databaseType">数据库类型：</label>
        <select id="databaseType" v-model="form.databaseType" required>
          <option value="">请选择</option>
          <option value="MySQL">MySQL</option>
          <option value="PostgreSQL">PostgreSQL</option>
          <option value="Oracle">Oracle</option>
        </select>
      </div>
      <div class="form-group">
        <label for="version">版本（可选）：</label>
        <input
          id="version"
          v-model="form.version"
          type="text"
          placeholder="留空则下载最新版本"
        />
      </div>
      <button type="submit" :disabled="loading" class="btn-download">
        {{ loading ? '下载中...' : '下载' }}
      </button>
    </form>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="success" class="success">下载成功！</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface FormData {
  databaseType: string
  version?: string
}

const form = ref<FormData>({
  databaseType: '',
  version: '',
})

const loading = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

const emit = defineEmits<{
  download: [databaseType: string, version?: string]
}>()

async function handleDownload() {
  loading.value = true
  error.value = null
  success.value = false

  try {
    emit('download', form.value.databaseType, form.value.version)
    success.value = true
    // Reset form state so the next download starts clean
    form.value = {
      databaseType: '',
      version: '',
    }
  } catch (err: any) {
    error.value = err.message || '下载失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.driver-download {
  margin-top: var(--spacing-lg);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.driver-download h3 {
  font-size: 20px;
  margin-bottom: var(--spacing-md);
  color: var(--color-text);
}

.download-form {
  max-width: 600px;
  margin-top: var(--spacing-md);
}

.form-group {
  margin-bottom: var(--spacing-md);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.form-group label {
  min-width: 120px;
  font-weight: 500;
  color: var(--color-text);
}

.form-group select,
.form-group input {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  transition: var(--transition);
}

.form-group select:focus,
.form-group input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.btn-download {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-success);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  transition: var(--transition);
  margin-top: var(--spacing-sm);
}

.btn-download:hover:not(:disabled) {
  background: var(--color-success-dark);
}

.btn-download:disabled {
  background: var(--color-text-muted);
  cursor: not-allowed;
}

.error {
  color: var(--color-danger);
  margin-top: var(--spacing-md);
  padding: var(--spacing-sm);
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-md);
}

.success {
  color: var(--color-success);
  margin-top: var(--spacing-md);
  padding: var(--spacing-sm);
  background: rgba(16, 185, 129, 0.1);
  border-radius: var(--radius-md);
}
</style>

