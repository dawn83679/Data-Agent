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
    // When "All" is selected we should load every driver type.
    // For now we only load MySQL as a placeholder until the backend supports it.
    await driverStore.fetchInstalledDrivers('MySQL')
  }
}

async function handleRefresh() {
  if (selectedDatabaseType.value) {
    await driverStore.fetchInstalledDrivers(selectedDatabaseType.value)
  }
}

onMounted(async () => {
  // Preload the MySQL driver list as a default example.
  await driverStore.fetchInstalledDrivers('MySQL')
})
</script>

<style scoped>
.driver-view {
  padding: var(--spacing-xl);
  max-width: 1400px;
  margin: 0 auto;
}

.driver-view h2 {
  font-size: 32px;
  margin-bottom: var(--spacing-lg);
  color: var(--color-text);
}

.driver-container {
  margin-top: var(--spacing-lg);
}

.filter-section {
  margin: var(--spacing-lg) 0;
  padding: var(--spacing-lg);
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.filter-section label {
  font-weight: 600;
  color: var(--color-text);
}

.filter-section select {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  min-width: 200px;
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

