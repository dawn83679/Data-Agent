import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { Archive, Brain, Plus, RefreshCcw, RotateCcw, Search, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Card, CardContent } from '../components/ui/Card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../components/ui/Dialog';
import { Input } from '../components/ui/Input';
import { I18N_KEYS } from '../constants/i18nKeys';
import { MemoryControlCenter, MemoryOverviewHero, type MemoryListItemView } from '../features/memory/components';
import type { FilterFormState, MemoryFormState } from '../features/memory/memoryPageModels';
import {
  appendOptionIfMissing,
  buildFallbackMemoryMetadata,
  buildFilterParams,
  buildMemoryPayload,
  buildSubTypeOptionsByType,
  defaultFilterFormState,
  defaultMemoryListParams,
  defaultMemoryPage,
  formatDateTime,
  formatWorkspaceBindingLabel,
  getDefaultMemorySubtypeByType,
  getInitialCreateMemoryFormState,
  getMemoryOptionLabel,
  getStatusLabelKey,
  getStatusToneClassName,
  mapMemoryToFormState,
  MEMORY_FORM_SELECT_CLASS_NAME,
  MEMORY_FORM_TEXTAREA_CLASS_NAME,
  normalizeMemoryMetadata,
} from '../features/memory/memoryPageUtils';
import { useToast } from '../hooks/useToast';
import { resolveErrorMessage } from '../lib/errorMessage';
import { cn } from '../lib/utils';
import { memoryService } from '../services/memory.service';
import { MEMORY_STATUS, type Memory, type MemoryMetadataResponse, type MemoryPage, type MemoryScope, type MemorySearchResult } from '../types/memory';
type MemoryDialogMode = 'closed' | 'create' | 'edit';

const SEMANTIC_SEARCH_LIMIT = 8;
const SEMANTIC_SEARCH_MIN_SCORE = 0.6;

const fallbackMemoryMetadata = buildFallbackMemoryMetadata();
const fallbackSubTypeOptionsByType = buildSubTypeOptionsByType(fallbackMemoryMetadata.memoryTypes);

