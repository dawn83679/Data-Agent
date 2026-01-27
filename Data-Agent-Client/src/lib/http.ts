import axios from 'axios';
import { TokenPairResponse } from '../types/auth';
import { TokenManager } from './token-manager';

const http = axios.create({
    baseURL: '/api', // Proxy will handle this in dev, Nginx in prod
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor: Inject token
http.interceptors.request.use(
    (config) => {
        const token = TokenManager.getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor: Handle 401 and refresh token
http.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        // If 401 and not already retrying
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const refreshToken = TokenManager.getRefreshToken();
                if (!refreshToken) {
                    throw new Error('No refresh token');
                }

                // Call refresh endpoint directly to avoid circular dependency
                const response = await axios.post<TokenPairResponse>('/api/auth/refresh', refreshToken, {
                    headers: { 'Content-Type': 'application/json' }
                });

                const { accessToken, refreshToken: newRefreshToken } = response.data;

                TokenManager.updateAccessToken(accessToken);
                TokenManager.updateRefreshToken(newRefreshToken);

                // Update header and retry original request
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return http(originalRequest);
            } catch (refreshError) {
                // Refresh failed - clear tokens and redirect to login
                TokenManager.clearTokens();
                window.location.href = '/login'; // Or trigger a global event
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default http;
