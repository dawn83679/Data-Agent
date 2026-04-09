import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { OrgRoleCode, User, WorkspaceType } from '../types/auth';
import { decodeJwt } from '../lib/jwtUtil';

declare global {
    interface Window {
        __OPEN_LOGIN_MODAL__: (() => void) | null;
    }
}

interface AuthStore {
    user: User | null;
    accessToken: string | null;
    refreshToken: string | null;
    expiresAt: number | null;
    rememberMe: boolean;
    isLoginModalOpen: boolean;

    /** API request workspace (headers X-Workspace-Type / X-Org-Id). */
    workspaceType: WorkspaceType;
    workspaceOrgId: number | null;
    workspaceOrgRole: OrgRoleCode | null;

    setAuth: (user: User | null, accessToken: string | null, refreshToken: string | null, rememberMe?: boolean) => void;
    clearAuth: () => void;
    openLoginModal: () => void;
    closeLoginModal: () => void;

    setWorkspacePersonal: () => void;
    setWorkspaceOrganization: (orgId: number) => void;
    /** Reset org workspace if persisted org is no longer valid for the user. */
    reconcileWorkspaceWithUser: () => void;
    /** True when in organization context with COMMON role (AI-only layout). */
    isOrgCommonWorkbench: () => boolean;
}

const defaultWorkspace = (): Pick<AuthStore, 'workspaceType' | 'workspaceOrgId' | 'workspaceOrgRole'> => ({
    workspaceType: 'PERSONAL',
    workspaceOrgId: null,
    workspaceOrgRole: null,
});

function normalizeRole(code: string | undefined | null): OrgRoleCode | null {
    if (!code) return null;
    const u = String(code).toUpperCase();
    if (u === 'ADMIN' || u === 'COMMON') return u;
    return null;
}

export const useAuthStore = create<AuthStore>()(
    persist(
        (set, get) => ({
            user: null,
            accessToken: null,
            refreshToken: null,
            expiresAt: null,
            rememberMe: false,
            isLoginModalOpen: false,
            ...defaultWorkspace(),

            setAuth: (user, accessToken, refreshToken, rememberMe = false) =>
                set((state) => {
                    let finalUser = user;
                    let finalExpiresAt: number | null = null;

                    if (accessToken) {
                        const decoded = decodeJwt(accessToken);
                        if (decoded) {
                            if (!finalUser) {
                                finalUser = decoded.user;
                            } else {
                                finalUser = {
                                    ...decoded.user,
                                    ...finalUser,
                                    organizations: finalUser.organizations ?? decoded.user.organizations,
                                };
                            }
                            finalExpiresAt = decoded.expiresAt;
                        }
                    }

                    return {
                        user: finalUser,
                        accessToken,
                        refreshToken,
                        rememberMe: rememberMe ?? state.rememberMe,
                        expiresAt: finalExpiresAt,
                    };
                }),

            clearAuth: () =>
                set({
                    user: null,
                    accessToken: null,
                    refreshToken: null,
                    expiresAt: null,
                    rememberMe: false,
                    ...defaultWorkspace(),
                }),

            openLoginModal: () =>
                set((state) => (state.isLoginModalOpen ? state : { ...state, isLoginModalOpen: true })),

            closeLoginModal: () =>
                set((state) => (state.isLoginModalOpen ? { ...state, isLoginModalOpen: false } : state)),

            setWorkspacePersonal: () =>
                set({
                    workspaceType: 'PERSONAL',
                    workspaceOrgId: null,
                    workspaceOrgRole: null,
                }),

            setWorkspaceOrganization: (orgId: number) => {
                const { user } = get();
                const match = user?.organizations?.find((o) => o.orgId === orgId);
                if (!match) return;
                const role = normalizeRole(match.roleCode as string);
                if (!role) return;
                set({
                    workspaceType: 'ORGANIZATION',
                    workspaceOrgId: orgId,
                    workspaceOrgRole: role,
                });
            },

            reconcileWorkspaceWithUser: () => {
                const { user, workspaceType, workspaceOrgId } = get();
                if (workspaceType !== 'ORGANIZATION' || workspaceOrgId == null) {
                    return;
                }
                const match = user?.organizations?.find((o) => o.orgId === workspaceOrgId);
                if (!match) {
                    set({ ...defaultWorkspace() });
                    return;
                }
                const role = normalizeRole(match.roleCode as string);
                if (!role) {
                    set({ ...defaultWorkspace() });
                    return;
                }
                set({ workspaceOrgRole: role });
            },

            isOrgCommonWorkbench: () => {
                const s = get();
                return s.workspaceType === 'ORGANIZATION' && s.workspaceOrgRole === 'COMMON';
            },
        }),
        {
            name: 'auth-storage',
            storage: {
                getItem: (name) => {
                    const str = localStorage.getItem(name) || sessionStorage.getItem(name);
                    return str ? JSON.parse(str) : null;
                },
                setItem: (name, value: any) => {
                    const str = JSON.stringify(value);
                    if (value.state.rememberMe) {
                        localStorage.setItem(name, str);
                        sessionStorage.removeItem(name);
                    } else {
                        sessionStorage.setItem(name, str);
                        localStorage.removeItem(name);
                    }
                },
                removeItem: (name) => {
                    localStorage.removeItem(name);
                    sessionStorage.removeItem(name);
                },
            },
            partialize: (state) => ({
                user: state.user,
                accessToken: state.accessToken,
                refreshToken: state.refreshToken,
                rememberMe: state.rememberMe,
                expiresAt: state.expiresAt,
                workspaceType: state.workspaceType,
                workspaceOrgId: state.workspaceOrgId,
                workspaceOrgRole: state.workspaceOrgRole,
            }),
        }
    )
);
