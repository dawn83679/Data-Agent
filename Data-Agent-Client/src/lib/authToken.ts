import { useAuthStore } from '../store/authStore';
import { decodeJwt, shouldRefreshToken } from './jwtUtil';
import type { TokenPairResponse } from '../types/auth';
import { ErrorCode, HttpStatusCode } from '../constants/errorCode';

/** Single in-flight refresh promise to avoid concurrent refresh calls */
let refreshPromise: Promise<string | null> | null = null;

export type AccessTokenRefreshResult =
    | { status: 'success'; tokens: TokenPairResponse }
    | { status: 'auth-failed' }
    | { status: 'retry-later' };

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
}

function readCode(payload: unknown): number | null {
    if (!isRecord(payload)) return null;
    const code = payload.code;
    return typeof code === 'number' ? code : null;
}

function readTokenPair(payload: unknown): TokenPairResponse | null {
    const data = isRecord(payload) && 'data' in payload ? payload.data : payload;
    if (!isRecord(data)) return null;
    const { accessToken, refreshToken } = data;
    if (typeof accessToken !== 'string' || typeof refreshToken !== 'string') {
        return null;
    }
    return { accessToken, refreshToken };
}

export function resolveRefreshAccessTokenResult(status: number, payload: unknown): AccessTokenRefreshResult {
    if (status === HttpStatusCode.UNAUTHORIZED || readCode(payload) === ErrorCode.NOT_LOGIN_ERROR) {
        return { status: 'auth-failed' };
    }

    if (status >= 200 && status < 300) {
        const tokens = readTokenPair(payload);
        if (tokens) {
            return { status: 'success', tokens };
        }
    }

    return { status: 'retry-later' };
}

export function applyRefreshedTokens(tokens: TokenPairResponse): void {
    const { user, rememberMe, setAuth } = useAuthStore.getState();
    setAuth(user, tokens.accessToken, tokens.refreshToken, rememberMe);
}

export function clearAuthAndOpenLogin(): void {
    const { clearAuth, openLoginModal } = useAuthStore.getState();
    clearAuth();
    openLoginModal();
}

export async function refreshAccessToken(): Promise<AccessTokenRefreshResult> {
    const { refreshToken } = useAuthStore.getState();
    if (!refreshToken) {
        return { status: 'auth-failed' };
    }

    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        });

        let payload: unknown = null;
        try {
            payload = await response.json();
        } catch {
            payload = null;
        }

        return resolveRefreshAccessTokenResult(response.status, payload);
    } catch {
        return { status: 'retry-later' };
    }
}

/**
 * Ensure the access token is valid: not expired and not expiring within 5 minutes.
 * If it is expired or expiring soon, refresh via POST /api/auth/refresh and return the new token.
 * Uses a lock so only one refresh runs at a time; concurrent callers wait for the same result.
 * @returns Current or newly refreshed access token, or null if no token / refresh failed
 */
export async function ensureValidAccessToken(): Promise<string | null> {
    const { accessToken, refreshToken, expiresAt } = useAuthStore.getState();

    if (!accessToken) {
        return null;
    }

    let effectiveExpiresAt = expiresAt;
    if (effectiveExpiresAt == null) {
        const decoded = decodeJwt(accessToken);
        effectiveExpiresAt = decoded?.expiresAt ?? 0;
    }

    const needRefresh = shouldRefreshToken(effectiveExpiresAt);

    if (!refreshToken) {
        // Cannot refresh; only return token if it is still valid
        return needRefresh ? null : accessToken;
    }

    if (!needRefresh) {
        return accessToken;
    }

    if (refreshPromise) {
        return refreshPromise;
    }

    refreshPromise = (async (): Promise<string | null> => {
        try {
            const result = await refreshAccessToken();
            if (result.status === 'success') {
                applyRefreshedTokens(result.tokens);
                return result.tokens.accessToken;
            }
            if (result.status === 'auth-failed') {
                clearAuthAndOpenLogin();
                return null;
            }
            return accessToken;
        } finally {
            refreshPromise = null;
        }
    })();

    return refreshPromise;
}
