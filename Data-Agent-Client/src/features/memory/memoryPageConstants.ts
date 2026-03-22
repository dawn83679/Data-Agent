import {
  MEMORY_ENABLE,
  MEMORY_MANUAL_SCOPE_OPTIONS,
  MEMORY_SOURCE_TYPE_OPTIONS,
  MEMORY_TYPE_OPTIONS,
} from '../../types/memory';

export const MEMORY_DIALOG_MODE = {
  CLOSED: 'closed',
  CREATE: 'create',
  EDIT: 'edit',
} as const;

export const MEMORY_SEMANTIC_SEARCH_LIMIT = 8;
export const MEMORY_SEMANTIC_SEARCH_MIN_SCORE = 0.6;
export const MEMORY_DEFAULT_TYPE = MEMORY_TYPE_OPTIONS[0];
export const MEMORY_DEFAULT_MANUAL_SCOPE = MEMORY_MANUAL_SCOPE_OPTIONS[0];
export const MEMORY_DEFAULT_SOURCE_TYPE = MEMORY_SOURCE_TYPE_OPTIONS[0];
export const MEMORY_DETAIL_CONTENT_MIN_HEIGHT_CLASS = 'min-h-[150px]';

export const MEMORY_ENABLE_TONE_CLASS_NAMES = {
  [MEMORY_ENABLE.ENABLE]: 'bg-emerald-500/12 text-emerald-300 border-emerald-500/25',
  [MEMORY_ENABLE.DISABLE]: 'bg-slate-500/12 text-slate-300 border-slate-500/25',
} as const;

export const MEMORY_SEARCH_RESULT_TONE_CLASS_NAME = 'border-sky-500/25 bg-sky-500/12 text-sky-300';
