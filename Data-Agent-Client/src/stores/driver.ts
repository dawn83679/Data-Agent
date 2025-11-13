/**
 * 驱动管理 Store
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as driverApi from '@/api/driver'
import type {
  AvailableDriverResponse,
  InstalledDriverResponse,
} from '@/types/driver'

export const useDriverStore = defineStore('driver', () => {
  // 状态
  const availableDrivers = ref<AvailableDriverResponse[]>([])
  const installedDrivers = ref<InstalledDriverResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 获取可用驱动列表
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

  // 获取已安装驱动列表
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

  // 下载驱动
  async function downloadDriver(databaseType: string, version?: string) {
    loading.value = true
    error.value = null
    try {
      const response = await driverApi.downloadDriver({
        databaseType,
        version,
      })
      // 下载成功后刷新已安装列表
      await fetchInstalledDrivers(databaseType)
      return response.data
    } catch (err: any) {
      error.value = err.message || '下载驱动失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 删除驱动
  async function removeDriver(databaseType: string, version: string) {
    loading.value = true
    error.value = null
    try {
      await driverApi.deleteDriver(databaseType, version)
      // 删除成功后刷新已安装列表
      await fetchInstalledDrivers(databaseType)
    } catch (err: any) {
      error.value = err.message || '删除驱动失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    // 状态
    availableDrivers,
    installedDrivers,
    loading,
    error,
    // 方法
    fetchAvailableDrivers,
    fetchInstalledDrivers,
    downloadDriver,
    removeDriver,
  }
})

