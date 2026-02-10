import { useAuthStore } from '../store/authStore';
import { decodeJwt, shouldRefreshToken } from './jwtUtil';
import type { TokenPairResponse } from '../types/auth';

/** Single in-flight refresh promise to avoid concurrent refresh calls */
let refreshPromise: Promise<string | null> | null = null;

/**
 * Ensure the access token is valid: not expired and not expiring within 5 minutes.
 * If it is expired or expiring soon, refresh via POST /api/auth/refresh and return the new token.
 * Uses a lock so only one refresh runs at a time; concurrent callers wait for the same result.
 * @returns Current or newly refreshed access token, or null if no token / refresh failed
 */
export async function ensureValidAccessToken(): Promise<string | null> {
    const { accessToken, refreshToken, expiresAt, user, setAuth, clearAuth, openLoginModal } =
        useAuthStore.getState();

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
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken }),
            });

            const json = await response.json();
            const data: TokenPairResponse | undefined = json?.data ?? json;
            if (!response.ok || !data?.accessToken || !data?.refreshToken) {
                throw new Error('Refresh failed');
            }

            setAuth(user, data.accessToken, data.refreshToken);
            return data.accessToken;
        } catch {
            clearAuth();
            openLoginModal();
            return null;
        } finally {
            refreshPromise = null;
        }
    })();

    return refreshPromise;
}
