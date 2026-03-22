import type { TFunction } from 'i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { MemoryListItemView } from './components/MemoryListPanel';
import {
  MEMORY_DEFAULT_MANUAL_SCOPE,
  MEMORY_DEFAULT_SOURCE_TYPE,
  MEMORY_DEFAULT_TYPE,
  MEMORY_DEFAULT_CONFIDENCE_SCORE,
  MEMORY_DEFAULT_SALIENCE_SCORE,
  MEMORY_DIALOG_MODE,
  MEMORY_SEARCH_RESULT_TONE_CLASS_NAME,
  MEMORY_STATUS_TONE_CLASS_NAMES,
  MEMORY_WORKSPACE_SCOPE,
} from './memoryPageConstants';
import {
  getDefaultMemorySubtype,
  MEMORY_SCOPE_OPTIONS,
  MEMORY_SOURCE_TYPE_OPTIONS,
  MEMORY_STATUS,
  MEMORY_SUBTYPE_OPTIONS_BY_TYPE,
  MEMORY_TYPE_OPTIONS,
  MEMORY_WORKSPACE_LEVEL_OPTIONS,
  type Memory,
  type MemoryCreateRequest,
  type MemoryListParams,
  type MemoryMaintenanceReport,
  type MemoryMetadataResponse,
  type MemoryMetadataTypeItem,
  type MemoryPage,
  type MemorySearchResult,
  type MemoryScope,
  type MemorySubType,
  type MemoryType,
} from '../../types/memory';
import type { FilterFormState, MemoryDialogMode, MemoryFormState } from './memoryPageModels';

export const DEFAULT_MEMORY_PAGE_SIZE = 20;

export const defaultMemoryPage: MemoryPage = {
  current: 1,
  size: DEFAULT_MEMORY_PAGE_SIZE,
  total: 0,
  pages: 0,
  records: [],
};

export const defaultMemoryListParams: MemoryListParams = {
  current: 1,
  size: DEFAULT_MEMORY_PAGE_SIZE,
  status: MEMORY_STATUS.ACTIVE,
};

export const emptyMaintenanceReport: MemoryMaintenanceReport = {
  generatedAt: '',
  activeMemoryCount: 0,
  archivedMemoryCount: 0,
  hiddenMemoryCount: 0,
  expiredActiveMemoryCount: 0,
  duplicateActiveMemoryCount: 0,
  processedArchivedCount: 0,
  processedHiddenCount: 0,
};

export const defaultFilterFormState: FilterFormState = {
  keyword: '',
  memoryType: '',
  scope: '',
  status: String(MEMORY_STATUS.ACTIVE),
};

export const createEmptyMemoryFormState = (): MemoryFormState => ({
  conversationId: '',
  memoryType: MEMORY_DEFAULT_TYPE,
  subType: getDefaultMemorySubtype(MEMORY_DEFAULT_TYPE),
  scope: MEMORY_DEFAULT_MANUAL_SCOPE,
  sourceType: MEMORY_DEFAULT_SOURCE_TYPE,
  title: '',
  reason: '',
  content: '',
  detailJson: '{}',
  confidenceScore: MEMORY_DEFAULT_CONFIDENCE_SCORE,
  salienceScore: MEMORY_DEFAULT_SALIENCE_SCORE,
  expiresAt: '',
});

export const buildFallbackMemoryMetadata = (): MemoryMetadataResponse => ({
  scopes: [...MEMORY_SCOPE_OPTIONS],
  workspaceLevels: [...MEMORY_WORKSPACE_LEVEL_OPTIONS],
  sourceTypes: [...MEMORY_SOURCE_TYPE_OPTIONS],
  memoryTypes: MEMORY_TYPE_OPTIONS.map((code) => ({
    code,
    subTypes: [...MEMORY_SUBTYPE_OPTIONS_BY_TYPE[code]],
  })),
});

