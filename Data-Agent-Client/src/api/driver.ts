import request from '@/utils/request'
import type {
  ApiResponse,
  DownloadDriverRequest,
  AvailableDriverResponse,
  InstalledDriverResponse,
  DownloadDriverResponse,
} from '@/types/api'

/**
 * 驱动管理 API
 */
export const driverApi = {
  /**
   * 获取可用驱动列表（从 Maven Central）
   * GET /api/drivers/available?databaseType={databaseType}
   */
  getAvailableDrivers(databaseType: string): Promise<ApiResponse<AvailableDriverResponse[]>> {
    return request.get('/drivers/available', {
      params: { databaseType },
    })
  },

  /**
   * 获取已安装驱动列表
   * GET /api/drivers/installed?databaseType={databaseType}
   */
  getInstalledDrivers(databaseType: string): Promise<ApiResponse<InstalledDriverResponse[]>> {
    return request.get('/drivers/installed', {
      params: { databaseType },
    })
  },

  /**
   * 下载驱动
   * POST /api/drivers/download
   */
  downloadDriver(data: DownloadDriverRequest): Promise<ApiResponse<DownloadDriverResponse>> {
    return request.post('/drivers/download', data)
  },

  /**
   * 删除驱动
   * DELETE /api/drivers/{databaseType}/{version}
   */
  deleteDriver(databaseType: string, version: string): Promise<ApiResponse<void>> {
    return request.delete(`/drivers/${databaseType}/${version}`)
  },
}

