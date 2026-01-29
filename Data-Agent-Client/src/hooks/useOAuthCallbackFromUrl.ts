import { useCallback, useEffect } from "react";
import { useAuthStore } from "../store/authStore";

export function useOAuthCallbackFromUrl() {
    const { setAuth } = useAuthStore();

    const handleOAuthCallback = useCallback(() => {
        const params = new URLSearchParams(window.location.search);
        const accessToken = params.get("access_token");
        const refreshToken = params.get("refresh_token");
        const loginError = params.get("loginError");

        if (accessToken && refreshToken) {
            setAuth(null, accessToken, refreshToken, true);
            window.history.replaceState({}, document.title, window.location.pathname);
        } else if (loginError) {
            console.error("OAuth Login Error:", loginError);
            window.history.replaceState({}, document.title, window.location.pathname);
        }
    }, [setAuth]);

    useEffect(() => {
        handleOAuthCallback();
    }, [handleOAuthCallback]);
}

