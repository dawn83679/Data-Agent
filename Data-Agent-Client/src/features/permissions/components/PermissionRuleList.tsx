import { Pencil, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../../components/ui/Button';
import { Card, CardContent } from '../../../components/ui/Card';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { WORKBENCH_EMPTY_STATE_CLASS } from '../../../constants/uiLayout';
import {
  permissionGrantPresetLabel,
  permissionRuleLocation,
  permissionRuleSummary,
  permissionScopeLabel,
} from '../../../lib/permissionDisplay';
import { cn } from '../../../lib/utils';
import { PermissionScopeType, type PermissionRule } from '../../../types/permission';
import { formatPermissionCreatedAt } from '../permissionPageUtils';

interface PermissionRuleListProps {
  rules: PermissionRule[];
  loading: boolean;
  selectedScopeType: PermissionScopeType;
  requestedConversationId: number | null;
  activeRuleId: number | null;
  toggleBusyId: number | null;
  deleteBusyId: number | null;
  onEdit: (rule: PermissionRule) => void;
  onToggle: (rule: PermissionRule) => void | Promise<void>;
  onDelete: (rule: PermissionRule) => void | Promise<void>;
}

export function PermissionRuleList({
  rules,
  loading,
  selectedScopeType,
  requestedConversationId,
  activeRuleId,
  toggleBusyId,
  deleteBusyId,
  onEdit,
  onToggle,
  onDelete,
}: PermissionRuleListProps) {
  const { t } = useTranslation();
  const listScopeLabel = permissionScopeLabel(t, selectedScopeType);

  return (
    <Card className="overflow-hidden border theme-border shadow-sm">
      <CardContent className="space-y-4 p-4 md:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="space-y-1">
            <h2 className="text-lg font-semibold theme-text-primary">{t(I18N_KEYS.PERMISSIONS_PAGE.LIST_TITLE)}</h2>
            <p className="text-sm theme-text-secondary">{t(I18N_KEYS.PERMISSIONS_PAGE.LIST_DESC)}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full border border-primary/30 bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
              {listScopeLabel}
            </span>
            {selectedScopeType === PermissionScopeType.CONVERSATION && requestedConversationId != null ? (
              <span className="rounded-full border theme-border px-2 py-0.5 text-[11px] theme-text-secondary">
                #{requestedConversationId}
              </span>
            ) : null}
            <span className="rounded-full border theme-border px-3 py-1 text-[11px] theme-text-secondary">
              {t(I18N_KEYS.PERMISSIONS_PAGE.LIST_COUNT, { count: rules.length })}
            </span>
          </div>
        </div>

        {loading ? (
          <div className="rounded-2xl border border-dashed theme-border p-6 text-sm theme-text-secondary">
            {t(I18N_KEYS.AI.PERMISSION.LOADING)}
          </div>
        ) : rules.length === 0 ? (
          <div className={WORKBENCH_EMPTY_STATE_CLASS}>
            <div className="max-w-md px-6 text-center text-sm leading-6 theme-text-secondary">
              {t(I18N_KEYS.PERMISSIONS_PAGE.EMPTY_DESC)}
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {rules.map((rule) => {
              const createdAt = formatPermissionCreatedAt(rule.createdAt);
              const isSelected = activeRuleId === rule.id;
              const isToggleBusy = toggleBusyId === rule.id;
              const isDeleteBusy = deleteBusyId === rule.id;

              return (
                <div
                  key={rule.id}
                  className={cn(
                    'rounded-2xl border theme-border bg-[linear-gradient(180deg,rgba(255,255,255,0.03),rgba(255,255,255,0.015))] p-4 transition-all',
                    isSelected && 'border-primary bg-primary/10 shadow-sm',
                  )}
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 space-y-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="truncate text-sm font-semibold theme-text-primary">
                          {rule.connectionName || `${t(I18N_KEYS.PERMISSIONS_PAGE.UNKNOWN_CONNECTION)} #${rule.connectionId}`}
                        </p>
                        <span className="rounded-full border border-primary/30 bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
                          {permissionScopeLabel(t, rule.scopeType)}
                        </span>
                        {rule.conversationId != null ? (
                          <span className="rounded-full border theme-border px-2 py-0.5 text-[11px] theme-text-secondary">
                            #{rule.conversationId}
                          </span>
                        ) : null}
                        <span
                          className={cn(
                            'rounded-full border px-2 py-0.5 text-[11px] font-medium',
                            rule.enabled
                              ? 'border-emerald-500/25 bg-emerald-500/12 text-emerald-300'
                              : 'theme-border bg-[color:var(--bg-main)]/70 theme-text-secondary',
                          )}
                        >
                          {rule.enabled ? t(I18N_KEYS.AI.PERMISSION.ENABLED) : t(I18N_KEYS.AI.PERMISSION.DISABLED)}
                        </span>
                      </div>

                      <p className="text-sm leading-6 theme-text-primary">{permissionRuleSummary(t, rule)}</p>

                      <div className="flex flex-wrap gap-3 text-[11px] theme-text-secondary">
                        <span>{permissionGrantPresetLabel(t, rule.grantPreset)}</span>
                        <span>{permissionRuleLocation(t, rule)}</span>
                        {createdAt ? <span>{t(I18N_KEYS.PERMISSIONS_PAGE.CREATED_AT, { value: createdAt })}</span> : null}
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => onEdit(rule)}
                        className="gap-2"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                        {t(I18N_KEYS.PERMISSIONS_PAGE.EDIT)}
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => void onToggle(rule)}
                        disabled={isToggleBusy}
                      >
                        {rule.enabled
                          ? t(I18N_KEYS.PERMISSIONS_PAGE.DISABLE)
                          : t(I18N_KEYS.PERMISSIONS_PAGE.ENABLE)}
                      </Button>
                      <Button
                        type="button"
                        variant="destructive"
                        size="sm"
                        onClick={() => void onDelete(rule)}
                        disabled={isDeleteBusy}
                        className="gap-2"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        {t(I18N_KEYS.PERMISSIONS_PAGE.DELETE)}
                      </Button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
