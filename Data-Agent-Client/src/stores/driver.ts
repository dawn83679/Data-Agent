/**
 * Pinia store for managing JDBC driver metadata.
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as driverApi from '@/api/driver'
import type {
  AvailableDriverResponse,
  InstalledDriverResponse,
} from '@/types/driver'

export const useDriverStore = defineStore('driver', () => {
  // Reactive state
  const availableDrivers = ref<AvailableDriverResponse[]>([])
  const installedDrivers = ref<InstalledDriverResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Fetch available driver versions from the backend
  async function fetchAvailableDrivers(databaseType: string) {
    loading.value = true
    error.value = null
    try {
      const response = await driverApi.listAvailableDrivers(databaseType)
      availableDrivers.value = response.data
    } catch (err: any) {
      error.value = err.message || '获取可用驱动列表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Fetch drivers already downloaded to disk
  async function fetchInstalledDrivers(databaseType: string) {
    loading.value = true
    error.value = null
    try {
      const response = await driverApi.listInstalledDrivers(databaseType)
      installedDrivers.value = response.data
    } catch (err: any) {
      error.value = err.message || '获取已安装驱动列表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Download a driver and persist it locally
  async function downloadDriver(databaseType: string, version?: string) {
    loading.value = true
    error.value = null
    try {
      const response = await driverApi.downloadDriver({
        databaseType,
        version,
      })
      // Refresh the installed list so the UI reflects the new artifact
      await fetchInstalledDrivers(databaseType)
      return response.data
    } catch (err: any) {
      error.value = err.message || '下载驱动失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Remove a driver from disk
  async function removeDriver(databaseType: string, version: string) {
    loading.value = true
    error.value = null
    try {
      await driverApi.deleteDriver(databaseType, version)
      // Refresh the installed list after deletion
      await fetchInstalledDrivers(databaseType)
    } catch (err: any) {
      error.value = err.message || '删除驱动失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    // Expose state
    availableDrivers,
    installedDrivers,
    loading,
    error,
    // Expose actions
    fetchAvailableDrivers,
    fetchInstalledDrivers,
    downloadDriver,
    removeDriver,
  }
})

