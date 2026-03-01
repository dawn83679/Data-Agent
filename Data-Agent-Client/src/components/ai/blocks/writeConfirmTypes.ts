/** Mirrors backend AskUserConfirmTool.WriteConfirmationResult */
export interface WriteConfirmPayload {
  confirmationToken: string;
  sqlPreview: string;
  explanation: string;
  connectionId: number;
  databaseName?: string | null;
  schemaName?: string | null;
  expiresInSeconds: number;
  error?: boolean;
  errorMessage?: string;
}

export const WRITE_CONFIRM_TOOL_NAME = 'askUserConfirm';

export function isWriteConfirmTool(toolName: string): boolean {
  return toolName === WRITE_CONFIRM_TOOL_NAME;
}

export function parseWriteConfirmPayload(
  responseData: string | null | undefined
): WriteConfirmPayload | null {
  if (!responseData?.trim()) return null;
  try {
    const parsed = JSON.parse(responseData.trim()) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;
    if (typeof obj.confirmationToken !== 'string' || !obj.confirmationToken) return null;
    const payload: WriteConfirmPayload = {
      confirmationToken: obj.confirmationToken,
      sqlPreview: typeof obj.sqlPreview === 'string' ? obj.sqlPreview : '',
      explanation: typeof obj.explanation === 'string' ? obj.explanation : '',
      connectionId: typeof obj.connectionId === 'number' ? obj.connectionId : 0,
      expiresInSeconds: typeof obj.expiresInSeconds === 'number' ? obj.expiresInSeconds : 300,
      error: typeof obj.error === 'boolean' ? obj.error : undefined,
      errorMessage: typeof obj.errorMessage === 'string' ? obj.errorMessage : undefined,
    };
    if (typeof obj.databaseName === 'string') {
      payload.databaseName = obj.databaseName;
    } else if (obj.databaseName === null) {
      payload.databaseName = null;
    }
    if (typeof obj.schemaName === 'string') {
      payload.schemaName = obj.schemaName;
    } else if (obj.schemaName === null) {
      payload.schemaName = null;
    }
    return payload;
  } catch {
    return null;
  }
}
