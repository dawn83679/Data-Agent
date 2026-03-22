import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { memoryService } from '../../services/memory.service';
import type { MemoryMetadataResponse } from '../../types/memory';
import { useMemoryCrudActions } from './useMemoryCrudActions';
import { useMemoryEditorFormState } from './useMemoryEditorFormState';

interface UseMemoryEditorStateArgs {
  memoryMetadata: MemoryMetadataResponse;
  subTypeOptionsByType: Record<string, string[]>;
  reloadMemories: () => Promise<void>;
  removeSemanticResult: (id: number) => void;
}

export function useMemoryEditorState({
  memoryMetadata,
  subTypeOptionsByType,
  reloadMemories,
  removeSemanticResult,
}: UseMemoryEditorStateArgs) {
  const { t } = useTranslation();
  const toast = useToast();
  const [submitting, setSubmitting] = useState(false);

  const formState = useMemoryEditorFormState({
    memoryMetadata,
    subTypeOptionsByType,
  });

  const openMemory = useCallback(async (id: number) => {
    formState.openMemoryShell(id);
    try {
      const memory = await memoryService.getById(id);
      formState.applyLoadedMemory(memory);
    } catch (error) {
      formState.failOpenMemory();
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.LOAD_FAILED)));
    } finally {
      formState.finishOpenMemory();
    }
  }, [formState, t, toast]);

  const actions = useMemoryCrudActions({
    memoryForm: formState.memoryForm,
    detailDialogMode: formState.detailDialogMode,
    subTypeOptionsByType,
    selectedMemoryId: formState.selectedMemoryId,
    isEditing: formState.isEditing,
    setFormErrors: formState.setFormErrors,
    setSubmitting,
    setSelectedMemoryId: formState.setSelectedMemoryId,
    setSelectedMemory: formState.setSelectedMemory,
    setMemoryForm: formState.setMemoryForm,
    setDetailDialogMode: formState.setDetailDialogMode,
    closeDetailDialog: formState.closeDetailDialog,
    reloadMemories,
    removeSemanticResult,
  });

  return {
    ...formState,
    submitting,
    openMemory,
    ...actions,
  };
}
