import { PermissionGrantCoverage } from '../../types/permission';

export const PERMISSION_FILTER_VALUE = {
  ALL: 'ALL',
} as const;

export const PERMISSION_STATUS_FILTER = {
  ALL: 'ALL',
  ENABLED: 'ENABLED',
  DISABLED: 'DISABLED',
} as const;

export const PERMISSION_EDITOR_MODE = {
  CLOSED: 'closed',
  CREATE: 'create',
  EDIT: 'edit',
} as const;

export const PERMISSION_FORM_SELECT_CLASS_NAME = [
  'h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
  'disabled:cursor-not-allowed disabled:opacity-50',
].join(' ');

export const PERMISSION_COVERAGE_OPTIONS = [
  PermissionGrantCoverage.EXACT_TARGET,
  PermissionGrantCoverage.DATABASE,
  PermissionGrantCoverage.CONNECTION,
] as const;

export const PERMISSION_CONVERSATION_LIST_PAGE_SIZE = 100;
