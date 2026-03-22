import { useTranslation } from 'react-i18next';
import type { Dispatch, FormEvent, SetStateAction } from 'react';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { memoryService } from '../../services/memory.service';
import type { Memory } from '../../types/memory';
import { MEMORY_DIALOG_MODE } from './memoryPageConstants';
import type { MemoryDialogMode, MemoryFormState } from './memoryPageModels';
import { buildMemoryPayload, mapMemoryToFormState, validateMemoryForm } from './memoryPageUtils';

interface UseMemoryCrudActionsArgs {
  memoryForm: MemoryFormState;
  detailDialogMode: MemoryDialogMode;
  subTypeOptionsByType: Record<string, string[]>;
  selectedMemoryId: number | null;
  isEditing: boolean;
  setFormErrors: Dispatch<SetStateAction<Record<string, string>>>;
  setSubmitting: Dispatch<SetStateAction<boolean>>;
  setSelectedMemoryId: Dispatch<SetStateAction<number | null>>;
  setSelectedMemory: Dispatch<SetStateAction<Memory | null>>;
  setMemoryForm: Dispatch<SetStateAction<MemoryFormState>>;
  setDetailDialogMode: Dispatch<SetStateAction<MemoryDialogMode>>;
  closeDetailDialog: () => void;
  reloadMemories: () => Promise<void>;
  removeSemanticResult: (id: number) => void;
}

export function useMemoryCrudActions({
  memoryForm,
  detailDialogMode,
  subTypeOptionsByType,
  selectedMemoryId,
  isEditing,
  setFormErrors,
  setSubmitting,
  setSelectedMemoryId,
  setSelectedMemory,
  setMemoryForm,
  setDetailDialogMode,
  closeDetailDialog,
  reloadMemories,
  removeSemanticResult,
}: UseMemoryCrudActionsArgs) {
  const { t } = useTranslation();
  const toast = useToast();

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const errors = validateMemoryForm(memoryForm, detailDialogMode, subTypeOptionsByType, t);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
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
      setDetailDialogMode(MEMORY_DIALOG_MODE.EDIT);
      toast.success(t(isEditing ? I18N_KEYS.MEMORY_PAGE.UPDATE_SUCCESS : I18N_KEYS.MEMORY_PAGE.CREATE_SUCCESS));
      await reloadMemories();
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
      await reloadMemories();
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
      await reloadMemories();
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
      removeSemanticResult(selectedMemoryId);
      closeDetailDialog();
      await reloadMemories();
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.DELETE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  };

  return {
    handleSubmit,
    handleArchive,
    handleRestore,
    handleDelete,
  };
}
