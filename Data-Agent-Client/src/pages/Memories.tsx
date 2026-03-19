import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Archive,
  ArrowLeft,
  Brain,
  Clock3,
  Plus,
  RefreshCcw,
  RotateCcw,
  Search,
  Trash2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { I18N_KEYS } from '../constants/i18nKeys';
import { useToast } from '../hooks/useToast';
import { resolveErrorMessage } from '../lib/errorMessage';
import { cn } from '../lib/utils';
import { memoryService } from '../services/memory.service';
import {
  getDefaultMemorySubtype,
  MEMORY_REVIEW_STATE_OPTIONS,
  MEMORY_SCOPE_OPTIONS,
  MEMORY_SOURCE_TYPE_OPTIONS,
  MEMORY_STATUS,
  MEMORY_SUBTYPE_OPTIONS_BY_TYPE,
  MEMORY_TYPE_OPTIONS,
  MEMORY_WORKSPACE_LEVEL_OPTIONS,
  type Memory,
  type MemoryCreateRequest,
  type MemoryListParams,
  type MemoryMetadataResponse,
  type MemoryMaintenanceReport,
  type MemoryPage,
  type MemoryReviewState,
  type MemoryScope,
  type MemorySearchResult,
  type MemorySourceType,
  type MemorySubType,
  type MemoryMetadataTypeItem,
  type MemoryType,
} from '../types/memory';

interface FilterFormState {
  keyword: string;
  memoryType: MemoryType | '';
  scope: MemoryScope | '';
  reviewState: MemoryReviewState | '';
  status: string;
}

interface MemoryFormState {
  conversationId: string;
  memoryType: MemoryType;
  subType: MemorySubType | '';
  scope: MemoryScope;
  reviewState: MemoryReviewState;
  sourceType: MemorySourceType;
  title: string;
  reason: string;
  content: string;
  detailJson: string;
  confidenceScore: string;
  salienceScore: string;
  expiresAt: string;
}

const defaultPage: MemoryPage = {
  current: 1,
  size: 20,
  total: 0,
  pages: 0,
  records: [],
};

const emptyMaintenance: MemoryMaintenanceReport = {
  generatedAt: '',
  activeMemoryCount: 0,
  archivedMemoryCount: 0,
  hiddenMemoryCount: 0,
  expiredActiveMemoryCount: 0,
  duplicateActiveMemoryCount: 0,
  processedArchivedCount: 0,
  processedHiddenCount: 0,
};

const buildFallbackMemoryMetadata = (): MemoryMetadataResponse => ({
  scopes: [...MEMORY_SCOPE_OPTIONS],
  workspaceLevels: [...MEMORY_WORKSPACE_LEVEL_OPTIONS],
  reviewStates: [...MEMORY_REVIEW_STATE_OPTIONS],
  sourceTypes: [...MEMORY_SOURCE_TYPE_OPTIONS],
  memoryTypes: MEMORY_TYPE_OPTIONS.map((code) => ({
    code,
    subTypes: [...MEMORY_SUBTYPE_OPTIONS_BY_TYPE[code]],
  })),
});

const normalizeOptionList = <T extends string>(values: unknown, fallback: readonly T[]): T[] => {
  if (!Array.isArray(values)) {
    return [...fallback];
  }
  const normalized = values
    .map((item) => (typeof item === 'string' ? item.trim().toUpperCase() : ''))
    .filter((item): item is T => item.length > 0);
  return normalized.length > 0 ? Array.from(new Set(normalized)) : [...fallback];
};

const normalizeMemoryMetadata = (metadata: MemoryMetadataResponse): MemoryMetadataResponse => {
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
    reviewStates: normalizeOptionList(metadata.reviewStates, fallback.reviewStates),
    sourceTypes: normalizeOptionList(metadata.sourceTypes, fallback.sourceTypes),
    memoryTypes: normalizedTypes.length > 0 ? normalizedTypes : fallback.memoryTypes,
  };
};

const appendOptionIfMissing = <T extends string>(options: readonly T[], value?: string | null): T[] => {
  if (!value) {
    return [...options];
  }
  return options.includes(value as T) ? [...options] : [...options, value as T];
};

const buildSubTypeOptionsByType = (memoryTypes: readonly MemoryMetadataTypeItem[]) =>
  memoryTypes.reduce<Record<string, MemorySubType[]>>((acc, item) => {
    acc[item.code] = [...item.subTypes];
    return acc;
  }, {});

const buildFilterParams = (form: FilterFormState, size: number): MemoryListParams => ({
  current: 1,
  size,
  keyword: form.keyword || undefined,
  memoryType: form.memoryType || undefined,
  scope: form.scope || undefined,
  reviewState: form.reviewState || undefined,
  status: form.status === '' ? undefined : Number(form.status),
});

const defaultFilters: FilterFormState = {
  keyword: '',
  memoryType: '',
  scope: '',
  reviewState: '',
  status: String(MEMORY_STATUS.ACTIVE),
};

const emptyForm = (): MemoryFormState => ({
  conversationId: '',
  memoryType: 'PREFERENCE',
  subType: getDefaultMemorySubtype('PREFERENCE'),
  scope: 'USER',
  reviewState: 'USER_CONFIRMED',
  sourceType: 'MANUAL',
  title: '',
  reason: '',
  content: '',
  detailJson: '{}',
  confidenceScore: '0.9',
  salienceScore: '0.6',
  expiresAt: '',
});

const mapMemoryToForm = (memory: Memory): MemoryFormState => ({
  conversationId: memory.conversationId == null ? '' : String(memory.conversationId),
  memoryType: memory.memoryType || 'PREFERENCE',
  subType: memory.subType || '',
  scope: memory.scope || 'USER',
  reviewState: memory.reviewState || 'USER_CONFIRMED',
  sourceType: memory.sourceType || 'MANUAL',
  title: memory.title || '',
  reason: memory.reason || '',
  content: memory.content || '',
  detailJson: memory.detailJson || '{}',
  confidenceScore: memory.confidenceScore == null ? '' : String(memory.confidenceScore),
  salienceScore: memory.salienceScore == null ? '' : String(memory.salienceScore),
  expiresAt: toDateTimeLocal(memory.expiresAt),
});

const toDateTimeLocal = (value?: string | null) => (value ? value.slice(0, 16) : '');

const toBackendDateTime = (value: string) => {
  if (!value) {
    return null;
  }
  return value.length === 16 ? `${value}:00` : value;
};

