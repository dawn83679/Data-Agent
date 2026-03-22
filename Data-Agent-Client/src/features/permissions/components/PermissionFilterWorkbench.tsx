import type { ChangeEvent, FormEvent } from 'react';
import { Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { MemoryControlCenter } from '../../memory/components';
import { permissionCoverageToGrantPreset, permissionGrantPresetLabel, permissionScopeLabel } from '../../../lib/permissionDisplay';
import { PermissionGrantCoverage, PermissionScopeType } from '../../../types/permission';
import type {
  PermissionConnectionOption,
  PermissionFilterFormState,
} from '../permissionPageModels';
import {
  PERMISSION_FILTER_VALUE,
  PERMISSION_FORM_SELECT_CLASS_NAME,
  PERMISSION_STATUS_FILTER,
} from '../permissionPageConstants';

interface PermissionFilterWorkbenchProps {
  selectedScopeType: PermissionScopeType;
  filterForm: PermissionFilterFormState;
  connectionOptions: PermissionConnectionOption[];
  coverageOptions: PermissionGrantCoverage[];
  onScopeTypeChange: (scopeType: PermissionScopeType) => void;
  onFilterInputChange: (
    field: keyof PermissionFilterFormState,
  ) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  onApplyFilters: (event: FormEvent) => void;
  onResetFilters: () => void;
}

export function PermissionFilterWorkbench({
  selectedScopeType,
  filterForm,
  connectionOptions,
  coverageOptions,
  onScopeTypeChange,
  onFilterInputChange,
  onApplyFilters,
  onResetFilters,
}: PermissionFilterWorkbenchProps) {
  const { t } = useTranslation();

  return (
    <MemoryControlCenter
      title={t(I18N_KEYS.PERMISSIONS_PAGE.FILTERS_TITLE)}
      description={t(I18N_KEYS.PERMISSIONS_PAGE.FILTERS_DESC)}
    >
      <div className="space-y-2">
        <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
          {t(I18N_KEYS.PERMISSIONS_PAGE.SCOPE_LABEL)}
        </label>
        <div className="flex rounded-xl border theme-border bg-[color:var(--bg-main)]/50 p-1">
          <Button
            type="button"
            variant={selectedScopeType === PermissionScopeType.USER ? 'default' : 'ghost'}
            size="sm"
            className="flex-1"
            onClick={() => onScopeTypeChange(PermissionScopeType.USER)}
          >
            {permissionScopeLabel(t, PermissionScopeType.USER)}
          </Button>
          <Button
            type="button"
            variant={selectedScopeType === PermissionScopeType.CONVERSATION ? 'default' : 'ghost'}
            size="sm"
            className="flex-1"
            onClick={() => onScopeTypeChange(PermissionScopeType.CONVERSATION)}
          >
            {permissionScopeLabel(t, PermissionScopeType.CONVERSATION)}
          </Button>
        </div>
      </div>

      <form onSubmit={onApplyFilters} className="space-y-4">
        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary" htmlFor="permission-search">
            {t(I18N_KEYS.COMMON.SEARCH)}
          </label>
          <Input
            id="permission-search"
            value={filterForm.searchText}
            onChange={onFilterInputChange('searchText')}
            placeholder={t(I18N_KEYS.PERMISSIONS_PAGE.SEARCH_PLACEHOLDER)}
          />
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary" htmlFor="permission-filter-connection">
            {t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_LABEL)}
          </label>
          <select
            id="permission-filter-connection"
            className={PERMISSION_FORM_SELECT_CLASS_NAME}
            value={filterForm.connectionId}
            onChange={onFilterInputChange('connectionId')}
          >
            <option value={PERMISSION_FILTER_VALUE.ALL}>{t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_ALL)}</option>
            {connectionOptions.map((connection) => (
              <option key={connection.id} value={String(connection.id)}>
                {connection.name}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary" htmlFor="permission-filter-coverage">
            {t(I18N_KEYS.PERMISSIONS_PAGE.COVERAGE_LABEL)}
          </label>
          <select
            id="permission-filter-coverage"
            className={PERMISSION_FORM_SELECT_CLASS_NAME}
            value={filterForm.coverage}
            onChange={onFilterInputChange('coverage')}
          >
            <option value={PERMISSION_FILTER_VALUE.ALL}>{t(I18N_KEYS.PERMISSIONS_PAGE.COVERAGE_ALL)}</option>
            {coverageOptions.map((coverage) => (
              <option key={coverage} value={coverage}>
                {permissionGrantPresetLabel(t, permissionCoverageToGrantPreset(coverage))}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary" htmlFor="permission-filter-status">
            {t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_LABEL)}
          </label>
          <select
            id="permission-filter-status"
            className={PERMISSION_FORM_SELECT_CLASS_NAME}
            value={filterForm.status}
            onChange={onFilterInputChange('status')}
          >
            <option value={PERMISSION_STATUS_FILTER.ALL}>{t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_ALL)}</option>
            <option value={PERMISSION_STATUS_FILTER.ENABLED}>{t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_ENABLED)}</option>
            <option value={PERMISSION_STATUS_FILTER.DISABLED}>{t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_DISABLED)}</option>
          </select>
        </div>

        <div className="grid gap-2 pt-2">
          <Button type="submit">
            <Search className="mr-2 h-4 w-4" />
            {t(I18N_KEYS.PERMISSIONS_PAGE.APPLY_FILTERS)}
          </Button>
          <Button type="button" variant="outline" onClick={onResetFilters}>
            {t(I18N_KEYS.PERMISSIONS_PAGE.RESET_FILTERS)}
          </Button>
        </div>
      </form>
    </MemoryControlCenter>
  );
}
