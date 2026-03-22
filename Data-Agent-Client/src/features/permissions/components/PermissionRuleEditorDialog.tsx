import type { ChangeEvent, FormEvent } from 'react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../../components/ui/Button';
import { WORKBENCH_DIALOG_BODY_CLASS, WORKBENCH_DIALOG_CHROME_CLASS } from '../../../constants/uiLayout';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/Dialog';
import { Input } from '../../../components/ui/Input';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { permissionCoverageToGrantPreset, permissionGrantPresetLabel, permissionScopeLabel } from '../../../lib/permissionDisplay';
import { cn } from '../../../lib/utils';
import { PermissionGrantCoverage, PermissionScopeType } from '../../../types/permission';
import { PERMISSION_EDITOR_MODE, PERMISSION_FORM_SELECT_CLASS_NAME } from '../permissionPageConstants';
import type {
  PermissionConnectionOption,
  PermissionConversationOption,
  PermissionEditorMode,
  PermissionFormErrors,
  PermissionFormState,
} from '../permissionPageModels';

interface PermissionRuleEditorDialogProps {
  open: boolean;
  mode: PermissionEditorMode;
  selectedScopeType: PermissionScopeType;
  dialogConversationId: number | null;
  conversationOptions: PermissionConversationOption[];
  form: PermissionFormState;
  formErrors: PermissionFormErrors;
  connectionOptions: PermissionConnectionOption[];
  coverageOptions: PermissionGrantCoverage[];
  submitting: boolean;
  onClose: () => void;
  onConversationChange: (value: string) => void;
  onFormChange: (field: keyof PermissionFormState, value: string | boolean) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void | Promise<void>;
}

