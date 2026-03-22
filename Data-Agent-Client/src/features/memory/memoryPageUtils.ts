import type { TFunction } from 'i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { MemoryListItemView } from './components/MemoryListPanel';
import {
  MEMORY_DEFAULT_MANUAL_SCOPE,
  MEMORY_DEFAULT_SOURCE_TYPE,
  MEMORY_DEFAULT_TYPE,
  MEMORY_ENABLE_TONE_CLASS_NAMES,
  MEMORY_SEARCH_RESULT_TONE_CLASS_NAME,
} from './memoryPageConstants';
import {
  getDefaultMemorySubtype,
  MEMORY_ENABLE,
  MEMORY_SCOPE_OPTIONS,
  MEMORY_SOURCE_TYPE_OPTIONS,
  MEMORY_SUBTYPE_OPTIONS_BY_TYPE,
  MEMORY_TYPE_OPTIONS,
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
  enable: MEMORY_ENABLE.ENABLE,
};

export const emptyMaintenanceReport: MemoryMaintenanceReport = {
  generatedAt: '',
  enabledMemoryCount: 0,
  disabledMemoryCount: 0,
  duplicateEnabledMemoryCount: 0,
  processedDisabledCount: 0,
};

export const defaultFilterFormState: FilterFormState = {
  keyword: '',
  memoryType: '',
  scope: '',
  enable: String(MEMORY_ENABLE.ENABLE),
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
});

export const buildFallbackMemoryMetadata = (): MemoryMetadataResponse => ({
  scopes: [...MEMORY_SCOPE_OPTIONS],
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
  enable: Number(form.enable),
});

export const mapMemoryToFormState = (memory: Memory): MemoryFormState => ({
  conversationId: memory.conversationId == null ? '' : String(memory.conversationId),
  memoryType: memory.memoryType || MEMORY_DEFAULT_TYPE,
  subType: memory.subType || '',
  scope: memory.scope || MEMORY_DEFAULT_MANUAL_SCOPE,
  sourceType: memory.sourceType || MEMORY_DEFAULT_SOURCE_TYPE,
  title: memory.title || '',
  reason: memory.reason || '',
  content: memory.content || '',
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
});

export const validateMemoryForm = (
  form: MemoryFormState,
  _dialogMode: MemoryDialogMode,
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
  if (!form.content.trim()) {
    errors.content = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_REQUIRED_CONTENT);
  }
  if (form.conversationId.trim() && Number.isNaN(Number(form.conversationId.trim()))) {
    errors.conversationId = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_CONVERSATION);
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
    statusLabel: t(getEnableLabelKey(memory.enable)),
    statusToneClassName: getEnableToneClassName(memory.enable),
    sourceLabel: `${t(I18N_KEYS.MEMORY_PAGE.META_SOURCE)}: ${getMemoryOptionLabel(t, memory.sourceType)}`,
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

export const getEnableToneClassName = (enable: number): string => {
  return MEMORY_ENABLE_TONE_CLASS_NAMES[enable as keyof typeof MEMORY_ENABLE_TONE_CLASS_NAMES]
    ?? MEMORY_ENABLE_TONE_CLASS_NAMES[MEMORY_ENABLE.ENABLE];
};

export const getEnableLabelKey = (enable: number): string => {
  return enable === MEMORY_ENABLE.DISABLE
    ? I18N_KEYS.MEMORY_PAGE.STATUS_DISABLED
    : I18N_KEYS.MEMORY_PAGE.STATUS_ENABLED;
};

export const getDefaultMemorySubtypeByType = (memoryType?: MemoryType | null): MemorySubType | '' =>
  getDefaultMemorySubtype(memoryType);

export const MEMORY_FORM_SELECT_CLASS_NAME =
  'flex h-10 w-full rounded-xl border theme-border theme-bg-main px-3 py-2 text-sm theme-text-primary';

export const MEMORY_FORM_TEXTAREA_CLASS_NAME =
  'min-h-[140px] w-full rounded-xl border theme-border theme-bg-main px-3 py-2 text-sm theme-text-primary';
