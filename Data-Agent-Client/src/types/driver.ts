/**
 * Driver-related DTO definitions.
 */

/**
 * Represents a driver that can be downloaded.
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
 * Represents a driver that already exists on disk.
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
 * Request payload used when downloading a driver.
 */
export interface DownloadDriverRequest {
  databaseType: string
  version?: string
}

/**
 * Response payload returned after a download completes.
 */
export interface DownloadDriverResponse {
  driverPath: string
  databaseType: string
  fileName: string
  version: string
}

