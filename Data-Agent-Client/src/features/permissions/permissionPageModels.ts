import {
  PERMISSION_EDITOR_MODE,
  PERMISSION_FILTER_VALUE,
  PERMISSION_STATUS_FILTER,
} from './permissionPageConstants';
import { PermissionGrantCoverage } from '../../types/permission';

export type PermissionFilterValue =
  (typeof PERMISSION_FILTER_VALUE)[keyof typeof PERMISSION_FILTER_VALUE];
export type PermissionStatusFilter =
  (typeof PERMISSION_STATUS_FILTER)[keyof typeof PERMISSION_STATUS_FILTER];
export type PermissionEditorMode =
  (typeof PERMISSION_EDITOR_MODE)[keyof typeof PERMISSION_EDITOR_MODE];

export interface PermissionFilterFormState {
  searchText: string;
  coverage: PermissionGrantCoverage | PermissionFilterValue;
  connectionId: string | PermissionFilterValue;
  status: PermissionStatusFilter;
}

export interface PermissionFormState {
  connectionId: string;
  coverage: PermissionGrantCoverage;
  catalogName: string;
  schemaName: string;
  enabled: boolean;
}

export interface PermissionFormErrors {
  conversationId?: string;
  connectionId?: string;
  catalogName?: string;
  schemaName?: string;
}

export interface PermissionConnectionOption {
  id: number;
  name: string;
}

export interface PermissionConversationOption {
  id: number;
  label: string;
}
