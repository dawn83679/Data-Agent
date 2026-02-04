/** Available driver version from Maven Central */
export interface AvailableDriverResponse {
  databaseType: string;
  version: string;
  installed: boolean;
  groupId: string;
  artifactId: string;
  mavenCoordinates: string;
}

/** Locally installed driver file */
export interface InstalledDriverResponse {
  databaseType: string;
  fileName: string;
  version: string;
  filePath: string;
  fileSize: number;
  lastModified: string;
}

/** Request for POST /drivers/download */
export interface DownloadDriverRequest {
  databaseType: string;
  version?: string;
}

/** Response from POST /drivers/download */
export interface DownloadDriverResponse {
  driverPath: string;
  databaseType: string;
  fileName: string;
  version: string;
}
