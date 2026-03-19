import { MemoryPaths } from '../constants/apiPaths';
import http from '../lib/http';
import type {
  Memory,
  MemoryCreateRequest,
  MemoryMetadataResponse,
  MemoryListParams,
  MemoryMaintenanceReport,
  MemoryPage,
  MemorySearchRequest,
  MemorySearchResult,
  MemoryUpdateRequest,
} from '../types/memory';

const buildMemoryPath = (template: string, id: number) => template.replace(':id', String(id));

export const memoryService = {
  list: async (params: MemoryListParams = {}): Promise<MemoryPage> => {
    const response = await http.get<MemoryPage>(MemoryPaths.LIST, {
      params: {
        current: params.current ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        memoryType: params.memoryType || undefined,
        status: params.status,
        reviewState: params.reviewState || undefined,
        scope: params.scope || undefined,
      },
    });
    return response.data;
  },

  getMetadata: async (): Promise<MemoryMetadataResponse> => {
    const response = await http.get<MemoryMetadataResponse>(MemoryPaths.METADATA);
    return response.data;
  },

  getById: async (id: number): Promise<Memory> => {
    const response = await http.get<Memory>(buildMemoryPath(MemoryPaths.DETAIL, id));
    return response.data;
  },

  search: async (data: MemorySearchRequest): Promise<MemorySearchResult[]> => {
    const response = await http.post<MemorySearchResult[]>(MemoryPaths.SEARCH, data);
    return Array.isArray(response.data) ? response.data : [];
  },

  getMaintenanceSummary: async (): Promise<MemoryMaintenanceReport> => {
    const response = await http.get<MemoryMaintenanceReport>(MemoryPaths.MAINTENANCE_SUMMARY);
    return response.data;
  },

  runMaintenance: async (): Promise<MemoryMaintenanceReport> => {
    const response = await http.post<MemoryMaintenanceReport>(MemoryPaths.MAINTENANCE_RUN);
    return response.data;
  },

  create: async (data: MemoryCreateRequest): Promise<Memory> => {
    const response = await http.post<Memory>(MemoryPaths.CREATE, data);
    return response.data;
  },

  update: async (id: number, data: MemoryUpdateRequest): Promise<Memory> => {
    const response = await http.put<Memory>(buildMemoryPath(MemoryPaths.UPDATE, id), data);
    return response.data;
  },

  archive: async (id: number): Promise<Memory> => {
    const response = await http.post<Memory>(buildMemoryPath(MemoryPaths.ARCHIVE, id));
    return response.data;
  },

  restore: async (id: number): Promise<Memory> => {
    const response = await http.post<Memory>(buildMemoryPath(MemoryPaths.RESTORE, id));
    return response.data;
  },

  confirm: async (id: number): Promise<Memory> => {
    const response = await http.post<Memory>(buildMemoryPath(MemoryPaths.CONFIRM, id));
    return response.data;
  },

  markNeedsReview: async (id: number): Promise<Memory> => {
    const response = await http.post<Memory>(buildMemoryPath(MemoryPaths.NEEDS_REVIEW, id));
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await http.delete(buildMemoryPath(MemoryPaths.DELETE, id));
  },
};
