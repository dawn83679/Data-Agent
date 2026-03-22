import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ChangeEvent } from 'react';
import {
  MEMORY_MANUAL_SCOPE_OPTIONS,
  type Memory,
  type MemoryMetadataResponse,
} from '../../types/memory';
import {
  MEMORY_DEFAULT_MANUAL_SCOPE,
  MEMORY_DEFAULT_SOURCE_TYPE,
  MEMORY_DEFAULT_TYPE,
  MEMORY_DIALOG_MODE,
} from './memoryPageConstants';
import type { MemoryDialogMode, MemoryFormState } from './memoryPageModels';
import {
  appendOptionIfMissing,
  buildFallbackMemoryMetadata,
  getDefaultMemorySubtypeByType,
  getInitialCreateMemoryFormState,
  mapMemoryToFormState,
} from './memoryPageUtils';

const fallbackMemoryMetadata = buildFallbackMemoryMetadata();

interface UseMemoryEditorFormStateArgs {
  memoryMetadata: MemoryMetadataResponse;
  subTypeOptionsByType: Record<string, string[]>;
}

export function useMemoryEditorFormState({
  memoryMetadata,
  subTypeOptionsByType,
}: UseMemoryEditorFormStateArgs) {
  const [memoryForm, setMemoryForm] = useState<MemoryFormState>(() =>
    getInitialCreateMemoryFormState(fallbackMemoryMetadata, subTypeOptionsByType, [...MEMORY_MANUAL_SCOPE_OPTIONS]),
  );
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [detailDialogMode, setDetailDialogMode] = useState<MemoryDialogMode>(MEMORY_DIALOG_MODE.CLOSED);
  const [selectedMemoryId, setSelectedMemoryId] = useState<number | null>(null);
  const [selectedMemory, setSelectedMemory] = useState<Memory | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const isDialogOpen = detailDialogMode !== MEMORY_DIALOG_MODE.CLOSED;
  const isEditing = detailDialogMode === MEMORY_DIALOG_MODE.EDIT && selectedMemoryId != null;

  const memoryTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.memoryTypes.map((item) => item.code), memoryForm.memoryType),
    [memoryForm.memoryType, memoryMetadata.memoryTypes],
  );
  const scopeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.scopes, memoryForm.scope),
    [memoryForm.scope, memoryMetadata.scopes],
  );
  const sourceTypeOptions = useMemo(
    () => appendOptionIfMissing(memoryMetadata.sourceTypes, memoryForm.sourceType),
    [memoryForm.sourceType, memoryMetadata.sourceTypes],
  );
  const manualScopeOptions = useMemo(
    () => (scopeOptions.length > 0 ? scopeOptions : [MEMORY_DEFAULT_MANUAL_SCOPE]),
    [scopeOptions],
  );
  const availableSubTypeOptions = useMemo(() => {
    const baseOptions = subTypeOptionsByType[memoryForm.memoryType] ?? [];
    return appendOptionIfMissing(baseOptions, memoryForm.subType);
  }, [memoryForm.memoryType, memoryForm.subType, subTypeOptionsByType]);
  const editorScopeOptions = manualScopeOptions;

  useEffect(() => {
    const allowedMemoryTypes = memoryMetadata.memoryTypes.map((item) => item.code);
    const allowedScopes = memoryMetadata.scopes;
    const allowedSourceTypes = memoryMetadata.sourceTypes;

    setMemoryForm((prev) => {
      const nextMemoryType = allowedMemoryTypes.includes(prev.memoryType)
        ? prev.memoryType
        : (allowedMemoryTypes[0] ?? MEMORY_DEFAULT_TYPE);
      const nextSubTypeOptions = subTypeOptionsByType[nextMemoryType] ?? [];
      const nextSubType = prev.subType && nextSubTypeOptions.includes(prev.subType)
        ? prev.subType
        : (nextSubTypeOptions[0] ?? getDefaultMemorySubtypeByType(nextMemoryType));
      const nextScope = allowedScopes.includes(prev.scope)
        ? prev.scope
        : (allowedScopes[0] ?? MEMORY_DEFAULT_MANUAL_SCOPE);
      const nextSourceType = allowedSourceTypes.includes(prev.sourceType)
        ? prev.sourceType
        : (allowedSourceTypes[0] ?? MEMORY_DEFAULT_SOURCE_TYPE);
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
  }, [memoryMetadata, subTypeOptionsByType]);

  const closeDetailDialog = useCallback(() => {
    setDetailDialogMode(MEMORY_DIALOG_MODE.CLOSED);
    setFormErrors({});
    setDetailLoading(false);
  }, []);

  const startCreateMemory = useCallback(() => {
    setSelectedMemoryId(null);
    setSelectedMemory(null);
    setMemoryForm(getInitialCreateMemoryFormState(memoryMetadata, subTypeOptionsByType, manualScopeOptions));
    setFormErrors({});
    setDetailDialogMode(MEMORY_DIALOG_MODE.CREATE);
  }, [manualScopeOptions, memoryMetadata, subTypeOptionsByType]);

  const applyLoadedMemory = useCallback((memory: Memory) => {
    setSelectedMemory(memory);
    setMemoryForm(mapMemoryToFormState(memory));
    setFormErrors({});
    setDetailDialogMode(MEMORY_DIALOG_MODE.EDIT);
  }, []);

  const openMemoryShell = useCallback((id: number) => {
    setSelectedMemoryId(id);
    setDetailDialogMode(MEMORY_DIALOG_MODE.EDIT);
    setDetailLoading(true);
  }, []);

  const finishOpenMemory = useCallback(() => {
    setDetailLoading(false);
  }, []);

  const failOpenMemory = useCallback(() => {
    setDetailDialogMode(MEMORY_DIALOG_MODE.CLOSED);
    setDetailLoading(false);
  }, []);

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

  return {
    memoryForm,
    setMemoryForm,
    formErrors,
    setFormErrors,
    detailDialogMode,
    setDetailDialogMode,
    selectedMemoryId,
    setSelectedMemoryId,
    selectedMemory,
    setSelectedMemory,
    detailLoading,
    isDialogOpen,
    isEditing,
    memoryTypeOptions,
    availableSubTypeOptions,
    editorScopeOptions,
    sourceTypeOptions,
    closeDetailDialog,
    startCreateMemory,
    applyLoadedMemory,
    openMemoryShell,
    finishOpenMemory,
    failOpenMemory,
    handleFormInputChange,
  };
}
