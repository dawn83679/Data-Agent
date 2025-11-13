<template>
  <div class="driver-view">
    <h2>驱动管理</h2>
    
    <div class="driver-container">
      <DriverDownload
        :loading="driverStore.loading"
        :error="driverStore.error"
        @download="handleDownload"
      />
      
      <div class="filter-section">
        <label for="databaseTypeFilter">筛选数据库类型：</label>
        <select id="databaseTypeFilter" v-model="selectedDatabaseType" @change="handleFilterChange">
          <option value="">全部</option>
          <option value="MySQL">MySQL</option>
          <option value="PostgreSQL">PostgreSQL</option>
          <option value="Oracle">Oracle</option>
        </select>
        <button @click="handleRefresh" class="btn-refresh">刷新</button>
      </div>
      
      <DriverList
        :drivers="filteredDrivers"
        :loading="driverStore.loading"
        :error="driverStore.error"
        @delete="handleDelete"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useDriverStore } from '@/stores/driver'
import DriverList from '@/components/driver/DriverList.vue'
import DriverDownload from '@/components/driver/DriverDownload.vue'

const driverStore = useDriverStore()
const selectedDatabaseType = ref('')

const filteredDrivers = computed(() => {
  if (!selectedDatabaseType.value) {
    return driverStore.installedDrivers
  }
  return driverStore.installedDrivers.filter(
    (driver) => driver.databaseType === selectedDatabaseType.value
  )
})

async function handleDownload(databaseType: string, version?: string) {
  try {
    await driverStore.downloadDriver(databaseType, version)
    alert('驱动下载成功！')
  } catch (err: any) {
    alert('下载失败：' + (err.message || '未知错误'))
  }
}

async function handleDelete(databaseType: string, version: string) {
  try {
    await driverStore.removeDriver(databaseType, version)
    alert('驱动删除成功！')
  } catch (err: any) {
    alert('删除失败：' + (err.message || '未知错误'))
  }
}

async function handleFilterChange() {
  if (selectedDatabaseType.value) {
    await driverStore.fetchInstalledDrivers(selectedDatabaseType.value)
  } else {
    // 如果选择"全部"，需要获取所有类型的驱动
    // 这里简化处理，可以后续优化
    await driverStore.fetchInstalledDrivers('MySQL')
  }
}

async function handleRefresh() {
  if (selectedDatabaseType.value) {
    await driverStore.fetchInstalledDrivers(selectedDatabaseType.value)
  }
}

onMounted(async () => {
  // 初始加载 MySQL 驱动列表作为示例
  await driverStore.fetchInstalledDrivers('MySQL')
})
</script>

<style scoped>
.driver-view {
  padding: 20px;
}

.driver-container {
  margin-top: 20px;
}

.filter-section {
  margin: 20px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-section label {
  font-weight: bold;
}

.filter-section select {
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.btn-refresh {
  padding: 5px 15px;
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

