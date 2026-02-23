import axios from 'axios';
import { useAuthStore } from '../store/authStore';
import { HttpStatusCode, ErrorCode } from '../constants/errorCode';
import type { TokenPairResponse } from '../types/auth';
import { ensureValidAccessToken } from './authToken';

const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

const isAuthEndpoint = (url?: string) => {
    const path = url ?? '';
    return path.includes('/auth/refresh') || path.includes('/auth/login');
};

http.interceptors.request.use(
    async (config) => {
        if (isAuthEndpoint(config.url)) {
            return config;
        }
        const token = await ensureValidAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        } else {
            const { accessToken } = useAuthStore.getState();
            if (accessToken) {
                config.headers.Authorization = `Bearer ${accessToken}`;
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

http.interceptors.response.use(
    (response) => {
        // 检查后端返回的 code 字段，只有 code === 200 才是真正的成功
        const responseData = response.data;
        if (responseData && typeof responseData === 'object' && 'code' in responseData) {
            const code = responseData.code;
            // 如果 code 不是 200，将其作为错误处理
            if (code !== ErrorCode.SUCCESS) {
                // 构造一个错误对象，保持与 axios 错误格式一致
                const error: any = new Error(responseData.message || 'Request failed');
                error.response = {
                    status: response.status,
                    data: responseData,
                };
                error.config = response.config;
                return Promise.reject(error);
            }
            // code === 200，如果响应是 ApiResponse 格式，自动提取 data 字段
            // 这样 service 层可以直接使用 response.data，无需关心 ApiResponse 结构
            if ('data' in responseData) {
                response.data = responseData.data;
            }
        }
        // code === 200 或没有 code 字段（兼容旧接口），返回响应
        return response;
    },
    async (error) => {
        const originalRequest = error.config as any;
        // HTTP 401 或 业务 code 401 任一成立即尝试刷新
        const isNotLogin =
            error.response?.status === HttpStatusCode.UNAUTHORIZED ||
            error.response?.data?.code === ErrorCode.NOT_LOGIN_ERROR;

        if (isNotLogin && originalRequest && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const { refreshToken, user, setAuth } = useAuthStore.getState();
                if (!refreshToken) {
                    throw new Error('No refresh token');
                }

                const refreshResponse = await http.post<TokenPairResponse>('/auth/refresh', { refreshToken });
                const { accessToken, refreshToken: newRefreshToken } = refreshResponse.data;

                setAuth(user, accessToken, newRefreshToken);

                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return http(originalRequest);
            } catch (refreshError) {
                const { clearAuth, openLoginModal } = useAuthStore.getState();
                clearAuth();
                openLoginModal();
                return Promise.reject(refreshError);
            }
        }

        if (error.response?.data?.code === ErrorCode.NOT_LOGIN_ERROR) {
            const { openLoginModal } = useAuthStore.getState();
            openLoginModal();
        }

        return Promise.reject(error);
    }
);

export default http;
