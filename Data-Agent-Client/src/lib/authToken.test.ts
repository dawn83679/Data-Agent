import { describe, expect, it } from 'vitest';
import { ErrorCode, HttpStatusCode } from '../constants/errorCode';
import { resolveRefreshAccessTokenResult } from './authToken';

describe('resolveRefreshAccessTokenResult', () => {
    it('keeps auth state retryable when refresh fails because the backend is unavailable', () => {
        expect(resolveRefreshAccessTokenResult(HttpStatusCode.INTERNAL_SERVER_ERROR, null)).toEqual({
            status: 'retry-later',
        });
    });

    it('clears auth only when refresh is explicitly unauthorized', () => {
        expect(resolveRefreshAccessTokenResult(HttpStatusCode.UNAUTHORIZED, null)).toEqual({
            status: 'auth-failed',
        });
        expect(resolveRefreshAccessTokenResult(HttpStatusCode.OK, { code: ErrorCode.NOT_LOGIN_ERROR })).toEqual({
            status: 'auth-failed',
        });
    });

    it('accepts valid token pairs from ApiResponse payloads', () => {
        expect(
            resolveRefreshAccessTokenResult(HttpStatusCode.OK, {
                code: ErrorCode.SUCCESS,
                data: {
                    accessToken: 'new-access-token',
                    refreshToken: 'new-refresh-token',
                },
            })
        ).toEqual({
            status: 'success',
            tokens: {
                accessToken: 'new-access-token',
                refreshToken: 'new-refresh-token',
            },
        });
    });
});
