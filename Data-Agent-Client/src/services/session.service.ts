import http from '../lib/http';
import { SessionInfo } from '../types/auth';

export const sessionService = {
    /**
     * List all active sessions for current user
     */
    listActiveSessions: async (): Promise<SessionInfo[]> => {
        const response = await http.get<SessionInfo[]>('/session/list');
        return response.data;
    },

    /**
     * Revoke a specific session (logout from another device)
     */
    revokeSession: async (sessionId: number): Promise<void> => {
        await http.delete<void>(`/session/${sessionId}`);
    },
};
