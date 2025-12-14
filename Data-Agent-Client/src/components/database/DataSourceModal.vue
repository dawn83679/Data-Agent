<template>
  <div class="fixed inset-0 z-[200] flex items-center justify-center bg-black/50" @click.self="$emit('close')">
    <div class="theme-bg-popup border theme-border rounded shadow-xl w-[500px] max-h-[80vh] overflow-auto">
      <div class="flex items-center justify-between px-4 py-3 border-b theme-border">
        <h3 class="text-sm font-semibold theme-text-primary">新建数据源</h3>
        <button @click="$emit('close')" class="text-xs theme-text-secondary hover:text-white">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>
      
      <div class="p-4 space-y-4">
        <div>
          <label class="block text-xs theme-text-secondary mb-1">名称</label>
          <input
            v-model="form.name"
            type="text"
            class="w-full px-3 py-2 text-xs theme-bg-main theme-text-primary border theme-border rounded focus:outline-none focus:border-blue-500"
            placeholder="连接名称"
          />
        </div>
        
        <div>
          <label class="block text-xs theme-text-secondary mb-1">数据库类型</label>
          <select
            v-model="form.dbType"
            class="w-full px-3 py-2 text-xs theme-bg-main theme-text-primary border theme-border rounded focus:outline-none focus:border-blue-500"
            @change="updatePortByType"
          >
            <option value="mysql">
              <img src="/mysql.png" class="w-4 h-4 inline mr-2" /> MySQL
            </option>
            <option value="postgres">PostgreSQL</option>
            <option value="clickhouse">ClickHouse</option>
            <option value="redis">Redis</option>
          </select>
        </div>
        
        <div>
          <label class="block text-xs theme-text-secondary mb-1">主机</label>
          <input
            v-model="form.host"
            type="text"
            class="w-full px-3 py-2 text-xs theme-bg-main theme-text-primary border theme-border rounded focus:outline-none focus:border-blue-500"
            placeholder="localhost"
          />
        </div>
        
        <div>
          <label class="block text-xs theme-text-secondary mb-1">端口</label>
          <input
            v-model.number="form.port"
            type="number"
            class="w-full px-3 py-2 text-xs theme-bg-main theme-text-primary border theme-border rounded focus:outline-none focus:border-blue-500"
            placeholder="3306"
          />
        </div>
        
        <div>
          <label class="block text-xs theme-text-secondary mb-1">数据库</label>
          <input
            v-model="form.database"
            type="text"
            class="w-full px-3 py-2 text-xs theme-bg-main theme-text-primary border theme-border rounded focus:outline-none focus:border-blue-500"
            placeholder="database name"
          />
        </div>
        
        <div>
          <label class="block text-xs theme-text-secondary mb-1">用户名</label>
          <input
            v-model="form.username"
            type="text"
            class="w-full px-3 py-2 text-xs theme-bg-main theme-text-primary border theme-border rounded focus:outline-none focus:border-blue-500"
            placeholder="username"
          />
        </div>
        
        <div>
          <label class="block text-xs theme-text-secondary mb-1">密码</label>
          <input
            v-model="form.password"
            type="password"
            class="w-full px-3 py-2 text-xs theme-bg-main theme-text-primary border theme-border rounded focus:outline-none focus:border-blue-500"
            placeholder="password"
          />
        </div>
      </div>
      
      <div class="flex items-center justify-end space-x-2 px-4 py-3 border-t theme-border">
        <button
          @click="$emit('close')"
          class="px-3 py-1 text-xs theme-text-secondary hover:text-white"
        >
          取消
        </button>
        <button
          @click="handleSave"
          class="px-3 py-1 text-xs theme-bg-main theme-text-primary border theme-border rounded hover:bg-white/5"
        >
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  'close': []
  'save': [data: any]
}>()

const form = ref({
  name: '',
  dbType: 'mysql',
  host: 'localhost',
  port: 3306,
  database: '',
  username: '',
  password: ''
})

const updatePortByType = () => {
  const portMap: Record<string, number> = {
    mysql: 3306,
    postgres: 5432,
    clickhouse: 8123,
    redis: 6379
  }
  form.value.port = portMap[form.value.dbType] || 3306
}

const handleSave = () => {
  emit('save', { ...form.value })
}
</script>

<style scoped>
@import '@/styles/database/theme.css';
</style>

