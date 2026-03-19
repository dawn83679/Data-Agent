import {
  PermissionGrantPreset,
  PermissionScopeType,
  type PermissionGrantOption,
} from '../../../types/permission';

export enum ExecuteNonSelectToolStatus {
  EXECUTED = 'EXECUTED',
  REQUIRES_CONFIRMATION = 'REQUIRES_CONFIRMATION',
}

export interface WriteExecutionConfirmationPayload {
  conversationId: number;
  connectionId: number;
  databaseName?: string | null;
  schemaName?: string | null;
  sql: string;
  sqlPreview: string;
  availableGrantOptions: PermissionGrantOption[];
}

export interface ExecuteNonSelectToolResultPayload {
  status: ExecuteNonSelectToolStatus;
  ruleMatched: boolean;
  requiresConfirmation: boolean;
  confirmation?: WriteExecutionConfirmationPayload | null;
  execution?: unknown;
  message?: string;
}

export function parseExecuteNonSelectToolResult(
  responseData: string | null | undefined
): ExecuteNonSelectToolResultPayload | null {
  if (!responseData?.trim()) return null;
  try {
    const parsed = JSON.parse(responseData) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;
    if (obj.status !== ExecuteNonSelectToolStatus.EXECUTED
      && obj.status !== ExecuteNonSelectToolStatus.REQUIRES_CONFIRMATION) return null;

    let confirmation: WriteExecutionConfirmationPayload | null = null;
    if (obj.status === ExecuteNonSelectToolStatus.REQUIRES_CONFIRMATION) {
      if (!obj.confirmation || typeof obj.confirmation !== 'object' || Array.isArray(obj.confirmation)) {
        return null;
      }
      const confirmationRaw = obj.confirmation as Record<string, unknown>;
      if (typeof confirmationRaw.conversationId !== 'number') return null;
      if (typeof confirmationRaw.connectionId !== 'number') return null;
      if (typeof confirmationRaw.sql !== 'string' || confirmationRaw.sql === '') return null;
      if (typeof confirmationRaw.sqlPreview !== 'string' || confirmationRaw.sqlPreview === '') return null;
      if (!Array.isArray(confirmationRaw.availableGrantOptions) || confirmationRaw.availableGrantOptions.length === 0) {
        return null;
      }

      const availableGrantOptions = confirmationRaw.availableGrantOptions.filter(
        (value): value is PermissionGrantOption =>
          !!value
          && typeof value === 'object'
          && 'scopeType' in value
          && 'grantPreset' in value
          && (value.scopeType === PermissionScopeType.CONVERSATION || value.scopeType === PermissionScopeType.USER)
          && (
            value.grantPreset === PermissionGrantPreset.EXACT_SCHEMA
            || value.grantPreset === PermissionGrantPreset.DATABASE_ALL_SCHEMAS
            || value.grantPreset === PermissionGrantPreset.CONNECTION_ALL_DATABASES
          )
      );
      if (availableGrantOptions.length !== confirmationRaw.availableGrantOptions.length) {
        return null;
      }

      confirmation = {
        conversationId: confirmationRaw.conversationId,
        connectionId: confirmationRaw.connectionId,
        databaseName:
          typeof confirmationRaw.databaseName === 'string'
            ? confirmationRaw.databaseName
            : confirmationRaw.databaseName === null
              ? null
              : undefined,
        schemaName:
          typeof confirmationRaw.schemaName === 'string'
            ? confirmationRaw.schemaName
            : confirmationRaw.schemaName === null
              ? null
              : undefined,
        sql: confirmationRaw.sql,
        sqlPreview: confirmationRaw.sqlPreview,
        availableGrantOptions,
      };
    }

    return {
      status: obj.status,
      ruleMatched: obj.ruleMatched === true,
      requiresConfirmation: obj.requiresConfirmation === true,
      confirmation,
      execution: obj.execution,
      message: typeof obj.message === 'string' ? obj.message : undefined,
    };
  } catch {
    return null;
  }
}