export function PermissionRuleEditorDialog({
  open,
  mode,
  selectedScopeType,
  dialogConversationId,
  conversationOptions,
  form,
  formErrors,
  connectionOptions,
  coverageOptions,
  submitting,
  onClose,
  onConversationChange,
  onFormChange,
  onSubmit,
}: PermissionRuleEditorDialogProps) {
  const { t } = useTranslation();
  const isEditing = mode === PERMISSION_EDITOR_MODE.EDIT;
  const canSubmit = connectionOptions.length > 0
    && !(selectedScopeType === PermissionScopeType.CONVERSATION && dialogConversationId == null);

  const coverageHint = useMemo(() => {
    if (form.coverage === PermissionGrantCoverage.EXACT_TARGET) {
      return t(I18N_KEYS.PERMISSIONS_PAGE.EXACT_HINT);
    }
    if (form.coverage === PermissionGrantCoverage.DATABASE) {
      return t(I18N_KEYS.PERMISSIONS_PAGE.DATABASE_HINT);
    }
    return t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_HINT);
  }, [form.coverage, t]);

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => (!nextOpen ? onClose() : null)}>
      <DialogContent className={`max-w-3xl ${WORKBENCH_DIALOG_CHROME_CLASS}`}>
        <DialogHeader className="border-b theme-border px-6 py-5">
          <div className="flex flex-wrap items-start justify-between gap-3 pr-8">
            <div className="space-y-1">
              <DialogTitle className="text-xl theme-text-primary">
                {isEditing
                  ? t(I18N_KEYS.PERMISSIONS_PAGE.EDIT_TITLE)
                  : t(I18N_KEYS.PERMISSIONS_PAGE.CREATE_TITLE)}
              </DialogTitle>
              <DialogDescription className="theme-text-secondary">
                {isEditing
                  ? t(I18N_KEYS.PERMISSIONS_PAGE.EDIT_DESC)
                  : t(I18N_KEYS.PERMISSIONS_PAGE.CREATE_DESC)}
              </DialogDescription>
            </div>
            <span className="rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              {permissionScopeLabel(t, selectedScopeType)}
              {dialogConversationId != null ? ` #${dialogConversationId}` : ''}
            </span>
          </div>
        </DialogHeader>
        <div className={WORKBENCH_DIALOG_BODY_CLASS}>
          <form className="space-y-6" onSubmit={onSubmit}>
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <label className="text-sm font-medium theme-text-primary" htmlFor="permission-form-scope">
                  {t(I18N_KEYS.PERMISSIONS_PAGE.SCOPE_LABEL)}
                </label>
                <div
                  id="permission-form-scope"
                  className="rounded-md border theme-border px-3 py-2 text-sm theme-text-primary"
                >
                  {permissionScopeLabel(t, selectedScopeType)}
                </div>
              </div>

              {selectedScopeType === PermissionScopeType.CONVERSATION ? (
                <div className="space-y-2">
                  <label className="text-sm font-medium theme-text-primary" htmlFor="permission-form-conversation">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.CONVERSATION_LABEL)}
                  </label>
                  <select
                    id="permission-form-conversation"
                    className={cn(
                      PERMISSION_FORM_SELECT_CLASS_NAME,
                      formErrors.conversationId && 'border-red-500 focus-visible:ring-red-500',
                    )}
                    value={dialogConversationId == null ? '' : String(dialogConversationId)}
                    onChange={(event) => onConversationChange(event.target.value)}
                  >
                    <option value="">{t(I18N_KEYS.PERMISSIONS_PAGE.CONVERSATION_PLACEHOLDER)}</option>
                    {conversationOptions.map((conversation) => (
                      <option key={conversation.id} value={String(conversation.id)}>
                        {conversation.label}
                      </option>
                    ))}
                  </select>
                  {formErrors.conversationId ? <p className="text-sm text-destructive">{formErrors.conversationId}</p> : null}
                </div>
              ) : null}

              <div className="space-y-2">
                <label className="text-sm font-medium theme-text-primary" htmlFor="permission-form-connection">
                  {t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_LABEL)}
                </label>
                <select
                  id="permission-form-connection"
                  className={cn(
                    PERMISSION_FORM_SELECT_CLASS_NAME,
                    formErrors.connectionId && 'border-red-500 focus-visible:ring-red-500',
                  )}
                  value={form.connectionId}
                  onChange={(event) => onFormChange('connectionId', event.target.value)}
                >
                  <option value="">
                    {connectionOptions.length > 0
                      ? t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_LABEL)
                      : t(I18N_KEYS.COMMON.NO_CONNECTIONS)}
                  </option>
                  {connectionOptions.map((connection) => (
                    <option key={connection.id} value={String(connection.id)}>
                      {connection.name}
                    </option>
                  ))}
                </select>
                {formErrors.connectionId ? <p className="text-sm text-destructive">{formErrors.connectionId}</p> : null}
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium theme-text-primary" htmlFor="permission-form-coverage">
                  {t(I18N_KEYS.PERMISSIONS_PAGE.COVERAGE_LABEL)}
                </label>
                <select
                  id="permission-form-coverage"
                  className={PERMISSION_FORM_SELECT_CLASS_NAME}
                  value={form.coverage}
                  onChange={(event) => onFormChange('coverage', event.target.value as PermissionGrantCoverage)}
                >
                  {coverageOptions.map((coverage) => (
                    <option key={coverage} value={coverage}>
                      {permissionGrantPresetLabel(t, permissionCoverageToGrantPreset(coverage))}
                    </option>
                  ))}
                </select>
                <p className="text-sm theme-text-secondary">{coverageHint}</p>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium theme-text-primary">{t(I18N_KEYS.PERMISSIONS_PAGE.ENABLED_LABEL)}</label>
                <label className="flex h-10 items-center gap-3 rounded-md border theme-border px-3 text-sm">
                  <input
                    type="checkbox"
                    checked={form.enabled}
                    onChange={(event) => onFormChange('enabled', event.target.checked)}
                    className="h-4 w-4 rounded border-input text-primary focus:ring-ring"
                  />
                  <span className="theme-text-primary">
                    {form.enabled ? t(I18N_KEYS.PERMISSIONS_PAGE.ENABLE) : t(I18N_KEYS.PERMISSIONS_PAGE.DISABLE)}
                  </span>
                </label>
              </div>

              {form.coverage !== PermissionGrantCoverage.CONNECTION ? (
                <div className="space-y-2">
                  <label className="text-sm font-medium theme-text-primary" htmlFor="permission-form-database">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.DATABASE_LABEL)}
                  </label>
                  <Input
                    id="permission-form-database"
                    value={form.catalogName}
                    onChange={(event: ChangeEvent<HTMLInputElement>) => onFormChange('catalogName', event.target.value)}
                    placeholder={t(I18N_KEYS.PERMISSIONS_PAGE.DATABASE_PLACEHOLDER)}
                    className={cn(formErrors.catalogName && 'border-red-500 focus-visible:ring-red-500')}
                  />
                  {formErrors.catalogName ? <p className="text-sm text-destructive">{formErrors.catalogName}</p> : null}
                </div>
              ) : null}

              {form.coverage === PermissionGrantCoverage.EXACT_TARGET ? (
                <div className="space-y-2">
                  <label className="text-sm font-medium theme-text-primary" htmlFor="permission-form-schema">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.SCHEMA_LABEL)}
                  </label>
                  <Input
                    id="permission-form-schema"
                    value={form.schemaName}
                    onChange={(event: ChangeEvent<HTMLInputElement>) => onFormChange('schemaName', event.target.value)}
                    placeholder={t(I18N_KEYS.PERMISSIONS_PAGE.SCHEMA_PLACEHOLDER)}
                    className={cn(formErrors.schemaName && 'border-red-500 focus-visible:ring-red-500')}
                  />
                  {formErrors.schemaName ? <p className="text-sm text-destructive">{formErrors.schemaName}</p> : null}
                </div>
              ) : null}
            </div>

            <div className="flex flex-wrap gap-2 border-t theme-border pt-4">
              <Button type="submit" disabled={submitting || !canSubmit}>
                {isEditing
                  ? t(I18N_KEYS.PERMISSIONS_PAGE.SAVE_UPDATE)
                  : t(I18N_KEYS.PERMISSIONS_PAGE.SAVE_CREATE)}
              </Button>
              <Button type="button" variant="outline" disabled={submitting} onClick={onClose}>
                {t(I18N_KEYS.COMMON.CLOSE)}
              </Button>
            </div>
          </form>
        </div>
      </DialogContent>
    </Dialog>
  );
}
