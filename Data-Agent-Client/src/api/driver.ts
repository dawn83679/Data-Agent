/**
 * Driver management API helpers.
 */
import { get, post, del } from '@/utils/request'
import type {
  AvailableDriverResponse,
  InstalledDriverResponse,
  DownloadDriverRequest,
  DownloadDriverResponse,
} from '@/types/driver'

/**
 * Fetch downloadable driver versions.
 */
export function listAvailableDrivers(databaseType: string) {
  return get<AvailableDriverResponse[]>('/api/drivers/available', {
    databaseType,
  })
}

/**
 * Fetch drivers that already exist on disk.
 */
export function listInstalledDrivers(databaseType: string) {
  return get<InstalledDriverResponse[]>('/api/drivers/installed', {
    databaseType,
  })
}

/**
 * Trigger a driver download.
 */
export function downloadDriver(request: DownloadDriverRequest) {
  return post<DownloadDriverResponse>('/api/drivers/download', request)
}

/**
 * Delete a downloaded driver artifact.
 */
export function deleteDriver(databaseType: string, version: string) {
  return del<void>(`/api/drivers/${databaseType}/${version}`)
}

