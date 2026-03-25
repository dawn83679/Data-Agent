export interface ExportFilePreview {
  columns: string[];
  rows: string[][];
  truncated?: boolean;
  totalRowCount?: number;
  totalColumnCount?: number;
}

export interface ExportedFileResult {
  fileId: string;
  filename: string;
  format: string;
  mimeType?: string;
  sizeBytes?: number;
  downloadPath?: string;
  createdAt?: string | number;
  rowCount?: number;
  columnCount?: number;
  preview?: ExportFilePreview;
}

export interface DownloadExportFileResponse {
  blob: Blob;
  contentDisposition?: string;
  contentType?: string;
}
