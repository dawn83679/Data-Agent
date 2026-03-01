/** Mirrors backend AskUserConfirmTool.WriteConfirmationResult */
export interface WriteConfirmPayload {
  confirmationToken: string;
  sqlPreview: string;
  explanation: string;
  connectionId: number;
  databaseName: string;
  schemaName?: string;
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
    return {
      confirmationToken: obj.confirmationToken,
      sqlPreview: typeof obj.sqlPreview === 'string' ? obj.sqlPreview : '',
      explanation: typeof obj.explanation === 'string' ? obj.explanation : '',
      connectionId: typeof obj.connectionId === 'number' ? obj.connectionId : 0,
      databaseName: typeof obj.databaseName === 'string' ? obj.databaseName : '',
      schemaName: typeof obj.schemaName === 'string' ? obj.schemaName : undefined,
      expiresInSeconds: typeof obj.expiresInSeconds === 'number' ? obj.expiresInSeconds : 300,
      error: typeof obj.error === 'boolean' ? obj.error : undefined,
      errorMessage: typeof obj.errorMessage === 'string' ? obj.errorMessage : undefined,
    };
  } catch {
    return null;
  }
}
