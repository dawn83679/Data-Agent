export enum PermissionScopeType {
  CONVERSATION = 'CONVERSATION',
  USER = 'USER',
}

export enum PermissionGrantPreset {
  EXACT_SCHEMA = 'EXACT_SCHEMA',
  DATABASE_ALL_SCHEMAS = 'DATABASE_ALL_SCHEMAS',
  CONNECTION_ALL_DATABASES = 'CONNECTION_ALL_DATABASES',
}

export enum PermissionGrantCoverage {
  EXACT_TARGET = 'EXACT_TARGET',
  DATABASE = 'DATABASE',
  CONNECTION = 'CONNECTION',
}

export interface PermissionGrantOption {
  scopeType: PermissionScopeType;
  grantPreset: PermissionGrantPreset;
}

export interface PermissionRule {
  id: number;
  scopeType: PermissionScopeType;
  conversationId?: number | null;
  connectionId: number;
  connectionName?: string | null;
  grantPreset: PermissionGrantPreset;
  catalogName?: string | null;
  schemaName?: string | null;
  enabled: boolean;
  createdAt?: string;
}

export interface PermissionUpsertRequest {
  scopeType: PermissionScopeType;
  conversationId?: number | null;
  connectionId: number;
  grantPreset: PermissionGrantPreset;
  catalogName?: string | null;
  schemaName?: string | null;
  enabled: boolean;
}

export interface PermissionApproveRequest {
  conversationId: number;
  connectionId: number;
  catalogName?: string | null;
  schemaName?: string | null;
  sql: string;
  scopeType?: PermissionScopeType | null;
  grantPreset?: PermissionGrantPreset | null;
}
