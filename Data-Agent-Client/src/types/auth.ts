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
    oldPassword?: string;
    newPassword: string;
}

export interface TokenPairResponse {
    accessToken: string;
    refreshToken: string;
}

export interface User {
    id: number;
    username: string;
    email: string;
    avatarUrl?: string;
    authProvider?: string;
    verified: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface AuthState {
    user: User | null;
    accessToken: string | null;
    refreshToken: string | null;
    isAuthenticated: boolean;
}
