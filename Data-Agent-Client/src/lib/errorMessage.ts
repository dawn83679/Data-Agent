import { ErrorCode, HttpStatusCode } from '../constants/errorCode';
import i18n from '../i18n';

interface ErrorResponseData {
    code?: number;
    message?: string;
}

interface ApiErrorLike {
    code?: string;
    request?: unknown;
    response?: {
        status?: number;
        data?: ErrorResponseData;
    };
    message?: string;
}

/**
 * 统一从后端错误中解析出用户可读的错误文案
 * @param error AxiosError 或任意 error 对象
 * @param fallback 默认兜底文案
 */
export function resolveErrorMessage(error: unknown, fallback: string): string {
    const err = error as ApiErrorLike | undefined;

    // 优先使用后端直接返回的 message
    const backendMessage = err?.response?.data?.message;
    if (backendMessage && typeof backendMessage === 'string') {
        return backendMessage;
    }

    const status = err?.response?.status;
    const code = err?.response?.data?.code;

    // 根据业务错误码做基础映射
    if (typeof code === 'number') {
        switch (code) {
            case ErrorCode.NOT_LOGIN_ERROR:
                return i18n.t('error.not_login');
            case ErrorCode.NO_AUTH_ERROR:
                return i18n.t('error.no_auth');
            case ErrorCode.VALIDATION_ERROR:
            case ErrorCode.REQUIRED_FIELD_EMPTY:
            case ErrorCode.FIELD_FORMAT_ERROR:
            case ErrorCode.FIELD_LENGTH_EXCEEDED:
            case ErrorCode.FIELD_VALUE_OUT_OF_RANGE:
                return i18n.t('error.validation');
            case ErrorCode.DB_CONNECTION_ERROR:
            case ErrorCode.DB_CONNECTION_TIMEOUT:
                return i18n.t('error.db_connection');
            case ErrorCode.SYSTEM_ERROR:
                return i18n.t('error.system');
            default:
                break;
        }
    }

    // 根据 HTTP 状态码做兜底映射
    if (typeof status === 'number') {
        switch (status) {
            case HttpStatusCode.BAD_REQUEST:
                return i18n.t('error.http_400');
            case HttpStatusCode.UNAUTHORIZED:
                return i18n.t('error.http_401');
            case HttpStatusCode.FORBIDDEN:
                return i18n.t('error.http_403');
            case HttpStatusCode.NOT_FOUND:
                return i18n.t('error.http_404');
            case HttpStatusCode.INTERNAL_SERVER_ERROR:
                return i18n.t('error.http_500');
            default:
                break;
        }
    }

    if (err?.code === 'ECONNABORTED') {
        return i18n.t('error.timeout');
    }

    if (err?.message === 'Network Error' || (err?.request && err?.response == null)) {
        return i18n.t('error.network');
    }

    // 兜底：用 error.message 或调用方给的 fallback
    if (err?.message && typeof err.message === 'string') {
        return err.message;
    }

    return fallback;
}
