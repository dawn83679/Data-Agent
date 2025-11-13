/**
 * 驱动相关类型定义
 */

/**
 * 可用驱动响应
 */
export interface AvailableDriverResponse {
  databaseType: string
  version: string
  installed: boolean
  groupId: string
  artifactId: string
  mavenCoordinates: string
}

/**
 * 已安装驱动响应
 */
export interface InstalledDriverResponse {
  databaseType: string
  fileName: string
  version: string
  filePath: string
  fileSize: number
  lastModified: string
}

/**
 * 下载驱动请求
 */
export interface DownloadDriverRequest {
  databaseType: string
  version?: string
}

/**
 * 下载驱动响应
 */
export interface DownloadDriverResponse {
  driverPath: string
  databaseType: string
  fileName: string
  version: string
}

