import { useCallback, useState } from 'react';
import { PermissionScopeType, type PermissionRule } from '../../types/permission';
import { PERMISSION_EDITOR_MODE } from './permissionPageConstants';
import type {
  PermissionEditorMode,
  PermissionFormErrors,
  PermissionFormState,
} from './permissionPageModels';
import { createDefaultPermissionFormState, mapPermissionRuleToFormState } from './permissionPageUtils';

interface UsePermissionEditorFormStateArgs {
  selectedScopeType: PermissionScopeType;
  selectedConversationId: number | null;
  setSelectedConversationId: React.Dispatch<React.SetStateAction<number | null>>;
}

export function usePermissionEditorFormState({
  selectedScopeType,
  selectedConversationId,
  setSelectedConversationId,
}: UsePermissionEditorFormStateArgs) {
  const [editorMode, setEditorMode] = useState<PermissionEditorMode>(PERMISSION_EDITOR_MODE.CLOSED);
  const [editingRule, setEditingRule] = useState<PermissionRule | null>(null);
  const [form, setForm] = useState<PermissionFormState>(() => createDefaultPermissionFormState());
  const [formErrors, setFormErrors] = useState<PermissionFormErrors>({});

  const dialogConversationId = selectedScopeType === PermissionScopeType.CONVERSATION ? selectedConversationId : null;

  const closeEditor = useCallback(() => {
    setEditorMode(PERMISSION_EDITOR_MODE.CLOSED);
    setEditingRule(null);
    setForm(createDefaultPermissionFormState());
    setFormErrors({});
  }, []);

  const handleStartCreate = useCallback(() => {
    setEditingRule(null);
    setForm(createDefaultPermissionFormState());
    setFormErrors({});
    setEditorMode(PERMISSION_EDITOR_MODE.CREATE);
  }, []);

  const handleEditRule = useCallback((rule: PermissionRule) => {
    setEditingRule(rule);
    setSelectedConversationId(rule.scopeType === PermissionScopeType.CONVERSATION ? rule.conversationId ?? null : null);
    setForm(mapPermissionRuleToFormState(rule));
    setFormErrors({});
    setEditorMode(PERMISSION_EDITOR_MODE.EDIT);
  }, [setSelectedConversationId]);

  const handleFormChange = useCallback((field: keyof PermissionFormState, value: string | boolean) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFormErrors((prev) => ({
      ...prev,
      connectionId: field === 'connectionId' ? undefined : prev.connectionId,
      catalogName: field === 'catalogName' || field === 'coverage' ? undefined : prev.catalogName,
      schemaName: field === 'schemaName' || field === 'coverage' ? undefined : prev.schemaName,
    }));
  }, []);

  const handleConversationChange = useCallback((value: string) => {
    setSelectedConversationId(value.trim() === '' ? null : Number(value));
    setFormErrors((prev) => ({ ...prev, conversationId: undefined }));
  }, [setSelectedConversationId]);

  return {
    editorMode,
    editingRule,
    form,
    formErrors,
    dialogConversationId,
    setFormErrors,
    closeEditor,
    handleStartCreate,
    handleEditRule,
    handleFormChange,
    handleConversationChange,
  };
}
