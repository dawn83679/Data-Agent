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
    // 重置表单
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
  margin-top: 20px;
}

.download-form {
  max-width: 500px;
  margin-top: 10px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: inline-block;
  width: 120px;
  font-weight: bold;
}

.form-group select,
.form-group input {
  width: 200px;
  padding: 5px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.btn-download {
  padding: 8px 20px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-download:hover:not(:disabled) {
  background-color: #45a049;
}

.btn-download:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.error {
  color: #f44336;
  margin-top: 10px;
}

.success {
  color: #4caf50;
  margin-top: 10px;
}
</style>

