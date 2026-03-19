import http from '../lib/http';
import type { PermissionApproveRequest, PermissionRule, PermissionUpsertRequest } from '../types/permission';

export const permissionService = {
  listRules: async (conversationId?: number | null): Promise<PermissionRule[]> => {
    const response = await http.get<PermissionRule[]>('/permissions/rules', {
      params: conversationId != null ? { conversationId } : undefined,
    });
    return response.data;
  },

  upsertRule: async (payload: PermissionUpsertRequest): Promise<PermissionRule> => {
    const response = await http.post<PermissionRule>('/permissions/rules', payload);
    return response.data;
  },

  setRuleEnabled: async (id: number, enabled: boolean): Promise<void> => {
    await http.patch(`/permissions/rules/${id}/enabled`, { enabled });
  },

  deleteRule: async (id: number): Promise<void> => {
    await http.delete(`/permissions/rules/${id}`);
  },

  approveWriteExecution: async (payload: PermissionApproveRequest): Promise<void> => {
    await http.post('/permissions/write-executions/approve', payload);
  },
};