export default function Memories() {
  const { t } = useTranslation();
  const toast = useToast();
  const navigate = useNavigate();

  const [page, setPage] = useState<MemoryPage>(defaultMemoryPage);
  const [filters, setFilters] = useState(defaultMemoryListParams);
  const [filterForm, setFilterForm] = useState<FilterFormState>(defaultFilterFormState);
  const [memoryForm, setMemoryForm] = useState<MemoryFormState>(() =>
    getInitialCreateMemoryFormState(fallbackMemoryMetadata, fallbackSubTypeOptionsByType, ['USER']),
  );
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [detailDialogMode, setDetailDialogMode] = useState<MemoryDialogMode>('closed');
  const [selectedMemoryId, setSelectedMemoryId] = useState<number | null>(null);
  const [selectedMemory, setSelectedMemory] = useState<Memory | null>(null);
  const [semanticResults, setSemanticResults] = useState<MemorySearchResult[] | null>(null);
  const [semanticSearchEnabled, setSemanticSearchEnabled] = useState(false);
  const [listLoading, setListLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [semanticLoading, setSemanticLoading] = useState(false);
  const [metadataLoading, setMetadataLoading] = useState(false);
  const [memoryMetadata, setMemoryMetadata] = useState<MemoryMetadataResponse>(fallbackMemoryMetadata);

  const isDialogOpen = detailDialogMode !== 'closed';
  const isEditing = detailDialogMode === 'edit' && selectedMemoryId != null;
  const isSemanticMode = semanticLoading || semanticResults != null;

  const subTypeOptionsByType = useMemo(
    () => buildSubTypeOptionsByType(memoryMetadata.memoryTypes),
    [memoryMetadata.memoryTypes],
  );
  const memoryTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.memoryTypes.map((item) => item.code), memoryForm.memoryType),
    [memoryForm.memoryType, memoryMetadata.memoryTypes],
  );
  const filterMemoryTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryTypeOptions, filterForm.memoryType),
    [filterForm.memoryType, memoryTypeOptions],
  );
  const scopeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.scopes, memoryForm.scope),
    [memoryForm.scope, memoryMetadata.scopes],
  );
  const filterScopeOptions = useMemo(
    () => appendOptionIfMissing(scopeOptions, filterForm.scope),
    [filterForm.scope, scopeOptions],
  );
  const sourceTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.sourceTypes, memoryForm.sourceType),
    [memoryForm.sourceType, memoryMetadata.sourceTypes],
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
    const allowedMemoryTypes = memoryMetadata.memoryTypes.map((item) => item.code);
    const allowedScopes = memoryMetadata.scopes;
    const allowedSourceTypes = memoryMetadata.sourceTypes;

    setFilterForm((prev) => ({
      ...prev,
      memoryType: prev.memoryType && !allowedMemoryTypes.includes(prev.memoryType) ? '' : prev.memoryType,
      scope: prev.scope && !allowedScopes.includes(prev.scope) ? '' : prev.scope,
    }));
    setFilters((prev) => ({
      ...prev,
      memoryType: prev.memoryType && !allowedMemoryTypes.includes(prev.memoryType) ? undefined : prev.memoryType,
      scope: prev.scope && !allowedScopes.includes(prev.scope) ? undefined : prev.scope,
    }));

    setMemoryForm((prev) => {
      const nextMemoryType = allowedMemoryTypes.includes(prev.memoryType)
        ? prev.memoryType
        : (allowedMemoryTypes[0] ?? 'PREFERENCE');
      const nextSubTypeOptions = subTypeOptionsByType[nextMemoryType] ?? [];
      const nextSubType = prev.subType && nextSubTypeOptions.includes(prev.subType)
        ? prev.subType
        : (nextSubTypeOptions[0] ?? getDefaultMemorySubtypeByType(nextMemoryType));
      const nextScope = (isEditing && selectedMemory?.scope === 'WORKSPACE')
        ? 'WORKSPACE'
        : allowedScopes.includes(prev.scope) && prev.scope !== 'WORKSPACE'
          ? prev.scope
          : (allowedScopes.find((scope) => scope !== 'WORKSPACE') ?? 'USER');
      const nextSourceType = allowedSourceTypes.includes(prev.sourceType)
        ? prev.sourceType
        : (allowedSourceTypes[0] ?? 'MANUAL');
      if (
        prev.memoryType === nextMemoryType &&
        prev.subType === nextSubType &&
        prev.scope === nextScope &&
        prev.sourceType === nextSourceType
      ) {
        return prev;
      }
      return {
        ...prev,
        memoryType: nextMemoryType,
        subType: nextSubType,
        scope: nextScope,
        sourceType: nextSourceType,
      };
    });
  }, [isEditing, memoryMetadata, selectedMemory?.scope, subTypeOptionsByType]);

  const runSemanticSearch = useCallback(async (query: string) => {
    setSemanticLoading(true);
    try {
      const results = await memoryService.search({
        queryText: query,
        limit: SEMANTIC_SEARCH_LIMIT,
        minScore: SEMANTIC_SEARCH_MIN_SCORE,
      });
      setSemanticResults(results);
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.SEARCH_FAILED)));
    } finally {
      setSemanticLoading(false);
    }
  }, [t, toast]);

  const refreshWorkspace = useCallback(async () => {
    if (semanticResults != null && filterForm.keyword.trim()) {
      await runSemanticSearch(filterForm.keyword.trim());
      return;
    }
    await loadMemories();
  }, [filterForm.keyword, loadMemories, runSemanticSearch, semanticResults]);

  const closeDetailDialog = useCallback(() => {
    setDetailDialogMode('closed');
    setFormErrors({});
    setDetailLoading(false);
  }, []);

  const startCreateMemory = useCallback(() => {
    setSelectedMemoryId(null);
    setSelectedMemory(null);
    setMemoryForm(getInitialCreateMemoryFormState(memoryMetadata, subTypeOptionsByType, manualScopeOptions));
    setFormErrors({});
    setDetailDialogMode('create');
  }, [manualScopeOptions, memoryMetadata, subTypeOptionsByType]);

  const openMemory = useCallback(async (id: number) => {
    setSelectedMemoryId(id);
    setDetailDialogMode('edit');
    setDetailLoading(true);
    try {
      const memory = await memoryService.getById(id);
      setSelectedMemory(memory);
      setMemoryForm(mapMemoryToFormState(memory));
      setFormErrors({});
    } catch (error) {
      setDetailDialogMode('closed');
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.LOAD_FAILED)));
    } finally {
      setDetailLoading(false);
    }
  }, [t, toast]);

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
            : (nextSubTypeOptions[0] ?? getDefaultMemorySubtypeByType(nextMemoryType));
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

  const handleApplyFilters = (event: FormEvent) => {
    event.preventDefault();
    if (semanticSearchEnabled && filterForm.keyword.trim()) {
      void runSemanticSearch(filterForm.keyword.trim());
      return;
    }
    setSemanticResults(null);
    setFilters(buildFilterParams(filterForm, page.size));
  };

  const handleResetFilters = () => {
    setFilterForm(defaultFilterFormState);
    setFilters({ ...defaultMemoryListParams, size: page.size });
    setSemanticResults(null);
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
    if (detailDialogMode === 'create' && memoryForm.scope === 'WORKSPACE') {
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

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      const payload = buildMemoryPayload(memoryForm);
      const saved = isEditing
        ? await memoryService.update(selectedMemoryId as number, {
            memoryType: payload.memoryType,
            workspaceContextKey: payload.workspaceContextKey,
            workspaceLevel: payload.workspaceLevel,
            scope: payload.scope,
            subType: payload.subType,
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
      setMemoryForm(mapMemoryToFormState(saved));
      setDetailDialogMode('edit');
      toast.success(t(isEditing ? I18N_KEYS.MEMORY_PAGE.UPDATE_SUCCESS : I18N_KEYS.MEMORY_PAGE.CREATE_SUCCESS));
      await loadMemories();
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
      setMemoryForm(mapMemoryToFormState(memory));
      toast.success(t(I18N_KEYS.MEMORY_PAGE.ARCHIVE_SUCCESS));
      await loadMemories();
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
      setMemoryForm(mapMemoryToFormState(memory));
      toast.success(t(I18N_KEYS.MEMORY_PAGE.RESTORE_SUCCESS));
      await loadMemories();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.RESTORE_FAILED)));
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
      setSelectedMemoryId(null);
      setSelectedMemory(null);
      setFormErrors({});
      setSemanticResults((prev) => (prev ? prev.filter((item) => item.id !== selectedMemoryId) : null));
      closeDetailDialog();
      await loadMemories();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.DELETE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  const listItems = useMemo<MemoryListItemView[]>(() => {
    if (semanticResults != null) {
      return semanticResults.map((result) => ({
        id: result.id,
        title: getMemoryOptionLabel(t, result.memoryType),
        summary: result.content,
        tags: [getMemoryOptionLabel(t, result.memoryType)],
        statusLabel: `${t(I18N_KEYS.MEMORY_PAGE.SEARCH_SCORE)} ${result.score.toFixed(3)}`,
        statusToneClassName: 'border-sky-500/25 bg-sky-500/12 text-sky-300',
        sourceLabel: result.conversationId
          ? `${t(I18N_KEYS.MEMORY_PAGE.META_CONVERSATION)}: ${result.conversationId}`
          : undefined,
      }));
    }

    return page.records.map((memory) => ({
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
      workspaceBindingLabel: memory.scope === 'WORKSPACE'
        ? `${t(I18N_KEYS.MEMORY_PAGE.META_WORKSPACE_BINDING)}: ${formatWorkspaceBindingLabel(t, memory)}`
        : undefined,
      updatedAtLabel: `${t(I18N_KEYS.MEMORY_PAGE.META_UPDATED)}: ${formatDateTime(memory.updatedAt)}`,
    }));
  }, [page.records, semanticResults, t]);

  const detailMeta = selectedMemory ? (
    <div className="grid gap-3 rounded-2xl border theme-border bg-[color:var(--bg-main)]/45 p-4 sm:grid-cols-2">
      {[
        [t(I18N_KEYS.MEMORY_PAGE.META_CREATED), formatDateTime(selectedMemory.createdAt)],
        [t(I18N_KEYS.MEMORY_PAGE.META_UPDATED), formatDateTime(selectedMemory.updatedAt)],
        [t(I18N_KEYS.MEMORY_PAGE.META_ACCESS_COUNT), String(selectedMemory.accessCount ?? 0)],
        [t(I18N_KEYS.MEMORY_PAGE.META_LAST_ACCESSED), formatDateTime(selectedMemory.lastAccessedAt)],
        [t(I18N_KEYS.MEMORY_PAGE.META_SOURCE), getMemoryOptionLabel(t, selectedMemory.sourceType)],
        [t(I18N_KEYS.MEMORY_PAGE.FIELD_SUB_TYPE), getMemoryOptionLabel(t, selectedMemory.subType)],
        [t(I18N_KEYS.MEMORY_PAGE.META_SCOPE), getMemoryOptionLabel(t, selectedMemory.scope)],
        [t(I18N_KEYS.MEMORY_PAGE.META_WORKSPACE_BINDING), formatWorkspaceBindingLabel(t, selectedMemory)],
        [
          t(I18N_KEYS.MEMORY_PAGE.META_CONVERSATION),
          selectedMemory.conversationId == null ? '--' : String(selectedMemory.conversationId),
        ],
        [t(I18N_KEYS.MEMORY_PAGE.FIELD_EXPIRES_AT), formatDateTime(selectedMemory.expiresAt)],
      ].map(([label, value]) => (
        <div
          key={label}
          className={cn(
            'rounded-xl bg-[color:var(--bg-panel)]/60 p-3',
            label === t(I18N_KEYS.MEMORY_PAGE.FIELD_EXPIRES_AT) && 'sm:col-span-2',
          )}
        >
          <div className="text-xs uppercase tracking-wide theme-text-secondary">{label}</div>
          <div className="mt-1 text-sm theme-text-primary">{value}</div>
        </div>
      ))}
    </div>
  ) : null;

  const detailEditor = (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONVERSATION_ID)}</label>
          <Input value={memoryForm.conversationId} onChange={handleFormInputChange('conversationId')} placeholder="123" />
          {formErrors.conversationId ? <p className="text-sm text-destructive">{formErrors.conversationId}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_MEMORY_TYPE)}</label>
          <select
            className={MEMORY_FORM_SELECT_CLASS_NAME}
            value={memoryForm.memoryType}
            onChange={handleFormInputChange('memoryType')}
            disabled={metadataLoading}
          >
            {memoryTypeOptions.map((option) => (
              <option key={option} value={option}>
                {getMemoryOptionLabel(t, option)}
              </option>
            ))}
          </select>
          {formErrors.memoryType ? <p className="text-sm text-destructive">{formErrors.memoryType}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SUB_TYPE)}</label>
          <select
            className={MEMORY_FORM_SELECT_CLASS_NAME}
            value={memoryForm.subType}
            onChange={handleFormInputChange('subType')}
            disabled={metadataLoading}
          >
            <option value="">{t(I18N_KEYS.MEMORY_PAGE.SELECT_SUB_TYPE_PLACEHOLDER)}</option>
            {availableSubTypeOptions.map((option) => (
              <option key={option} value={option}>
                {getMemoryOptionLabel(t, option)}
              </option>
            ))}
          </select>
          {formErrors.subType ? <p className="text-sm text-destructive">{formErrors.subType}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SCOPE)}</label>
          <select
            className={MEMORY_FORM_SELECT_CLASS_NAME}
            value={memoryForm.scope}
            onChange={handleFormInputChange('scope')}
            disabled={metadataLoading || (isEditing && selectedMemory?.scope === 'WORKSPACE')}
          >
            {editorScopeOptions.map((option) => (
              <option key={option} value={option}>
                {getMemoryOptionLabel(t, option)}
              </option>
            ))}
          </select>
          {formErrors.scope ? <p className="text-sm text-destructive">{formErrors.scope}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SOURCE_TYPE)}</label>
          <select
            className={MEMORY_FORM_SELECT_CLASS_NAME}
            value={memoryForm.sourceType}
            onChange={handleFormInputChange('sourceType')}
            disabled={metadataLoading || (isEditing && selectedMemory?.scope === 'WORKSPACE')}
          >
            {sourceTypeOptions.map((option) => (
              <option key={option} value={option}>
                {getMemoryOptionLabel(t, option)}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-2 md:col-span-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_EXPIRES_AT)}</label>
          <Input type="datetime-local" value={memoryForm.expiresAt} onChange={handleFormInputChange('expiresAt')} />
        </div>

        {selectedMemory?.scope === 'WORKSPACE' ? (
          <>
            <div className="space-y-2">
              <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_WORKSPACE_LEVEL)}</label>
              <Input value={getMemoryOptionLabel(t, selectedMemory.workspaceLevel)} readOnly />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_WORKSPACE_CONTEXT)}</label>
              <Input value={selectedMemory.workspaceContextKey || '--'} readOnly />
            </div>
          </>
        ) : null}

        <div className="space-y-2 md:col-span-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_TITLE)}</label>
          <Input value={memoryForm.title} onChange={handleFormInputChange('title')} />
        </div>
        <div className="space-y-2 md:col-span-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_REASON)}</label>
          <Input value={memoryForm.reason} onChange={handleFormInputChange('reason')} />
        </div>
        <div className="space-y-2 md:col-span-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONTENT)}</label>
          <textarea
            className={cn(MEMORY_FORM_TEXTAREA_CLASS_NAME, 'min-h-[150px]')}
            value={memoryForm.content}
            onChange={handleFormInputChange('content')}
          />
          {formErrors.content ? <p className="text-sm text-destructive">{formErrors.content}</p> : null}
        </div>
        <div className="space-y-2 md:col-span-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_DETAIL_JSON)}</label>
          <textarea
            className={cn(MEMORY_FORM_TEXTAREA_CLASS_NAME, 'font-mono')}
            value={memoryForm.detailJson}
            onChange={handleFormInputChange('detailJson')}
          />
          {formErrors.detailJson ? <p className="text-sm text-destructive">{formErrors.detailJson}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_CONFIDENCE)}</label>
          <Input
            type="number"
            min="0"
            max="1"
            step="0.01"
            value={memoryForm.confidenceScore}
            onChange={handleFormInputChange('confidenceScore')}
          />
          {formErrors.confidenceScore ? <p className="text-sm text-destructive">{formErrors.confidenceScore}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.MEMORY_PAGE.FIELD_SALIENCE)}</label>
          <Input
            type="number"
            min="0"
            max="1"
            step="0.01"
            value={memoryForm.salienceScore}
            onChange={handleFormInputChange('salienceScore')}
          />
          {formErrors.salienceScore ? <p className="text-sm text-destructive">{formErrors.salienceScore}</p> : null}
        </div>
      </div>

      <div className="flex flex-wrap gap-2 border-t theme-border pt-4">
        <Button type="submit" disabled={submitting}>
          {isEditing ? t(I18N_KEYS.MEMORY_PAGE.UPDATE_ACTION) : t(I18N_KEYS.MEMORY_PAGE.CREATE_ACTION)}
        </Button>
        <Button type="button" variant="outline" disabled={submitting} onClick={closeDetailDialog}>
          {t(I18N_KEYS.COMMON.CLOSE)}
        </Button>
        {selectedMemory && selectedMemory.status === MEMORY_STATUS.ACTIVE ? (
          <Button type="button" variant="outline" disabled={submitting} onClick={handleArchive}>
            <Archive className="mr-2 h-4 w-4" />
            {t(I18N_KEYS.MEMORY_PAGE.ARCHIVE)}
          </Button>
        ) : null}
        {selectedMemory && selectedMemory.status === MEMORY_STATUS.ARCHIVED ? (
          <Button type="button" variant="outline" disabled={submitting} onClick={handleRestore}>
            <RotateCcw className="mr-2 h-4 w-4" />
            {t(I18N_KEYS.MEMORY_PAGE.RESTORE)}
          </Button>
        ) : null}
        {selectedMemory ? (
          <Button type="button" variant="destructive" disabled={submitting} onClick={handleDelete}>
            <Trash2 className="mr-2 h-4 w-4" />
            {t(I18N_KEYS.MEMORY_PAGE.DELETE)}
          </Button>
        ) : null}
      </div>
    </form>
  );

  return (
    <div className="space-y-6 pb-8 theme-text-primary">
      <MemoryOverviewHero
        title={t(I18N_KEYS.MEMORY_PAGE.PAGE_TITLE)}
        description={t(I18N_KEYS.MEMORY_PAGE.PAGE_DESC)}
        backLabel={t(I18N_KEYS.SETTINGS_PAGE.BACK_TO_WORKSPACE)}
        onBack={() => navigate('/')}
        actions={(
          <>
            <Button type="button" variant="outline" onClick={() => void refreshWorkspace()} disabled={listLoading || semanticLoading}>
              <RefreshCcw className="mr-2 h-4 w-4" />
              {t(I18N_KEYS.MEMORY_PAGE.REFRESH)}
            </Button>
            <Button type="button" onClick={startCreateMemory}>
              <Plus className="mr-2 h-4 w-4" />
              {t(I18N_KEYS.MEMORY_PAGE.NEW_MEMORY)}
            </Button>
          </>
        )}
      />

      <div className="grid gap-6 xl:grid-cols-[300px_minmax(0,1fr)]">
        <MemoryControlCenter
          title={t(I18N_KEYS.MEMORY_PAGE.SECTION_FILTERS_TITLE)}
          description={t(I18N_KEYS.MEMORY_PAGE.SECTION_FILTERS_DESC)}
        >
          <form onSubmit={handleApplyFilters} className="space-y-4">
            <div className="space-y-1">
              <div className="flex items-center justify-between gap-3">
                <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
                  {t(I18N_KEYS.MEMORY_PAGE.FILTER_KEYWORD)}
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setSemanticSearchEnabled((prev) => {
                      const next = !prev;
                      if (!next) {
                        setSemanticResults(null);
                      }
                      return next;
                    });
                  }}
                  className="inline-flex items-center gap-2 rounded-full border theme-border bg-[color:var(--bg-main)]/50 px-2.5 py-1 text-[11px] font-medium theme-text-secondary transition-colors hover:theme-text-primary"
                >
                  <Brain className={cn('h-3.5 w-3.5', semanticSearchEnabled && 'text-sky-300')} />
                  <span>{t(I18N_KEYS.MEMORY_PAGE.SEMANTIC_SEARCH_TOGGLE)}</span>
                  <span
                    className={cn(
                      'relative inline-flex h-5 w-9 shrink-0 rounded-full border transition-colors',
                      semanticSearchEnabled ? 'border-sky-400/50 bg-sky-500/30' : 'theme-border bg-[color:var(--bg-main)]/80',
                    )}
                  >
                    <span
                      className={cn(
                        'absolute top-0.5 h-4 w-4 rounded-full bg-white shadow-sm transition-all',
                        semanticSearchEnabled ? 'left-[18px]' : 'left-0.5',
                      )}
                    />
                  </span>
                </button>
              </div>
              <Input
                value={filterForm.keyword}
                onChange={handleFilterInputChange('keyword')}
                placeholder={t(
                  semanticSearchEnabled
                    ? I18N_KEYS.MEMORY_PAGE.SEARCH_PLACEHOLDER
                    : I18N_KEYS.MEMORY_PAGE.FILTER_KEYWORD,
                )}
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
                {t(I18N_KEYS.MEMORY_PAGE.FILTER_MEMORY_TYPE)}
              </label>
              <select
                className={MEMORY_FORM_SELECT_CLASS_NAME}
                value={filterForm.memoryType}
                onChange={handleFilterInputChange('memoryType')}
                disabled={metadataLoading}
              >
                <option value="">{t(I18N_KEYS.MEMORY_PAGE.PRESET_ALL)}</option>
                {filterMemoryTypeOptions.map((option) => (
                  <option key={option} value={option}>
                    {getMemoryOptionLabel(t, option)}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
                {t(I18N_KEYS.MEMORY_PAGE.FILTER_SCOPE)}
              </label>
              <select
                className={MEMORY_FORM_SELECT_CLASS_NAME}
                value={filterForm.scope}
                onChange={handleFilterInputChange('scope')}
                disabled={metadataLoading}
              >
                <option value="">{t(I18N_KEYS.MEMORY_PAGE.PRESET_ALL)}</option>
                {filterScopeOptions.map((option) => (
                  <option key={option} value={option}>
                    {getMemoryOptionLabel(t, option)}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
                {t(I18N_KEYS.MEMORY_PAGE.FILTER_STATUS)}
              </label>
              <select
                className={MEMORY_FORM_SELECT_CLASS_NAME}
                value={filterForm.status}
                onChange={handleFilterInputChange('status')}
              >
                <option value="">{t(I18N_KEYS.MEMORY_PAGE.PRESET_ALL)}</option>
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
        </MemoryControlCenter>

        <Card className="overflow-hidden border theme-border shadow-sm">
          <CardContent className="space-y-4 p-4 md:p-6">
            {(listLoading && !isSemanticMode) || semanticLoading ? (
              <div className="rounded-2xl border border-dashed theme-border p-6 text-sm theme-text-secondary">
                {t(I18N_KEYS.COMMON.LOADING)}
              </div>
            ) : listItems.length === 0 ? (
              <div className="flex min-h-[calc(100vh-320px)] w-full items-center justify-center">
                <div className="max-w-md px-6 text-center text-sm leading-6 theme-text-secondary">
                  {isSemanticMode ? t(I18N_KEYS.MEMORY_PAGE.SEARCH_EMPTY) : t(I18N_KEYS.MEMORY_PAGE.EMPTY)}
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                {listItems.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => void openMemory(item.id)}
                    className={cn(
                      'w-full rounded-2xl border theme-border bg-[linear-gradient(180deg,rgba(255,255,255,0.03),rgba(255,255,255,0.015))] p-4 text-left transition-all hover:border-primary/40 hover:bg-[var(--bg-hover)]',
                      selectedMemoryId === item.id && 'border-primary bg-primary/10 shadow-sm',
                    )}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold theme-text-primary">{item.title}</p>
                        <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] theme-text-secondary">
                          {item.tags.map((tag) => (
                            <span
                              key={`${item.id}-${tag}`}
                              className="rounded-full border border-primary/30 bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary"
                            >
                              {tag}
                            </span>
                          ))}
                        </div>
                      </div>
                      <span className={cn('shrink-0 rounded-full border px-2 py-1 text-[11px] font-medium', item.statusToneClassName)}>
                        {item.statusLabel}
                      </span>
                    </div>
                    <p className="mt-3 line-clamp-3 text-sm theme-text-secondary">{item.summary}</p>
                    <div className="mt-3 flex flex-wrap gap-3 text-[11px] theme-text-secondary">
                      {item.sourceLabel ? <span>{item.sourceLabel}</span> : null}
                      {item.workspaceBindingLabel ? <span>{item.workspaceBindingLabel}</span> : null}
                      {item.updatedAtLabel ? <span>{item.updatedAtLabel}</span> : null}
                    </div>
                  </button>
                ))}
              </div>
            )}

            {!isSemanticMode && listItems.length > 0 ? (
              <div className="flex items-center justify-between gap-2 border-t theme-border pt-3">
                <span className="text-xs theme-text-secondary">
                  {t(I18N_KEYS.EXPLORER.PAGES)}: {page.current} / {Math.max(page.pages, 1)}
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
            ) : null}
          </CardContent>
        </Card>
      </div>

      <Dialog open={isDialogOpen} onOpenChange={(open) => (!open ? closeDetailDialog() : null)}>
        <DialogContent className="max-h-[90vh] max-w-5xl overflow-hidden rounded-3xl border theme-border theme-bg-panel p-0 shadow-xl">
          <DialogHeader className="border-b theme-border px-6 py-5">
            <div className="flex flex-wrap items-start justify-between gap-3 pr-8">
              <div className="space-y-1">
                <DialogTitle className="text-xl theme-text-primary">
                  {isEditing ? t(I18N_KEYS.MEMORY_PAGE.DETAIL_EDIT_TITLE) : t(I18N_KEYS.MEMORY_PAGE.DETAIL_NEW_TITLE)}
                </DialogTitle>
                <DialogDescription className="theme-text-secondary">
                  {isEditing ? t(I18N_KEYS.MEMORY_PAGE.DETAIL_EDIT_DESC) : t(I18N_KEYS.MEMORY_PAGE.DETAIL_NEW_DESC)}
                </DialogDescription>
              </div>
              {isEditing && selectedMemory ? (
                <span className={cn('rounded-full border px-3 py-1 text-xs font-medium', getStatusToneClassName(selectedMemory.status))}>
                  {t(getStatusLabelKey(selectedMemory.status))}
                </span>
              ) : null}
            </div>
          </DialogHeader>
          <div className="max-h-[calc(90vh-104px)] overflow-y-auto px-6 py-5">
            {detailLoading ? (
              <div className="rounded-2xl border border-dashed theme-border p-6 text-sm theme-text-secondary">
                {t(I18N_KEYS.COMMON.LOADING)}
              </div>
            ) : (
              <div className="space-y-6">
                {detailEditor}
                {detailMeta}
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
