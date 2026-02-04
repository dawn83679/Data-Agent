import http from '../lib/http';
import type {
  AvailableDriverResponse,
  InstalledDriverResponse,
  DownloadDriverResponse,
} from '../types/driver';

export const driverService = {
  /**
   * List driver versions available from Maven Central for the given database type.
   */
  listAvailableDrivers: async (databaseType: string): Promise<AvailableDriverResponse[]> => {
    const response = await http.get<AvailableDriverResponse[]>('/drivers/available', {
      params: { databaseType },
    });
    return response.data;
  },

  /**
   * List locally installed drivers for the given database type.
   */
  listInstalledDrivers: async (databaseType: string): Promise<InstalledDriverResponse[]> => {
    const response = await http.get<InstalledDriverResponse[]>('/drivers/installed', {
      params: { databaseType },
    });
    return response.data;
  },

  /**
   * Download a driver from Maven Central. If version is omitted, latest is used.
   */
  downloadDriver: async (
    databaseType: string,
    version?: string
  ): Promise<DownloadDriverResponse> => {
    const response = await http.post<DownloadDriverResponse>('/drivers/download', {
      databaseType,
      version,
    });
    return response.data;
  },

  /**
   * Delete a locally installed driver.
   */
  deleteDriver: async (databaseType: string, version: string): Promise<void> => {
    await http.delete(`/drivers/${encodeURIComponent(databaseType)}/${encodeURIComponent(version)}`);
  },
};
