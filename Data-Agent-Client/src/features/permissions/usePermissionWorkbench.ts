import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { PermissionScopeType } from '../../types/permission';
import { usePermissionConversationState } from './usePermissionConversationState';
import { usePermissionEditorState } from './usePermissionEditorState';
import { usePermissionListState } from './usePermissionListState';
import {
  buildPermissionConnectionOptions,
  buildPermissionConversationOptions,
} from './permissionPageUtils';

export function usePermissionWorkbench(requestedConversationId: number | null) {
  const { t } = useTranslation();
  const [selectedScopeType, setSelectedScopeType] = useState<PermissionScopeType>(
    () => requestedConversationId != null ? PermissionScopeType.CONVERSATION : PermissionScopeType.USER,
  );

  useEffect(() => {
    setSelectedScopeType(requestedConversationId != null ? PermissionScopeType.CONVERSATION : PermissionScopeType.USER);
  }, [requestedConversationId]);

  const listState = usePermissionListState(requestedConversationId, selectedScopeType);
  const conversationState = usePermissionConversationState(requestedConversationId);
  const editorState = usePermissionEditorState({
    selectedScopeType,
    selectedConversationId: conversationState.selectedConversationId,
    loadRules: listState.loadRules,
    setRules: listState.setRules,
    setSelectedConversationId: conversationState.setSelectedConversationId,
  });

  const connectionOptions = useMemo(
    () => buildPermissionConnectionOptions(
      listState.connections,
      listState.rules,
      editorState.editingRule,
      t(I18N_KEYS.PERMISSIONS_PAGE.UNKNOWN_CONNECTION),
    ),
    [editorState.editingRule, listState.connections, listState.rules, t],
  );

  const conversationOptions = useMemo(
    () => buildPermissionConversationOptions(
      conversationState.conversations,
      editorState.editingRule?.conversationId,
      requestedConversationId,
    ),
    [conversationState.conversations, editorState.editingRule?.conversationId, requestedConversationId],
  );

  const handleScopeTypeChange = useCallback((scopeType: PermissionScopeType) => {
    setSelectedScopeType(scopeType);
    conversationState.setSelectedConversationId(
      scopeType === PermissionScopeType.CONVERSATION ? requestedConversationId : null,
    );
    editorState.closeEditor();
  }, [conversationState, editorState, requestedConversationId]);

  return {
    selectedScopeType,
    connectionOptions,
    conversationOptions,
    coverageOptions: listState.coverageOptions,
    filterForm: listState.filterForm,
    filteredRules: listState.filteredRules,
    listLoading: listState.listLoading,
    submitting: editorState.submitting,
    toggleBusyId: editorState.toggleBusyId,
    deleteBusyId: editorState.deleteBusyId,
    editorMode: editorState.editorMode,
    editingRule: editorState.editingRule,
    form: editorState.form,
    formErrors: editorState.formErrors,
    dialogConversationId:
      selectedScopeType === PermissionScopeType.CONVERSATION ? conversationState.selectedConversationId : null,
    loadRules: listState.loadRules,
    closeEditor: editorState.closeEditor,
    handleScopeTypeChange,
    handleFilterInputChange: listState.handleFilterInputChange,
    handleApplyFilters: listState.handleApplyFilters,
    handleResetFilters: listState.handleResetFilters,
    handleStartCreate: editorState.handleStartCreate,
    handleEditRule: editorState.handleEditRule,
    handleToggleRule: editorState.handleToggleRule,
    handleDeleteRule: editorState.handleDeleteRule,
    handleConversationChange: editorState.handleConversationChange,
    handleFormChange: editorState.handleFormChange,
    handleSubmit: editorState.handleSubmit,
  };
}