const formatLabel = (value?: string | null) => {
  if (!value) {
    return '--';
  }
  return value
    .toLowerCase()
    .split('_')
    .map((item) => item.charAt(0).toUpperCase() + item.slice(1))
    .join(' ');
};

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '--';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const getStatusTone = (status: number) => {
  switch (status) {
    case MEMORY_STATUS.ARCHIVED:
      return 'bg-amber-100 text-amber-700 border-amber-200';
    case MEMORY_STATUS.HIDDEN:
      return 'bg-slate-200 text-slate-700 border-slate-300';
    default:
      return 'bg-emerald-100 text-emerald-700 border-emerald-200';
  }
};

const getStatusLabelKey = (status: number) => {
  switch (status) {
    case MEMORY_STATUS.ARCHIVED:
      return I18N_KEYS.MEMORY_PAGE.STATUS_ARCHIVED;
    case MEMORY_STATUS.HIDDEN:
      return I18N_KEYS.MEMORY_PAGE.STATUS_HIDDEN;
    default:
      return I18N_KEYS.MEMORY_PAGE.STATUS_ACTIVE;
  }
};

const formatWorkspaceBinding = (memory?: Memory | null) => {
  if (!memory || memory.scope !== 'WORKSPACE') {
    return '--';
  }
  const level = formatLabel(memory.workspaceLevel);
  const key = memory.workspaceContextKey || '--';
  return `${level} / ${key}`;
};

