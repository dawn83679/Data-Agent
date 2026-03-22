import { PermissionScopeType, type PermissionRule } from '../../types/permission';
import type { Dispatch, SetStateAction } from 'react';
import { usePermissionEditorFormState } from './usePermissionEditorFormState';
import { usePermissionRuleActions } from './usePermissionRuleActions';

interface UsePermissionEditorStateArgs {
  selectedScopeType: PermissionScopeType;
  selectedConversationId: number | null;
  loadRules: () => Promise<void>;
  setRules: Dispatch<SetStateAction<PermissionRule[]>>;
  setSelectedConversationId: Dispatch<SetStateAction<number | null>>;
}

export function usePermissionEditorState({
  selectedScopeType,
  selectedConversationId,
  loadRules,
  setRules,
  setSelectedConversationId,
}: UsePermissionEditorStateArgs) {
  const formState = usePermissionEditorFormState({
    selectedScopeType,
    selectedConversationId,
    setSelectedConversationId,
  });

  const actions = usePermissionRuleActions({
    selectedScopeType,
    dialogConversationId: formState.dialogConversationId,
    editorMode: formState.editorMode,
    editingRule: formState.editingRule,
    form: formState.form,
    setFormErrors: formState.setFormErrors,
    closeEditor: formState.closeEditor,
    loadRules,
    setRules,
  });

  return {
    ...formState,
    ...actions,
  };
}