export const normalizeOptionList = <T extends string>(values: unknown, fallback: readonly T[]): T[] => {
  if (!Array.isArray(values)) {
    return [...fallback];
  }
  const normalized = values
    .map((item) => (typeof item === 'string' ? item.trim().toUpperCase() : ''))
    .filter((item): item is T => item.length > 0);
  return normalized.length > 0 ? Array.from(new Set(normalized)) : [...fallback];
};

export const normalizeMemoryMetadata = (metadata: MemoryMetadataResponse): MemoryMetadataResponse => {
  const fallback = buildFallbackMemoryMetadata();
  const normalizedTypes = Array.isArray(metadata.memoryTypes)
    ? metadata.memoryTypes
        .map((item) => {
          const code = typeof item.code === 'string' ? item.code.trim().toUpperCase() : '';
          if (!code) {
            return null;
          }
          const fallbackSubTypes = MEMORY_TYPE_OPTIONS.includes(code as (typeof MEMORY_TYPE_OPTIONS)[number])
            ? MEMORY_SUBTYPE_OPTIONS_BY_TYPE[code as (typeof MEMORY_TYPE_OPTIONS)[number]]
            : [];
          return {
            code,
            subTypes: normalizeOptionList(item.subTypes, fallbackSubTypes),
          };
        })
        .filter((item): item is NonNullable<typeof item> => item != null)
    : [];

  return {
    scopes: normalizeOptionList(metadata.scopes, fallback.scopes),
    workspaceLevels: normalizeOptionList(metadata.workspaceLevels, fallback.workspaceLevels),
    sourceTypes: normalizeOptionList(metadata.sourceTypes, fallback.sourceTypes),
    memoryTypes: normalizedTypes.length > 0 ? normalizedTypes : fallback.memoryTypes,
  };
};

export const appendOptionIfMissing = <T extends string>(options: readonly T[], value?: string | null): T[] => {
  if (!value) {
    return [...options];
  }
  return options.includes(value as T) ? [...options] : [...options, value as T];
};

export const buildSubTypeOptionsByType = (memoryTypes: readonly MemoryMetadataTypeItem[]) =>
  memoryTypes.reduce<Record<string, MemorySubType[]>>((acc, item) => {
    acc[item.code] = [...item.subTypes];
    return acc;
  }, {});

export const buildFilterParams = (form: FilterFormState, size: number): MemoryListParams => ({
  current: 1,
  size,
  keyword: form.keyword || undefined,
  memoryType: form.memoryType || undefined,
  scope: form.scope || undefined,
  status: form.status === '' ? undefined : Number(form.status),
});

export const toDateTimeLocal = (value?: string | null): string => (value ? value.slice(0, 16) : '');

export const toBackendDateTime = (value: string): string | null => {
  if (!value) {
    return null;
  }
  return value.length === 16 ? `${value}:00` : value;
};

export const mapMemoryToFormState = (memory: Memory): MemoryFormState => ({
  conversationId: memory.conversationId == null ? '' : String(memory.conversationId),
  memoryType: memory.memoryType || MEMORY_DEFAULT_TYPE,
  subType: memory.subType || '',
  scope: memory.scope || MEMORY_DEFAULT_MANUAL_SCOPE,
  sourceType: memory.sourceType || MEMORY_DEFAULT_SOURCE_TYPE,
  title: memory.title || '',
  reason: memory.reason || '',
  content: memory.content || '',
  detailJson: memory.detailJson || '{}',
  confidenceScore: memory.confidenceScore == null ? '' : String(memory.confidenceScore),
  salienceScore: memory.salienceScore == null ? '' : String(memory.salienceScore),
  expiresAt: toDateTimeLocal(memory.expiresAt),
});