export default function Memories() {
  const { t } = useTranslation();
  const toast = useToast();
  const navigate = useNavigate();

  const [page, setPage] = useState<MemoryPage>(defaultPage);
  const [filters, setFilters] = useState<MemoryListParams>({ current: 1, size: 20, status: MEMORY_STATUS.ACTIVE });
  const [filterForm, setFilterForm] = useState<FilterFormState>(defaultFilters);
  const [memoryForm, setMemoryForm] = useState<MemoryFormState>(emptyForm);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [selectedMemoryId, setSelectedMemoryId] = useState<number | null>(null);
  const [selectedMemory, setSelectedMemory] = useState<Memory | null>(null);
  const [semanticQuery, setSemanticQuery] = useState('');
  const [semanticResults, setSemanticResults] = useState<MemorySearchResult[]>([]);
  const [maintenance, setMaintenance] = useState<MemoryMaintenanceReport>(emptyMaintenance);
  const [listLoading, setListLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [semanticLoading, setSemanticLoading] = useState(false);
  const [maintenanceLoading, setMaintenanceLoading] = useState(false);
  const [metadataLoading, setMetadataLoading] = useState(false);
  const [memoryMetadata, setMemoryMetadata] = useState<MemoryMetadataResponse>(buildFallbackMemoryMetadata);

  const isEditing = selectedMemoryId != null;
  const subTypeOptionsByType = useMemo(
    () => buildSubTypeOptionsByType(memoryMetadata.memoryTypes),
    [memoryMetadata.memoryTypes]
  );
  const memoryTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.memoryTypes.map((item) => item.code), memoryForm.memoryType),
    [memoryForm.memoryType, memoryMetadata.memoryTypes]
  );
  const filterMemoryTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryTypeOptions, filterForm.memoryType),
    [filterForm.memoryType, memoryTypeOptions]
  );
  const scopeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.scopes, memoryForm.scope),
    [memoryForm.scope, memoryMetadata.scopes]
  );
  const filterScopeOptions = useMemo(
    () => appendOptionIfMissing(scopeOptions, filterForm.scope),
    [filterForm.scope, scopeOptions]
  );
  const reviewStateOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.reviewStates, memoryForm.reviewState),
    [memoryForm.reviewState, memoryMetadata.reviewStates]
  );
  const filterReviewStateOptions = useMemo(
    () => appendOptionIfMissing(reviewStateOptions, filterForm.reviewState),
    [filterForm.reviewState, reviewStateOptions]
  );
  const sourceTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.sourceTypes, memoryForm.sourceType),
    [memoryForm.sourceType, memoryMetadata.sourceTypes]
  );
  const manualScopeOptions = useMemo(() => {
    const options = scopeOptions.filter((option) => option !== 'WORKSPACE');
    return options.length > 0 ? options : (['USER'] as MemoryScope[]);
  }, [scopeOptions]);
  const availableSubTypeOptions = useMemo(() => {
    const baseOptions = subTypeOptionsByType[memoryForm.memoryType] ?? [];
    return appendOptionIfMissing(baseOptions, memoryForm.subType);
  }, [memoryForm.memoryType, memoryForm.subType, subTypeOptionsByType]);
  const editorScopeOptions = useMemo(() => {
    if (isEditing && selectedMemory?.scope === 'WORKSPACE') {
      return ['WORKSPACE'] as MemoryScope[];
    }
    return manualScopeOptions;
  }, [isEditing, manualScopeOptions, selectedMemory?.scope]);

  const pageSummary = useMemo(() => {
    if (page.total === 0) {
      return '0 / 0';
    }
    const start = (page.current - 1) * page.size + 1;
    const end = Math.min(page.current * page.size, page.total);
    return `${start}-${end} / ${page.total}`;
  }, [page]);

  const loadMemories = useCallback(async () => {
    setListLoading(true);
    try {
      const nextPage = await memoryService.list(filters);
      setPage(nextPage);
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.LOAD_FAILED)));
    } finally {
      setListLoading(false);
    }
  }, [filters, t, toast]);

  useEffect(() => {
    void loadMemories();
  }, [loadMemories]);

  const loadMaintenanceSummary = useCallback(async () => {
    setMaintenanceLoading(true);
    try {
      const summary = await memoryService.getMaintenanceSummary();
      setMaintenance(summary);
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_SUMMARY_FAILED)));
    } finally {
      setMaintenanceLoading(false);
    }
  }, [t, toast]);

  useEffect(() => {
    void loadMaintenanceSummary();
  }, [loadMaintenanceSummary]);

  const loadMemoryMetadata = useCallback(async () => {
    setMetadataLoading(true);
    try {
      const metadata = await memoryService.getMetadata();
      setMemoryMetadata(normalizeMemoryMetadata(metadata));
    } catch (error) {
      setMemoryMetadata(buildFallbackMemoryMetadata());
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.METADATA_LOAD_FAILED)));
    } finally {
      setMetadataLoading(false);
    }
  }, [t, toast]);

  useEffect(() => {
    void loadMemoryMetadata();
  }, [loadMemoryMetadata]);

  useEffect(() => {
    const allowedFilterMemoryTypes = memoryMetadata.memoryTypes.map((item) => item.code);
    const allowedScopes = memoryMetadata.scopes;
    const allowedReviewStates = memoryMetadata.reviewStates;
    const allowedSourceTypes = memoryMetadata.sourceTypes;

    setFilterForm((prev) => ({
      ...prev,
      memoryType: prev.memoryType && !allowedFilterMemoryTypes.includes(prev.memoryType) ? '' : prev.memoryType,
      scope: prev.scope && !allowedScopes.includes(prev.scope) ? '' : prev.scope,
      reviewState: prev.reviewState && !allowedReviewStates.includes(prev.reviewState) ? '' : prev.reviewState,
    }));
    setFilters((prev) => ({
      ...prev,
      memoryType: prev.memoryType && !allowedFilterMemoryTypes.includes(prev.memoryType) ? undefined : prev.memoryType,
      scope: prev.scope && !allowedScopes.includes(prev.scope) ? undefined : prev.scope,
      reviewState: prev.reviewState && !allowedReviewStates.includes(prev.reviewState) ? undefined : prev.reviewState,
    }));

    setMemoryForm((prev) => {
      const nextMemoryType = allowedFilterMemoryTypes.includes(prev.memoryType)
        ? prev.memoryType
        : allowedFilterMemoryTypes[0] ?? 'PREFERENCE';
      const nextSubTypeOptions = subTypeOptionsByType[nextMemoryType] ?? [];
      const nextSubType = prev.subType && nextSubTypeOptions.includes(prev.subType)
        ? prev.subType
        : nextSubTypeOptions[0] ?? '';
      const nextScope = (isEditing && selectedMemory?.scope === 'WORKSPACE')
        ? 'WORKSPACE'
        : allowedScopes.includes(prev.scope) && prev.scope !== 'WORKSPACE'
          ? prev.scope
          : allowedScopes.find((scope) => scope !== 'WORKSPACE') ?? 'USER';
      const nextReviewState = allowedReviewStates.includes(prev.reviewState)
        ? prev.reviewState
        : allowedReviewStates[0] ?? 'USER_CONFIRMED';
      const nextSourceType = allowedSourceTypes.includes(prev.sourceType)
        ? prev.sourceType
        : allowedSourceTypes[0] ?? 'MANUAL';
      if (
        prev.memoryType === nextMemoryType &&
        prev.subType === nextSubType &&
        prev.scope === nextScope &&
        prev.reviewState === nextReviewState &&
        prev.sourceType === nextSourceType
      ) {
        return prev;
      }
      return {
        ...prev,
        memoryType: nextMemoryType,
        subType: nextSubType,
        scope: nextScope,
        reviewState: nextReviewState,
        sourceType: nextSourceType,
      };
    });
  }, [isEditing, memoryMetadata, selectedMemory?.scope, subTypeOptionsByType]);

  const handleFilterInputChange =
    (field: keyof FilterFormState) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setFilterForm((prev) => ({ ...prev, [field]: event.target.value }));
    };

  const handleFormInputChange =
    (field: keyof MemoryFormState) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
      const nextValue = event.target.value;

      setMemoryForm((prev) => {
        if (field === 'memoryType') {
          const nextMemoryType = nextValue.trim() ? nextValue.toUpperCase() : prev.memoryType;
          const nextSubTypeOptions = subTypeOptionsByType[nextMemoryType] ?? [];
          const nextSubType = prev.subType && nextSubTypeOptions.includes(prev.subType)
            ? prev.subType
            : nextSubTypeOptions[0] ?? getDefaultMemorySubtype(nextMemoryType);
          return { ...prev, memoryType: nextMemoryType, subType: nextSubType };
        }

        return { ...prev, [field]: nextValue };
      });

      if (formErrors[field] || (field === 'memoryType' && formErrors.subType)) {
        setFormErrors((prev) => ({
          ...prev,
          [field]: '',
          ...(field === 'memoryType' ? { subType: '' } : {}),
        }));
      }
    };

  const openMemory = useCallback(
    async (id: number) => {
      setDetailLoading(true);
      try {
        const memory = await memoryService.getById(id);
        setSelectedMemoryId(id);
        setSelectedMemory(memory);
        setMemoryForm(mapMemoryToForm(memory));
        setFormErrors({});
      } catch (error) {
        toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.LOAD_FAILED)));
      } finally {
        setDetailLoading(false);
      }
    },
    [t, toast]
  );

  const resetEditor = () => {
    const nextMemoryType = memoryMetadata.memoryTypes[0]?.code ?? 'PREFERENCE';
    const nextSubType = subTypeOptionsByType[nextMemoryType]?.[0] ?? getDefaultMemorySubtype(nextMemoryType);
    const nextScope = manualScopeOptions[0] ?? 'USER';
    const nextReviewState = memoryMetadata.reviewStates[0] ?? 'USER_CONFIRMED';
    const nextSourceType = memoryMetadata.sourceTypes[0] ?? 'MANUAL';
    setSelectedMemoryId(null);
    setSelectedMemory(null);
    setMemoryForm({
      ...emptyForm(),
      memoryType: nextMemoryType,
      subType: nextSubType,
      scope: nextScope,
      reviewState: nextReviewState,
      sourceType: nextSourceType,
    });
    setFormErrors({});
  };

  const handleApplyFilters = (event: FormEvent) => {
    event.preventDefault();
    setFilters(buildFilterParams(filterForm, page.size));
  };

  const handleResetFilters = () => {
    setFilterForm(defaultFilters);
    setFilters({ current: 1, size: page.size, status: MEMORY_STATUS.ACTIVE });
  };

  const handleFilterReviewQueue = () => {
    setFilterForm((prev) => {
      const nextForm: FilterFormState = {
        ...prev,
        reviewState: 'NEEDS_REVIEW',
        status: String(MEMORY_STATUS.ACTIVE),
      };
      setFilters(buildFilterParams(nextForm, page.size));
      return nextForm;
    });
  };

  const validateForm = () => {
    const errors: Record<string, string> = {};
    const subTypesForType = subTypeOptionsByType[memoryForm.memoryType] ?? [];

    if (!memoryForm.memoryType.trim()) {
      errors.memoryType = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_REQUIRED_TYPE);
    }
    if (!memoryForm.subType.trim()) {
      errors.subType = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_REQUIRED_SUB_TYPE);
    } else if (subTypesForType.length > 0 && !subTypesForType.includes(memoryForm.subType)) {
      errors.subType = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_INVALID_SUB_TYPE);
    }
    if (!isEditing && memoryForm.scope === 'WORKSPACE') {
      errors.scope = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_MANUAL_WORKSPACE_SCOPE);
    }
    if (!memoryForm.content.trim()) {
      errors.content = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_REQUIRED_CONTENT);
    }
    if (memoryForm.conversationId.trim() && Number.isNaN(Number(memoryForm.conversationId.trim()))) {
      errors.conversationId = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_CONVERSATION);
    }
    const numericScores = [memoryForm.confidenceScore, memoryForm.salienceScore]
      .filter(Boolean)
      .map((item) => Number(item));
    if (numericScores.some((item) => Number.isNaN(item) || item < 0 || item > 1)) {
      errors.confidenceScore = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_SCORE_RANGE);
      errors.salienceScore = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_SCORE_RANGE);
    }
    if (memoryForm.detailJson.trim()) {
      try {
        JSON.parse(memoryForm.detailJson);
      } catch {
        errors.detailJson = t(I18N_KEYS.MEMORY_PAGE.VALIDATION_JSON);
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const buildPayload = (): MemoryCreateRequest => ({
    conversationId: memoryForm.conversationId.trim() ? Number(memoryForm.conversationId.trim()) : undefined,
    memoryType: memoryForm.memoryType,
    subType: memoryForm.subType || undefined,
    scope: memoryForm.scope,
    reviewState: memoryForm.reviewState,
    sourceType: memoryForm.sourceType,
    title: memoryForm.title.trim() || undefined,
    reason: memoryForm.reason.trim() || undefined,
    content: memoryForm.content.trim(),
    detailJson: memoryForm.detailJson.trim() || '{}',
    confidenceScore: memoryForm.confidenceScore.trim() ? Number(memoryForm.confidenceScore) : undefined,
    salienceScore: memoryForm.salienceScore.trim() ? Number(memoryForm.salienceScore) : undefined,
    expiresAt: toBackendDateTime(memoryForm.expiresAt),
  });

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      const payload = buildPayload();
      const saved = isEditing
        ? await memoryService.update(selectedMemoryId, {
            memoryType: payload.memoryType,
            workspaceContextKey: payload.workspaceContextKey,
            workspaceLevel: payload.workspaceLevel,
            scope: payload.scope,
            subType: payload.subType,
            reviewState: payload.reviewState,
            sourceType: payload.sourceType,
            title: payload.title,
            reason: payload.reason,
            content: payload.content,
            detailJson: payload.detailJson,
            sourceMessageIds: payload.sourceMessageIds,
            confidenceScore: payload.confidenceScore,
            salienceScore: payload.salienceScore,
            expiresAt: payload.expiresAt,
          })
        : await memoryService.create(payload);

      setSelectedMemoryId(saved.id);
      setSelectedMemory(saved);
      setMemoryForm(mapMemoryToForm(saved));
      toast.success(t(isEditing ? I18N_KEYS.MEMORY_PAGE.UPDATE_SUCCESS : I18N_KEYS.MEMORY_PAGE.CREATE_SUCCESS));
      await loadMemories();
      await loadMaintenanceSummary();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.SAVE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  const handleArchive = async () => {
    if (!selectedMemoryId) {
      return;
    }
    setSubmitting(true);
    try {
      const memory = await memoryService.archive(selectedMemoryId);
      setSelectedMemory(memory);
      setMemoryForm(mapMemoryToForm(memory));
      toast.success(t(I18N_KEYS.MEMORY_PAGE.ARCHIVE_SUCCESS));
      await loadMemories();
      await loadMaintenanceSummary();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.ARCHIVE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  const handleRestore = async () => {
    if (!selectedMemoryId) {
      return;
    }
    setSubmitting(true);
    try {
      const memory = await memoryService.restore(selectedMemoryId);
      setSelectedMemory(memory);
      setMemoryForm(mapMemoryToForm(memory));
      toast.success(t(I18N_KEYS.MEMORY_PAGE.RESTORE_SUCCESS));
      await loadMemories();
      await loadMaintenanceSummary();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.RESTORE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmReview = async () => {
    if (!selectedMemoryId) {
      return;
    }
    setSubmitting(true);
    try {
      const memory = await memoryService.confirm(selectedMemoryId);
      setSelectedMemory(memory);
      setMemoryForm(mapMemoryToForm(memory));
      toast.success(t(I18N_KEYS.MEMORY_PAGE.CONFIRM_REVIEW_SUCCESS));
      await loadMemories();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.CONFIRM_REVIEW_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  const handleMarkNeedsReview = async () => {
    if (!selectedMemoryId) {
      return;
    }
    setSubmitting(true);
    try {
      const memory = await memoryService.markNeedsReview(selectedMemoryId);
      setSelectedMemory(memory);
      setMemoryForm(mapMemoryToForm(memory));
      toast.success(t(I18N_KEYS.MEMORY_PAGE.MARK_NEEDS_REVIEW_SUCCESS));
      await loadMemories();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.MARK_NEEDS_REVIEW_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedMemoryId || !window.confirm(t(I18N_KEYS.MEMORY_PAGE.DELETE_CONFIRM))) {
      return;
    }
    setSubmitting(true);
    try {
      await memoryService.delete(selectedMemoryId);
      toast.success(t(I18N_KEYS.MEMORY_PAGE.DELETE_SUCCESS));
      resetEditor();
      await loadMemories();
      await loadMaintenanceSummary();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.DELETE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  const handleSemanticSearch = async (event: FormEvent) => {
    event.preventDefault();
    if (!semanticQuery.trim()) {
      setSemanticResults([]);
      return;
    }

    setSemanticLoading(true);
    try {
      const results = await memoryService.search({ queryText: semanticQuery.trim(), limit: 8, minScore: 0.6 });
      setSemanticResults(results);
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.SEARCH_FAILED)));
    } finally {
      setSemanticLoading(false);
    }
  };

  const handleRunMaintenance = async () => {
    setMaintenanceLoading(true);
    try {
      const report = await memoryService.runMaintenance();
      setMaintenance(report);
      toast.success(t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_RUN_SUCCESS));
      await loadMemories();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_RUN_FAILED)));
    } finally {
      setMaintenanceLoading(false);
    }
  };

  const statusLabel = selectedMemory ? t(getStatusLabelKey(selectedMemory.status)) : '';
  const listStart = page.total === 0 ? 0 : (page.current - 1) * page.size + 1;
  const listEnd = page.total === 0 ? 0 : Math.min(page.current * page.size, page.total);
  const currentPageNeedsReview = page.records.filter((item) => item.reviewState === 'NEEDS_REVIEW').length;
  const commonSelectClassName = 'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm';
  const commonTextAreaClassName = 'min-h-[140px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm';

  return (
    <div className="space-y-6 pb-8 theme-text-primary">
      <section className="relative overflow-hidden rounded-3xl border theme-border theme-bg-panel px-6 py-7 shadow-sm">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(16,185,129,0.18),transparent_24%),radial-gradient(circle_at_bottom_left,rgba(59,130,246,0.18),transparent_22%)]" />
        <div className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full bg-emerald-400/15 blur-3xl" />
        <div className="pointer-events-none absolute -left-10 bottom-0 h-32 w-32 rounded-full bg-sky-400/15 blur-3xl" />
        <div className="relative space-y-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="space-y-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-8 rounded-full border theme-border theme-bg-main px-3 theme-text-secondary hover:theme-text-primary"
                onClick={() => navigate('/')}
              >
                <ArrowLeft className="mr-2 h-4 w-4" />
                {t(I18N_KEYS.SETTINGS_PAGE.BACK_TO_WORKSPACE)}
              </Button>
              <div className="inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-400">
                <Brain className="h-3.5 w-3.5" />
                {t(I18N_KEYS.AI.MEMORY_ENTRY_LABEL)}
              </div>
              <h1 className="text-2xl font-semibold tracking-tight">{t(I18N_KEYS.MEMORY_PAGE.PAGE_TITLE)}</h1>
              <p className="max-w-3xl text-sm theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.PAGE_DESC)}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button type="button" variant="outline" onClick={() => void loadMemories()}>
                <RefreshCcw className="mr-2 h-4 w-4" />
                {t(I18N_KEYS.MEMORY_PAGE.REFRESH)}
              </Button>
              <Button type="button" variant="outline" onClick={handleFilterReviewQueue} disabled={metadataLoading}>
                {t(I18N_KEYS.MEMORY_PAGE.FILTER_REVIEW_QUEUE)}
              </Button>
              <Button type="button" onClick={resetEditor}>
                <Plus className="mr-2 h-4 w-4" />
                {t(I18N_KEYS.MEMORY_PAGE.NEW_MEMORY)}
              </Button>
            </div>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-xl border theme-border bg-[var(--bg-main)]/70 p-3">
              <p className="text-xs uppercase tracking-wide theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.LIST_TITLE)}</p>
              <p className="mt-1 text-2xl font-semibold">{page.total}</p>
              <p className="text-xs theme-text-secondary">{listStart}-{listEnd}</p>
            </div>
            <div className="rounded-xl border theme-border bg-[var(--bg-main)]/70 p-3">
              <p className="text-xs uppercase tracking-wide theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.FILTER_REVIEW_STATE)}</p>
              <p className="mt-1 text-2xl font-semibold">{currentPageNeedsReview}</p>
              <p className="text-xs theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.STATUS_ACTIVE)}</p>
            </div>
            <div className="rounded-xl border theme-border bg-[var(--bg-main)]/70 p-3">
              <p className="text-xs uppercase tracking-wide theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_EXPIRED)}</p>
              <p className="mt-1 text-2xl font-semibold">{maintenance.expiredActiveMemoryCount}</p>
              <p className="text-xs theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_TITLE)}</p>
            </div>
            <div className="rounded-xl border theme-border bg-[var(--bg-main)]/70 p-3">
              <p className="text-xs uppercase tracking-wide theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_DUPLICATES)}</p>
              <p className="mt-1 text-2xl font-semibold">{maintenance.duplicateActiveMemoryCount}</p>
              <p className="text-xs theme-text-secondary">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_GENERATED_AT)}</p>
            </div>
          </div>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)_420px]">
        <div className="space-y-6 xl:sticky xl:top-4 xl:self-start">
          <Card className="border border-border/70 shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">{t(I18N_KEYS.MEMORY_PAGE.APPLY_FILTERS)}</CardTitle>
              <CardDescription>{t(I18N_KEYS.MEMORY_PAGE.LIST_DESC)}</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleApplyFilters} className="space-y-3">
                <div className="space-y-1">
                  <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    {t(I18N_KEYS.MEMORY_PAGE.FILTER_KEYWORD)}
                  </label>
                  <Input
                    value={filterForm.keyword}
                    onChange={handleFilterInputChange('keyword')}
                    placeholder={t(I18N_KEYS.MEMORY_PAGE.FILTER_KEYWORD)}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    {t(I18N_KEYS.MEMORY_PAGE.FILTER_MEMORY_TYPE)}
                  </label>
                  <select
                    className={commonSelectClassName}
                    value={filterForm.memoryType}
                    onChange={handleFilterInputChange('memoryType')}
                    disabled={metadataLoading}
                  >
                    <option value="">--</option>
                    {filterMemoryTypeOptions.map((option) => (
                      <option key={option} value={option}>
                        {formatLabel(option)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    {t(I18N_KEYS.MEMORY_PAGE.FILTER_SCOPE)}
                  </label>
                  <select
                    className={commonSelectClassName}
                    value={filterForm.scope}
                    onChange={handleFilterInputChange('scope')}
                    disabled={metadataLoading}
                  >
                    <option value="">--</option>
                    {filterScopeOptions.map((option) => (
                      <option key={option} value={option}>
                        {formatLabel(option)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    {t(I18N_KEYS.MEMORY_PAGE.FILTER_REVIEW_STATE)}
                  </label>
                  <select
                    className={commonSelectClassName}
                    value={filterForm.reviewState}
                    onChange={handleFilterInputChange('reviewState')}
                    disabled={metadataLoading}
                  >
                    <option value="">--</option>
                    {filterReviewStateOptions.map((option) => (
                      <option key={option} value={option}>
                        {formatLabel(option)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    {t(I18N_KEYS.MEMORY_PAGE.FILTER_STATUS)}
                  </label>
                  <select
                    className={commonSelectClassName}
                    value={filterForm.status}
                    onChange={handleFilterInputChange('status')}
                  >
                    <option value="">--</option>
                    <option value={String(MEMORY_STATUS.ACTIVE)}>{t(I18N_KEYS.MEMORY_PAGE.STATUS_ACTIVE)}</option>
                    <option value={String(MEMORY_STATUS.ARCHIVED)}>{t(I18N_KEYS.MEMORY_PAGE.STATUS_ARCHIVED)}</option>
                    <option value={String(MEMORY_STATUS.HIDDEN)}>{t(I18N_KEYS.MEMORY_PAGE.STATUS_HIDDEN)}</option>
                  </select>
                </div>
                <div className="grid gap-2 pt-2">
                  <Button type="submit">
                    <Search className="mr-2 h-4 w-4" />
                    {t(I18N_KEYS.MEMORY_PAGE.APPLY_FILTERS)}
                  </Button>
                  <Button type="button" variant="outline" onClick={handleResetFilters}>
                    {t(I18N_KEYS.MEMORY_PAGE.RESET_FILTERS)}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          <Card className="border border-border/70 shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">{t(I18N_KEYS.MEMORY_PAGE.SEARCH_TITLE)}</CardTitle>
              <CardDescription>{t(I18N_KEYS.MEMORY_PAGE.SEARCH_DESC)}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <form onSubmit={handleSemanticSearch} className="space-y-2">
                <Input
                  value={semanticQuery}
                  onChange={(event) => setSemanticQuery(event.target.value)}
                  placeholder={t(I18N_KEYS.MEMORY_PAGE.SEARCH_PLACEHOLDER)}
                />
                <Button type="submit" disabled={semanticLoading} className="w-full">
                  <Brain className="mr-2 h-4 w-4" />
                  {t(I18N_KEYS.MEMORY_PAGE.SEARCH_BUTTON)}
                </Button>
              </form>
              <div className="space-y-2">
                {semanticLoading ? (
                  <p className="text-sm text-muted-foreground">{t(I18N_KEYS.COMMON.LOADING)}</p>
                ) : semanticResults.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.SEARCH_EMPTY)}</p>
                ) : (
                  semanticResults.map((result) => (
                    <button
                      key={`${result.id}-${result.score}`}
                      type="button"
                      onClick={() => void openMemory(result.id)}
                      className="w-full rounded-lg border border-border p-3 text-left transition-colors hover:border-primary/40 hover:bg-muted/40"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-medium">{formatLabel(result.memoryType)}</span>
                        <span className="rounded-full bg-primary/10 px-2 py-1 text-[11px] font-medium text-primary">
                          {t(I18N_KEYS.MEMORY_PAGE.SEARCH_SCORE)} {result.score.toFixed(3)}
                        </span>
                      </div>
                      <p className="mt-2 line-clamp-2 text-xs text-muted-foreground">{result.content}</p>
                    </button>
                  ))
                )}
              </div>
            </CardContent>
          </Card>

          <Card className="border border-border/70 shadow-sm">
            <CardHeader>
              <div className="flex items-center justify-between gap-2">
                <div>
                  <CardTitle className="text-base">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_TITLE)}</CardTitle>
                  <CardDescription>{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_DESC)}</CardDescription>
                </div>
                <Button type="button" variant="ghost" size="sm" onClick={() => void loadMaintenanceSummary()} disabled={maintenanceLoading}>
                  <RefreshCcw className="h-4 w-4" />
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="grid grid-cols-2 gap-2">
                <div className="rounded-lg border border-border bg-muted/30 p-2">
                  <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_ACTIVE)}</p>
                  <p className="mt-1 text-lg font-semibold">{maintenance.activeMemoryCount}</p>
                </div>
                <div className="rounded-lg border border-border bg-muted/30 p-2">
                  <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_ARCHIVED)}</p>
                  <p className="mt-1 text-lg font-semibold">{maintenance.archivedMemoryCount}</p>
                </div>
                <div className="rounded-lg border border-border bg-muted/30 p-2">
                  <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_HIDDEN)}</p>
                  <p className="mt-1 text-lg font-semibold">{maintenance.hiddenMemoryCount}</p>
                </div>
                <div className="rounded-lg border border-border bg-muted/30 p-2">
                  <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_DUPLICATES)}</p>
                  <p className="mt-1 text-lg font-semibold">{maintenance.duplicateActiveMemoryCount}</p>
                </div>
              </div>
              <div className="rounded-lg border border-dashed border-border p-2 text-xs text-muted-foreground">
                {t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_GENERATED_AT)}: {formatDateTime(maintenance.generatedAt)}
              </div>
              <Button type="button" className="w-full" onClick={handleRunMaintenance} disabled={maintenanceLoading}>
                <Archive className="mr-2 h-4 w-4" />
                {t(I18N_KEYS.MEMORY_PAGE.MAINTENANCE_RUN)}
              </Button>
            </CardContent>
          </Card>
        </div>

        <Card className="border border-border/70 shadow-sm">
          <CardHeader className="border-b border-border/60 bg-muted/20">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <CardTitle className="text-lg">{t(I18N_KEYS.MEMORY_PAGE.LIST_TITLE)}</CardTitle>
                <CardDescription>{t(I18N_KEYS.MEMORY_PAGE.LIST_DESC)}</CardDescription>
              </div>
              <div className="rounded-full border border-border bg-background px-3 py-1 text-xs text-muted-foreground">
                {pageSummary}
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4 p-4 md:p-6">
            {listLoading ? (
              <div className="rounded-lg border border-dashed border-border p-6 text-sm text-muted-foreground">
                {t(I18N_KEYS.COMMON.LOADING)}
              </div>
            ) : page.records.length === 0 ? (
              <div className="rounded-lg border border-dashed border-border p-6 text-sm text-muted-foreground">
                {t(I18N_KEYS.MEMORY_PAGE.EMPTY)}
              </div>
            ) : (
              <div className="space-y-3">
                {page.records.map((memory) => (
                  <button
                    key={memory.id}
                    type="button"
                    onClick={() => void openMemory(memory.id)}
                    className={cn(
                      'w-full rounded-2xl border p-4 text-left transition-all hover:border-primary/40 hover:bg-muted/40',
                      selectedMemoryId === memory.id && 'border-primary bg-primary/5 shadow-sm'
                    )}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-foreground">
                          {memory.title || formatLabel(memory.memoryType)}
                        </p>
                        <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
                          {memory.subType && (
                            <span className="rounded-full border border-primary/30 bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
                              {formatLabel(memory.subType)}
                            </span>
                          )}
                          <span>{formatLabel(memory.memoryType)}</span>
                          <span>{formatLabel(memory.scope)}</span>
                          {memory.scope === 'WORKSPACE' && memory.workspaceLevel && <span>{formatLabel(memory.workspaceLevel)}</span>}
                          <span className={cn(memory.reviewState === 'NEEDS_REVIEW' && 'font-semibold text-amber-700')}>
                            {formatLabel(memory.reviewState)}
                          </span>
                        </div>
                      </div>
                      <span className={cn('shrink-0 rounded-full border px-2 py-1 text-[11px] font-medium', getStatusTone(memory.status))}>
                        {t(getStatusLabelKey(memory.status))}
                      </span>
                    </div>
                    <p className="mt-3 line-clamp-3 text-sm text-muted-foreground">{memory.content}</p>
                    <div className="mt-3 flex flex-wrap gap-3 text-[11px] text-muted-foreground">
                      <span>{t(I18N_KEYS.MEMORY_PAGE.META_SOURCE)}: {formatLabel(memory.sourceType)}</span>
                      {memory.scope === 'WORKSPACE' && (
                        <span>{t(I18N_KEYS.MEMORY_PAGE.META_WORKSPACE_BINDING)}: {formatWorkspaceBinding(memory)}</span>
                      )}
                      <span>{t(I18N_KEYS.MEMORY_PAGE.META_UPDATED)}: {formatDateTime(memory.updatedAt)}</span>
                    </div>
                  </button>
                ))}
              </div>
            )}
            <div className="flex items-center justify-between gap-2 border-t border-border pt-3">
              <span className="text-xs text-muted-foreground">
                {page.current} / {Math.max(page.pages, 1)}
              </span>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={filters.current === 1 || listLoading}
                  onClick={() => setFilters((prev) => ({ ...prev, current: Math.max(1, (prev.current ?? 1) - 1) }))}
                >
                  {t(I18N_KEYS.EXPLORER.PREVIOUS)}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={listLoading || (page.pages > 0 && filters.current === page.pages) || page.records.length === 0}
                  onClick={() => setFilters((prev) => ({ ...prev, current: (prev.current ?? 1) + 1 }))}
                >
                  {t(I18N_KEYS.EXPLORER.NEXT)}
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border border-border/70 shadow-sm xl:sticky xl:top-4 xl:self-start">
          <CardHeader className="border-b border-border/60 bg-muted/20">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <CardTitle className="text-lg">
                  {t(isEditing ? I18N_KEYS.MEMORY_PAGE.DETAIL_EDIT_TITLE : I18N_KEYS.MEMORY_PAGE.DETAIL_NEW_TITLE)}
                </CardTitle>
                <CardDescription>
                  {t(isEditing ? I18N_KEYS.MEMORY_PAGE.DETAIL_EDIT_DESC : I18N_KEYS.MEMORY_PAGE.DETAIL_NEW_DESC)}
                </CardDescription>
              </div>
              {selectedMemory && (
                <span className={cn('rounded-full border px-3 py-1 text-xs font-medium', getStatusTone(selectedMemory.status))}>
                  {statusLabel}
                </span>
              )}
            </div>
          </CardHeader>
          <CardContent className="space-y-5 p-4 md:p-6">
            {detailLoading ? (
              <div className="rounded-lg border border-dashed border-border p-5 text-sm text-muted-foreground">
                {t(I18N_KEYS.COMMON.LOADING)}
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-5">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONVERSATION_ID)}</label>
                    <Input value={memoryForm.conversationId} onChange={handleFormInputChange('conversationId')} placeholder="123" />
                    {formErrors.conversationId && <p className="text-sm text-destructive">{formErrors.conversationId}</p>}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_MEMORY_TYPE)}</label>
                    <select
                      className={commonSelectClassName}
                      value={memoryForm.memoryType}
                      onChange={handleFormInputChange('memoryType')}
                      disabled={metadataLoading}
                    >
                      {memoryTypeOptions.map((option) => (
                        <option key={option} value={option}>
                          {formatLabel(option)}
                        </option>
                      ))}
                    </select>
                    {formErrors.memoryType && <p className="text-sm text-destructive">{formErrors.memoryType}</p>}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SUB_TYPE)}</label>
                    <select
                      className={commonSelectClassName}
                      value={memoryForm.subType}
                      onChange={handleFormInputChange('subType')}
                      disabled={metadataLoading}
                    >
                      <option value="">{t(I18N_KEYS.MEMORY_PAGE.SELECT_SUB_TYPE_PLACEHOLDER)}</option>
                      {availableSubTypeOptions.map((option) => (
                        <option key={option} value={option}>
                          {formatLabel(option)}
                        </option>
                      ))}
                    </select>
                    {formErrors.subType && <p className="text-sm text-destructive">{formErrors.subType}</p>}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SCOPE)}</label>
                    <select
                      className={commonSelectClassName}
                      value={memoryForm.scope}
                      onChange={handleFormInputChange('scope')}
                      disabled={metadataLoading || (isEditing && selectedMemory?.scope === 'WORKSPACE')}
                    >
                      {editorScopeOptions.map((option) => (
                        <option key={option} value={option}>
                          {formatLabel(option)}
                        </option>
                      ))}
                    </select>
                    {formErrors.scope && <p className="text-sm text-destructive">{formErrors.scope}</p>}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_REVIEW_STATE)}</label>
                    <select
                      className={commonSelectClassName}
                      value={memoryForm.reviewState}
                      onChange={handleFormInputChange('reviewState')}
                      disabled={metadataLoading}
                    >
                      {reviewStateOptions.map((option) => (
                        <option key={option} value={option}>
                          {formatLabel(option)}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SOURCE_TYPE)}</label>
                    <select
                      className={commonSelectClassName}
                      value={memoryForm.sourceType}
                      onChange={handleFormInputChange('sourceType')}
                      disabled={metadataLoading || (isEditing && selectedMemory?.scope === 'WORKSPACE')}
                    >
                      {sourceTypeOptions.map((option) => (
                        <option key={option} value={option}>
                          {formatLabel(option)}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_EXPIRES_AT)}</label>
                    <Input type="datetime-local" value={memoryForm.expiresAt} onChange={handleFormInputChange('expiresAt')} />
                  </div>

                  {selectedMemory?.scope === 'WORKSPACE' && (
                    <>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_WORKSPACE_LEVEL)}</label>
                        <Input value={formatLabel(selectedMemory.workspaceLevel)} readOnly />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_WORKSPACE_CONTEXT)}</label>
                        <Input value={selectedMemory.workspaceContextKey || '--'} readOnly />
                      </div>
                    </>
                  )}
                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_TITLE)}</label>
                    <Input value={memoryForm.title} onChange={handleFormInputChange('title')} placeholder={t(I18N_KEYS.MEMORY_PAGE.DETAIL_NEW_TITLE)} />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_REASON)}</label>
                    <Input value={memoryForm.reason} onChange={handleFormInputChange('reason')} placeholder={t(I18N_KEYS.MEMORY_PAGE.FIELD_REASON)} />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONTENT)}</label>
                    <textarea
                      className={cn(commonTextAreaClassName, 'min-h-[150px]')}
                      value={memoryForm.content}
                      onChange={handleFormInputChange('content')}
                      placeholder={t(I18N_KEYS.MEMORY_PAGE.FIELD_CONTENT)}
                    />
                    {formErrors.content && <p className="text-sm text-destructive">{formErrors.content}</p>}
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_DETAIL_JSON)}</label>
                    <textarea
                      className={cn(commonTextAreaClassName, 'font-mono')}
                      value={memoryForm.detailJson}
                      onChange={handleFormInputChange('detailJson')}
                    />
                    {formErrors.detailJson && <p className="text-sm text-destructive">{formErrors.detailJson}</p>}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONFIDENCE)}</label>
                    <Input type="number" min="0" max="1" step="0.01" value={memoryForm.confidenceScore} onChange={handleFormInputChange('confidenceScore')} />
                    {formErrors.confidenceScore && <p className="text-sm text-destructive">{formErrors.confidenceScore}</p>}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SALIENCE)}</label>
                    <Input type="number" min="0" max="1" step="0.01" value={memoryForm.salienceScore} onChange={handleFormInputChange('salienceScore')} />
                    {formErrors.salienceScore && <p className="text-sm text-destructive">{formErrors.salienceScore}</p>}
                  </div>
                </div>
                <div className="flex flex-wrap gap-2 border-t border-border pt-4">
                  <Button type="submit" disabled={submitting}>
                    {isEditing ? t(I18N_KEYS.MEMORY_PAGE.UPDATE_ACTION) : t(I18N_KEYS.MEMORY_PAGE.CREATE_ACTION)}
                  </Button>
                  <Button type="button" variant="outline" disabled={submitting} onClick={resetEditor}>
                    <Plus className="mr-2 h-4 w-4" />
                    {t(I18N_KEYS.MEMORY_PAGE.NEW_MEMORY)}
                  </Button>
                  {selectedMemory && selectedMemory.reviewState !== 'USER_CONFIRMED' && (
                    <Button type="button" variant="outline" disabled={submitting} onClick={handleConfirmReview}>
                      {t(I18N_KEYS.MEMORY_PAGE.CONFIRM_REVIEW)}
                    </Button>
                  )}
                  {selectedMemory && selectedMemory.reviewState !== 'NEEDS_REVIEW' && (
                    <Button type="button" variant="outline" disabled={submitting} onClick={handleMarkNeedsReview}>
                      {t(I18N_KEYS.MEMORY_PAGE.MARK_NEEDS_REVIEW)}
                    </Button>
                  )}
                  {selectedMemory && selectedMemory.status === MEMORY_STATUS.ACTIVE && (
                    <Button type="button" variant="outline" disabled={submitting} onClick={handleArchive}>
                      <Archive className="mr-2 h-4 w-4" />
                      {t(I18N_KEYS.MEMORY_PAGE.ARCHIVE)}
                    </Button>
                  )}
                  {selectedMemory && selectedMemory.status === MEMORY_STATUS.ARCHIVED && (
                    <Button type="button" variant="outline" disabled={submitting} onClick={handleRestore}>
                      <RotateCcw className="mr-2 h-4 w-4" />
                      {t(I18N_KEYS.MEMORY_PAGE.RESTORE)}
                    </Button>
                  )}
                  {selectedMemory && (
                    <Button type="button" variant="destructive" disabled={submitting} onClick={handleDelete}>
                      <Trash2 className="mr-2 h-4 w-4" />
                      {t(I18N_KEYS.MEMORY_PAGE.DELETE)}
                    </Button>
                  )}
                </div>
              </form>
            )}

            {selectedMemory && (
              <div className="grid gap-3 rounded-xl border border-border bg-muted/20 p-4 sm:grid-cols-2">
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_CREATED)}</div>
                  <div className="mt-1 text-sm">{formatDateTime(selectedMemory.createdAt)}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_UPDATED)}</div>
                  <div className="mt-1 text-sm">{formatDateTime(selectedMemory.updatedAt)}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_ACCESS_COUNT)}</div>
                  <div className="mt-1 text-sm">{selectedMemory.accessCount ?? 0}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_LAST_ACCESSED)}</div>
                  <div className="mt-1 flex items-center gap-2 text-sm">
                    <Clock3 className="h-4 w-4 text-muted-foreground" />
                    {formatDateTime(selectedMemory.lastAccessedAt)}
                  </div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_SOURCE)}</div>
                  <div className="mt-1 text-sm">{formatLabel(selectedMemory.sourceType)}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_REVIEW)}</div>
                  <div className="mt-1 text-sm">{formatLabel(selectedMemory.reviewState)}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SUB_TYPE)}</div>
                  <div className="mt-1 text-sm">{formatLabel(selectedMemory.subType)}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_SCOPE)}</div>
                  <div className="mt-1 text-sm">{formatLabel(selectedMemory.scope)}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_WORKSPACE_BINDING)}</div>
                  <div className="mt-1 text-sm">{formatWorkspaceBinding(selectedMemory)}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.META_CONVERSATION)}</div>
                  <div className="mt-1 text-sm">{selectedMemory.conversationId ?? '--'}</div>
                </div>
                <div className="rounded-lg bg-background/80 p-3 sm:col-span-2">
                  <div className="text-xs uppercase tracking-wide text-muted-foreground">{t(I18N_KEYS.MEMORY_PAGE.FIELD_EXPIRES_AT)}</div>
                  <div className="mt-1 text-sm">{formatDateTime(selectedMemory.expiresAt)}</div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
