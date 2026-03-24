import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { memoryService } from '../../services/memory.service';
import type { MemoryMetadataResponse, MemoryPage, MemoryScope, MemorySearchResult } from '../../types/memory';
import { useToast } from '../../hooks/useToast';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { MEMORY_SEMANTIC_SEARCH_LIMIT, MEMORY_SEMANTIC_SEARCH_MIN_SCORE } from './memoryPageConstants';
import type { FilterFormState } from './memoryPageModels';
import {
  appendOptionIfMissing,
  buildFilterParams,
  buildMemoryListItems,
  defaultFilterFormState,
  defaultMemoryListParams,
  defaultMemoryPage,
} from './memoryPageUtils';

export function useMemoryListState(memoryMetadata: MemoryMetadataResponse) {
  const { t } = useTranslation();
  const toast = useToast();
  const [page, setPage] = useState<MemoryPage>(defaultMemoryPage);
  const [filters, setFilters] = useState(defaultMemoryListParams);
  const [filterForm, setFilterForm] = useState<FilterFormState>(defaultFilterFormState);
  const [semanticResults, setSemanticResults] = useState<MemorySearchResult[] | null>(null);
  const [semanticSearchEnabled, setSemanticSearchEnabled] = useState(false);
  const [listLoading, setListLoading] = useState(false);
  const [semanticLoading, setSemanticLoading] = useState(false);

  const filterMemoryTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.memoryTypes.map((item) => item.code), filterForm.memoryType),
    [filterForm.memoryType, memoryMetadata.memoryTypes],
  );
  const filterScopeOptions = useMemo(() => {
    const scopeOptions = appendOptionIfMissing(memoryMetadata.scopes, filterForm.scope);
    return scopeOptions as MemoryScope[];
  }, [filterForm.scope, memoryMetadata.scopes]);

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

  const runSemanticSearch = useCallback(async (query: string, filters: FilterFormState) => {
    setSemanticLoading(true);
    try {
      const results = await memoryService.search({
        queryText: query,
        limit: MEMORY_SEMANTIC_SEARCH_LIMIT,
        minScore: MEMORY_SEMANTIC_SEARCH_MIN_SCORE,
        memoryType: filters.memoryType || undefined,
        scope: filters.scope || undefined,
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
      await runSemanticSearch(filterForm.keyword.trim(), filterForm);
      return;
    }
    await loadMemories();
  }, [filterForm, loadMemories, runSemanticSearch, semanticResults]);

  const handleFilterInputChange =
    (field: keyof FilterFormState) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setFilterForm((prev) => ({ ...prev, [field]: event.target.value }));
    };

  const handleApplyFilters = (event: FormEvent) => {
    event.preventDefault();
    if (semanticSearchEnabled && filterForm.keyword.trim()) {
      void runSemanticSearch(filterForm.keyword.trim(), filterForm);
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

  const listItems = useMemo(
    () => buildMemoryListItems(page.records, semanticResults, t),
    [page.records, semanticResults, t],
  );

  const handleSemanticToggle = () => {
    setSemanticSearchEnabled((prev) => {
      const next = !prev;
      if (!next) {
        setSemanticResults(null);
      }
      return next;
    });
  };

  const goToPreviousPage = () => {
    setFilters((prev) => ({ ...prev, current: Math.max(1, (prev.current ?? 1) - 1) }));
  };

  const goToNextPage = () => {
    setFilters((prev) => ({ ...prev, current: (prev.current ?? 1) + 1 }));
  };

  const removeSemanticResult = (id: number) => {
    setSemanticResults((prev) => (prev ? prev.filter((item) => item.id !== id) : null));
  };

  return {
    page,
    filterForm,
    filterMemoryTypeOptions,
    filterScopeOptions,
    listItems,
    listLoading,
    semanticLoading,
    semanticSearchEnabled,
    isSemanticMode: semanticLoading || semanticResults != null,
    loadMemories,
    refreshWorkspace,
    handleFilterInputChange,
    handleApplyFilters,
    handleResetFilters,
    handleSemanticToggle,
    goToPreviousPage,
    goToNextPage,
    removeSemanticResult,
  };
}
