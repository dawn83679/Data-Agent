const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export const TokenManager = {
    setTokens(accessToken: string, refreshToken: string, rememberMe: boolean) {
        if (rememberMe) {
            localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
            localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
            // Clear session storage to avoid duplicates/confusion
            sessionStorage.removeItem(ACCESS_TOKEN_KEY);
            sessionStorage.removeItem(REFRESH_TOKEN_KEY);
        } else {
            sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
            sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
            // Clear local storage
            localStorage.removeItem(ACCESS_TOKEN_KEY);
            localStorage.removeItem(REFRESH_TOKEN_KEY);
        }
    },

    getAccessToken(): string | null {
        return localStorage.getItem(ACCESS_TOKEN_KEY) || sessionStorage.getItem(ACCESS_TOKEN_KEY);
    },

    getRefreshToken(): string | null {
        return localStorage.getItem(REFRESH_TOKEN_KEY) || sessionStorage.getItem(REFRESH_TOKEN_KEY);
    },

    clearTokens() {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
        sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    },

    /**
     * Updates the access token in the storage where it currently exists.
     * If it doesn't exist, defaults to sessionStorage (safe default).
     */
    updateAccessToken(accessToken: string) {
        if (localStorage.getItem(ACCESS_TOKEN_KEY)) {
            localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
        } else {
            sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
        }
    },

    /**
     * Updates the refresh token in the storage where it currently exists.
     */
    updateRefreshToken(refreshToken: string) {
        if (localStorage.getItem(REFRESH_TOKEN_KEY)) {
            localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
        } else {
            sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
        }
    }
};
