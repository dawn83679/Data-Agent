export interface LoginRequest {
    email: string;
    password: string;
    rememberMe?: boolean;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
}

export interface ResetPasswordRequest {
    email: string;
    oldPassword: string;
    newPassword: string;
}

export interface UpdateUserRequest {
    username?: string;
    avatarUrl?: string;
}

export interface TokenPairResponse {
    accessToken: string;
    refreshToken: string;
}

export type OrgRoleCode = 'ADMIN' | 'COMMON';

export interface UserOrganizationMembership {
    orgId: number;
    orgCode: string;
    orgName: string;
    roleCode: OrgRoleCode | string;
}

export interface User {
    id: number;
    username: string;
    email: string;
    avatarUrl?: string;
    authProvider?: string;
    organizations?: UserOrganizationMembership[];
}

export type WorkspaceType = 'PERSONAL' | 'ORGANIZATION';

export interface SessionInfo {
    id: number;
    ipAddress?: string;
    userAgent?: string;
    isCurrent: boolean;
    lastRefreshAt: string;
    createdAt: string;
}