export const buildMemoryPayload = (form: MemoryFormState): MemoryCreateRequest => ({
  conversationId: form.conversationId.trim() ? Number(form.conversationId.trim()) : undefined,
  memoryType: form.memoryType,
  subType: form.subType || undefined,
  scope: form.scope,
  sourceType: form.sourceType,
  title: form.title.trim() || undefined,
  reason: form.reason.trim() || undefined,
  content: form.content.trim(),
  detailJson: form.detailJson.trim() || '{}',
  confidenceScore: form.confidenceScore.trim() ? Number(form.confidenceScore) : undefined,
  salienceScore: form.salienceScore.trim() ? Number(form.salienceScore) : undefined,
  expiresAt: toBackendDateTime(form.expiresAt),
});

export const validateMemoryForm = (
  form: MemoryFormState,
  dialogMode: MemoryDialogMode,
  subTypeOptionsByType: Record<string, MemorySubType[]>,
  t: TFunction,
): Record<string, string> => {
  const errors: Record<string, string> = {};
  const subTypesForType = subTypeOptionsByType[form.memoryType] ?? [];

  if (!form.memoryType.trim()) {
    errors.memoryType = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_REQUIRED_TYPE);
  }
  if (!form.subType.trim()) {
    errors.subType = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_REQUIRED_SUB_TYPE);
  } else if (subTypesForType.length > 0 && !subTypesForType.includes(form.subType)) {
    errors.subType = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_INVALID_SUB_TYPE);
  }
  if (dialogMode === MEMORY_DIALOG_MODE.CREATE && form.scope === MEMORY_WORKSPACE_SCOPE) {
    errors.scope = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_MANUAL_WORKSPACE_SCOPE);
  }
  if (!form.content.trim()) {
    errors.content = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_REQUIRED_CONTENT);
  }
  if (form.conversationId.trim() && Number.isNaN(Number(form.conversationId.trim()))) {
    errors.conversationId = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_CONVERSATION);
  }

  const numericScores = [form.confidenceScore, form.salienceScore]
    .filter(Boolean)
    .map((item) => Number(item));
  if (numericScores.some((item) => Number.isNaN(item) || item < 0 || item > 1)) {
    errors.confidenceScore = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_SCORE_RANGE);
    errors.salienceScore = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_SCORE_RANGE);
  }

  if (form.detailJson.trim()) {
    try {
      JSON.parse(form.detailJson);
    } catch {
      errors.detailJson = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_JSON);
    }
  }

  return errors;
};

export const buildMemoryListItems = (
  pageRecords: Memory[],
  semanticResults: MemorySearchResult[] | null,
  t: TFunction,
): MemoryListItemView[] => {
  if (semanticResults != null) {
    return semanticResults.map((result) => ({
      id: result.id,
      title: getMemoryOptionLabel(t, result.memoryType),
      summary: result.content,
      tags: [getMemoryOptionLabel(t, result.memoryType)],
      statusLabel: `${t(I18N_KEYS.MEMORY_PAGE.SEARCH_SCORE)} ${result.score.toFixed(3)}`,
      statusToneClassName: MEMORY_SEARCH_RESULT_TONE_CLASS_NAME,
      sourceLabel: result.conversationId
        ? `${t(I18N_KEYS.MEMORY_PAGE.META_CONVERSATION)}: ${result.conversationId}`
        : undefined,
    }));
  }

  return pageRecords.map((memory) => ({
    id: memory.id,
    title: memory.title || getMemoryOptionLabel(t, memory.memoryType),
    summary: memory.content,
    tags: [
      getMemoryOptionLabel(t, memory.memoryType),
      getMemoryOptionLabel(t, memory.scope),
      ...(memory.subType ? [getMemoryOptionLabel(t, memory.subType)] : []),
    ],
    statusLabel: t(getStatusLabelKey(memory.status)),
    statusToneClassName: getStatusToneClassName(memory.status),
    sourceLabel: `${t(I18N_KEYS.MEMORY_PAGE.META_SOURCE)}: ${getMemoryOptionLabel(t, memory.sourceType)}`,
    workspaceBindingLabel: memory.scope === MEMORY_WORKSPACE_SCOPE
      ? `${t(I18N_KEYS.MEMORY_PAGE.META_WORKSPACE_BINDING)}: ${formatWorkspaceBindingLabel(t, memory)}`
      : undefined,
    updatedAtLabel: `${t(I18N_KEYS.MEMORY_PAGE.META_UPDATED)}: ${formatDateTime(memory.updatedAt)}`,
  }));
};

