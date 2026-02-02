import http from '../lib/http';
import { LoginRequest, RegisterRequest, ResetPasswordRequest, TokenPairResponse, User } from '../types/auth';

export const authService = {
    /**
     * Login with email and password
     */
    login: async (data: LoginRequest): Promise<TokenPairResponse> => {
        const response = await http.post<TokenPairResponse>('/auth/login', data);
        return response.data;
    },

    /**
     * Get current user info
     */
    getCurrentUser: async (): Promise<User> => {
        const response = await http.get<User>('/user/me');
        return response.data;
    },

    /**
     * Register a new user
     */
    register: async (data: RegisterRequest): Promise<boolean> => {
        const response = await http.post<boolean>('/auth/register', data);
        return response.data;
    },

    /**
     * Logout the current user
     */
    logout: async (): Promise<void> => {
        await http.post<void>('/auth/logout');
    },

    /**
     * Reset password
     */
    resetPassword: async (data: ResetPasswordRequest): Promise<void> => {
        await http.post<void>('/auth/reset-password', data);
    },

    /**
     * Refresh access token using refresh token
     */
    refresh: async (refreshToken: string): Promise<TokenPairResponse> => {
        const response = await http.post<TokenPairResponse>('/auth/refresh', { refreshToken });
        return response.data;
    },

    /**
     * Get OAuth authorization URL
     * @param provider 'google' | 'github'
     * @param fromUrl URL to redirect back to after login
     */
    getOAuthUrl: (provider: 'google' | 'github', fromUrl: string): string => {
        // Note: This is a direct browser redirect, not an AJAX call
        // We construct the URL pointing to our backend
        const encodedFromUrl = encodeURIComponent(fromUrl);
        return `/api/oauth/${provider}?fromUrl=${encodedFromUrl}`;
    },
};
