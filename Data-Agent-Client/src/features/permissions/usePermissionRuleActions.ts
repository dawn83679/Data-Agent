import { useCallback, useMemo, useState } from 'react';
import type { Dispatch, FormEvent, SetStateAction } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useConfirmDialog } from '../../hooks/useConfirmDialog';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { permissionService } from '../../services/permission.service';
import { PermissionScopeType, type PermissionRule } from '../../types/permission';
import { PERMISSION_EDITOR_MODE } from './permissionPageConstants';
import type {
  PermissionEditorMode,
  PermissionFormErrors,
  PermissionFormState,
} from './permissionPageModels';
import { buildPermissionPayload, hasRuleDefinitionChanged } from './permissionPageUtils';

interface UsePermissionRuleActionsArgs {
  selectedScopeType: PermissionScopeType;
  dialogConversationId: number | null;
  editorMode: PermissionEditorMode;
  editingRule: PermissionRule | null;
  form: PermissionFormState;
  setFormErrors: Dispatch<SetStateAction<PermissionFormErrors>>;
  closeEditor: () => void;
  loadRules: () => Promise<void>;
  setRules: Dispatch<SetStateAction<PermissionRule[]>>;
}

export function usePermissionRuleActions({
  selectedScopeType,
  dialogConversationId,
  editorMode,
  editingRule,
  form,
  setFormErrors,
  closeEditor,
  loadRules,
  setRules,
}: UsePermissionRuleActionsArgs) {
  const { t } = useTranslation();
  const toast = useToast();
  const { confirm, confirmDialog } = useConfirmDialog();
  const [submitting, setSubmitting] = useState(false);
  const [toggleBusyId, setToggleBusyId] = useState<number | null>(null);
  const [deleteBusyId, setDeleteBusyId] = useState<number | null>(null);

  const validationMessages = useMemo(() => ({
    conversation: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_CONTEXT),
    connection: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_CONNECTION),
    database: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_DATABASE),
    schema: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_SCHEMA),
  }), [t]);

  const handleToggleRule = useCallback(async (rule: PermissionRule) => {
    if (toggleBusyId != null) {
      return;
    }

    setToggleBusyId(rule.id);
    try {
      await permissionService.setRuleEnabled(rule.id, !rule.enabled);
      setRules((prev) => prev.map((item) => (
        item.id === rule.id
          ? { ...item, enabled: !item.enabled }
          : item
      )));
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.TOGGLE_FAILED)));
    } finally {
      setToggleBusyId(null);
    }
  }, [setRules, t, toast, toggleBusyId]);

  const handleDeleteRule = useCallback(async (rule: PermissionRule) => {
    if (deleteBusyId != null) {
      return;
    }

    const confirmed = await confirm({
      title: t(I18N_KEYS.PERMISSIONS_PAGE.DELETE),
      description: t(I18N_KEYS.PERMISSIONS_PAGE.DELETE_CONFIRM, {
        target: rule.catalogName || rule.connectionName || `#${rule.connectionId}`,
      }),
      confirmLabel: t(I18N_KEYS.PERMISSIONS_PAGE.DELETE),
      confirmVariant: 'destructive',
    });
    if (!confirmed) {
      return;
    }

    setDeleteBusyId(rule.id);
    try {
      await permissionService.deleteRule(rule.id);
      setRules((prev) => prev.filter((item) => item.id !== rule.id));
      if (editingRule?.id === rule.id) {
        closeEditor();
      }
      toast.success(t(I18N_KEYS.PERMISSIONS_PAGE.DELETE_SUCCESS));
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.DELETE_FAILED)));
    } finally {
      setDeleteBusyId(null);
    }
  }, [closeEditor, confirm, deleteBusyId, editingRule?.id, setRules, t, toast]);

  const handleSubmit = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { payload, errors } = buildPermissionPayload(
      form,
      selectedScopeType,
      dialogConversationId,
      validationMessages,
    );
    setFormErrors(errors);
    if (!payload) {
      return;
    }

    setSubmitting(true);
    try {
      const savedRule = await permissionService.upsertRule(payload);
      if (editorMode === PERMISSION_EDITOR_MODE.EDIT && editingRule && hasRuleDefinitionChanged(editingRule, payload) && savedRule.id !== editingRule.id) {
        await permissionService.deleteRule(editingRule.id);
      }
      await loadRules();
      closeEditor();
      toast.success(t(
        editorMode === PERMISSION_EDITOR_MODE.EDIT
          ? I18N_KEYS.PERMISSIONS_PAGE.UPDATE_SUCCESS
          : I18N_KEYS.PERMISSIONS_PAGE.CREATE_SUCCESS,
      ));
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.SAVE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  }, [closeEditor, dialogConversationId, editingRule, editorMode, form, loadRules, selectedScopeType, setFormErrors, t, toast, validationMessages]);

  return {
    submitting,
    toggleBusyId,
    deleteBusyId,
    confirmDialog,
    handleToggleRule,
    handleDeleteRule,
    handleSubmit,
  };
}