export const getInitialCreateMemoryFormState = (
  metadata: MemoryMetadataResponse,
  subTypeOptionsByType: Record<string, MemorySubType[]>,
  manualScopeOptions: readonly MemoryScope[],
): MemoryFormState => {
  const nextMemoryType = metadata.memoryTypes[0]?.code ?? MEMORY_DEFAULT_TYPE;
  const nextSubType = subTypeOptionsByType[nextMemoryType]?.[0] ?? getDefaultMemorySubtype(nextMemoryType);
  const nextScope = manualScopeOptions[0] ?? MEMORY_DEFAULT_MANUAL_SCOPE;
  const nextSourceType = metadata.sourceTypes[0] ?? MEMORY_DEFAULT_SOURCE_TYPE;
  return {
    ...createEmptyMemoryFormState(),
    memoryType: nextMemoryType,
    subType: nextSubType,
    scope: nextScope,
    sourceType: nextSourceType,
  };
};

export const formatLabel = (value?: string | null): string => {
  if (!value) {
    return '--';
  }
  return value
    .toLowerCase()
    .split('_')
    .map((item) => item.charAt(0).toUpperCase() + item.slice(1))
    .join(' ');
};

export const getMemoryOptionLabel = (t: TFunction, value?: string | null): string => {
  if (!value) {
    return '--';
  }
  return t(`memoryOption.${value.toLowerCase()}`, {
    defaultValue: formatLabel(value),
  });
};

export const formatDateTime = (value?: string | null): string => {
  if (!value) {
    return '--';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

export const getStatusToneClassName = (status: number): string => {
  return MEMORY_STATUS_TONE_CLASS_NAMES[status as keyof typeof MEMORY_STATUS_TONE_CLASS_NAMES]
    ?? MEMORY_STATUS_TONE_CLASS_NAMES[MEMORY_STATUS.ACTIVE];
};

export const getStatusLabelKey = (status: number): string => {
  switch (status) {
    case MEMORY_STATUS.ARCHIVED:
      return I18N_KEYS.MEMORY_PAGE.STATUS_ARCHIVED;
    case MEMORY_STATUS.HIDDEN:
      return I18N_KEYS.MEMORY_PAGE.STATUS_HIDDEN;
    default:
      return I18N_KEYS.MEMORY_PAGE.STATUS_ACTIVE;
  }
};

export const formatWorkspaceBinding = (memory?: Memory | null): string => {
  if (!memory || memory.scope !== MEMORY_WORKSPACE_SCOPE) {
    return '--';
  }
  const level = formatLabel(memory.workspaceLevel);
  const key = memory.workspaceContextKey || '--';
  return `${level} / ${key}`;
};

export const formatWorkspaceBindingLabel = (t: TFunction, memory?: Memory | null): string => {
  if (!memory || memory.scope !== MEMORY_WORKSPACE_SCOPE) {
    return '--';
  }
  const level = getMemoryOptionLabel(t, memory.workspaceLevel);
  const key = memory.workspaceContextKey || '--';
  return `${level} / ${key}`;
};

export const getDefaultMemorySubtypeByType = (memoryType?: MemoryType | null): MemorySubType | '' =>
  getDefaultMemorySubtype(memoryType);

export const MEMORY_FORM_SELECT_CLASS_NAME =
  'flex h-10 w-full rounded-xl border theme-border theme-bg-main px-3 py-2 text-sm theme-text-primary';

export const MEMORY_FORM_TEXTAREA_CLASS_NAME =
  'min-h-[140px] w-full rounded-xl border theme-border theme-bg-main px-3 py-2 text-sm theme-text-primary';
