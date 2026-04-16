import http from '../lib/http';
import { X_WORKSPACE_TYPE } from '../constants/workspaceHeaders';

export interface ManagedOrganization {
  id: number;
  orgCode: string;
  orgName: string;
}

export interface MyOrganizationMembership {
  id: number;
  orgCode: string;
  orgName: string;
  roleCode: string;
}

export interface OrganizationMemberRow {
  memberId: number;
  userId: number;
  username: string;
  email: string;
  roleCode: string;
}

export const organizationService = {
  managementHeaders: () => ({
    [X_WORKSPACE_TYPE]: 'PERSONAL',
  }),

  listManaged: async (): Promise<ManagedOrganization[]> => {
    const response = await http.get<ManagedOrganization[]>('/organizations/managed', {
      headers: organizationService.managementHeaders(),
    });
    return Array.isArray(response.data) ? response.data : [];
  },

  listMemberships: async (): Promise<MyOrganizationMembership[]> => {
    const response = await http.get<MyOrganizationMembership[]>('/organizations/memberships', {
      headers: organizationService.managementHeaders(),
    });
    return Array.isArray(response.data) ? response.data : [];
  },

  create: async (orgCode: string, orgName: string): Promise<ManagedOrganization> => {
    const response = await http.post<ManagedOrganization>('/organizations', { orgCode, orgName }, {
      headers: organizationService.managementHeaders(),
    });
    return response.data;
  },

  listMembers: async (orgId: number): Promise<OrganizationMemberRow[]> => {
    const response = await http.get<OrganizationMemberRow[]>(`/organizations/${orgId}/members`, {
      headers: organizationService.managementHeaders(),
    });
    return Array.isArray(response.data) ? response.data : [];
  },

  addMember: async (orgId: number, email: string, roleCode: string): Promise<void> => {
    await http.post(`/organizations/${orgId}/members`, { email, roleCode }, {
      headers: organizationService.managementHeaders(),
    });
  },

  removeMember: async (orgId: number, memberId: number): Promise<void> => {
    await http.delete(`/organizations/${orgId}/members/${memberId}`, {
      headers: organizationService.managementHeaders(),
    });
  },

  leaveOrganization: async (orgId: number): Promise<void> => {
    await http.delete(`/organizations/${orgId}/members/me`, {
      headers: organizationService.managementHeaders(),
    });
  },
};
