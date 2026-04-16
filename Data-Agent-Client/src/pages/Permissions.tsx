import { Plus, RefreshCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { Button } from '../components/ui/Button';
import { I18N_KEYS } from '../constants/i18nKeys';
import { ROUTES } from '../constants/routes';
import { WORKBENCH_GRID_LAYOUT_CLASS } from '../constants/uiLayout';
import { PermissionFilterWorkbench, PermissionRuleEditorDialog, PermissionRuleList } from '../features/permissions/components';
import { PERMISSION_EDITOR_MODE } from '../features/permissions/permissionPageConstants';
import { parseConversationId } from '../features/permissions/permissionPageUtils';
import { usePermissionWorkbench } from '../features/permissions/usePermissionWorkbench';
import { MemoryOverviewHero } from '../features/memory/components';

export default function Permissions() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const requestedConversationId = parseConversationId(searchParams.get('conversationId'));
  const workbench = usePermissionWorkbench(requestedConversationId);
  const isOrgCommon =
    useAuthStore((s) => s.workspaceType === 'ORGANIZATION' && s.workspaceOrgRole === 'COMMON');

  if (isOrgCommon) {
    return <Navigate to={ROUTES.HOME} replace />;
  }

  return (
    <div className="space-y-6 pb-8 theme-text-primary">
      <MemoryOverviewHero
        title={t(I18N_KEYS.PERMISSIONS_PAGE.TITLE)}
        backLabel={t(I18N_KEYS.SETTINGS_PAGE.BACK_TO_WORKSPACE)}
        onBack={() => navigate(ROUTES.HOME)}
        actions={(
          <>
            <Button type="button" variant="outline" onClick={() => void workbench.loadRules()} disabled={workbench.listLoading}>
              <RefreshCcw className="mr-2 h-4 w-4" />
              {t(I18N_KEYS.COMMON.REFRESH)}
            </Button>
            <Button type="button" onClick={workbench.handleStartCreate}>
              <Plus className="mr-2 h-4 w-4" />
              {t(I18N_KEYS.PERMISSIONS_PAGE.CREATE_RULE)}
            </Button>
          </>
        )}
      />

      <div className={WORKBENCH_GRID_LAYOUT_CLASS}>
        <PermissionFilterWorkbench
          selectedScopeType={workbench.selectedScopeType}
          filterForm={workbench.filterForm}
          connectionOptions={workbench.connectionOptions}
          coverageOptions={workbench.coverageOptions}
          onScopeTypeChange={workbench.handleScopeTypeChange}
          onFilterInputChange={workbench.handleFilterInputChange}
          onApplyFilters={workbench.handleApplyFilters}
          onResetFilters={workbench.handleResetFilters}
        />

        <PermissionRuleList
          rules={workbench.filteredRules}
          loading={workbench.listLoading}
          selectedScopeType={workbench.selectedScopeType}
          requestedConversationId={requestedConversationId}
          activeRuleId={workbench.editorMode === PERMISSION_EDITOR_MODE.EDIT ? workbench.editingRule?.id ?? null : null}
          toggleBusyId={workbench.toggleBusyId}
          deleteBusyId={workbench.deleteBusyId}
          onEdit={workbench.handleEditRule}
          onToggle={workbench.handleToggleRule}
          onDelete={workbench.handleDeleteRule}
        />
      </div>

      <PermissionRuleEditorDialog
        open={workbench.editorMode !== PERMISSION_EDITOR_MODE.CLOSED}
        mode={workbench.editorMode}
        selectedScopeType={workbench.selectedScopeType}
        dialogConversationId={workbench.dialogConversationId}
        conversationOptions={workbench.conversationOptions}
        form={workbench.form}
        formErrors={workbench.formErrors}
        connectionOptions={workbench.connectionOptions}
        coverageOptions={workbench.coverageOptions}
        submitting={workbench.submitting}
        onClose={workbench.closeEditor}
        onConversationChange={workbench.handleConversationChange}
        onFormChange={workbench.handleFormChange}
        onSubmit={workbench.handleSubmit}
      />
      {workbench.confirmDialog}
    </div>
  );
}
