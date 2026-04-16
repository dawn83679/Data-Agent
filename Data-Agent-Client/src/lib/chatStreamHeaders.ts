import { useAuthStore } from '../store/authStore';
import { X_ORG_ID, X_WORKSPACE_TYPE } from '../constants/workspaceHeaders';

/** Headers for POST /api/chat/stream — must match axios `http` interceptor (Sa-Token + org workspace). */
export function buildChatStreamFetchHeaders(token: string): Record<string, string> {
  const { workspaceType, workspaceOrgId } = useAuthStore.getState();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
    [X_WORKSPACE_TYPE]: workspaceType,
  };
  if (workspaceType === 'ORGANIZATION' && workspaceOrgId != null) {
    headers[X_ORG_ID] = String(workspaceOrgId);
  }
  return headers;
}
