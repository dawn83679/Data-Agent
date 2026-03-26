import http from '../lib/http';
import type { DownloadExportFileResponse, ExportFileStatus } from '../types/exportFile';

export const exportFileService = {
  getFileStatus: async (fileId: string): Promise<ExportFileStatus> => {
    const response = await http.get<ExportFileStatus>(`/ai/files/${encodeURIComponent(fileId)}/status`);
    return response.data;
  },

  downloadFile: async (fileId: string): Promise<DownloadExportFileResponse> => {
    const response = await http.get<Blob>(`/ai/files/${encodeURIComponent(fileId)}`, {
      responseType: 'blob',
    });

    const headers = response.headers as Record<string, string | undefined>;
    return {
      blob: response.data,
      contentDisposition: headers['content-disposition'],
      contentType: headers['content-type'],
    };
  },
};
