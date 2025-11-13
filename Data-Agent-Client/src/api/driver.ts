/**
 * 驱动管理 API
 */
import { get, post, del } from '@/utils/request'
import type {
  AvailableDriverResponse,
  InstalledDriverResponse,
  DownloadDriverRequest,
  DownloadDriverResponse,
} from '@/types/driver'

/**
 * 获取可用驱动列表
 */
export function listAvailableDrivers(databaseType: string) {
  return get<AvailableDriverResponse[]>('/api/drivers/available', {
    databaseType,
  })
}

/**
 * 获取已安装驱动列表
 */
export function listInstalledDrivers(databaseType: string) {
  return get<InstalledDriverResponse[]>('/api/drivers/installed', {
    databaseType,
  })
}

/**
 * 下载驱动
 */
export function downloadDriver(request: DownloadDriverRequest) {
  return post<DownloadDriverResponse>('/api/drivers/download', request)
}

/**
 * 删除驱动
 */
export function deleteDriver(databaseType: string, version: string) {
  return del<void>(`/api/drivers/${databaseType}/${version}`)
}

