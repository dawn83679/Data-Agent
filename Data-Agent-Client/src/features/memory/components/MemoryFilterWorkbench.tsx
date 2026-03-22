import type { ChangeEvent, FormEvent } from 'react';
import { Brain, Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { cn } from '../../../lib/utils';
import { MEMORY_ENABLE, type MemoryScope, type MemoryType } from '../../../types/memory';
import { getMemoryOptionLabel, MEMORY_FORM_SELECT_CLASS_NAME } from '../memoryPageUtils';
import { MemoryControlCenter } from './MemoryControlCenter';
import type { FilterFormState } from '../memoryPageModels';

interface MemoryFilterWorkbenchProps {
  filterForm: FilterFormState;
  semanticSearchEnabled: boolean;
  metadataLoading: boolean;
  filterMemoryTypeOptions: string[];
  filterScopeOptions: MemoryScope[];
  onFilterInputChange: (field: keyof FilterFormState) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  onSemanticToggle: () => void;
  onApplyFilters: (event: FormEvent) => void;
  onResetFilters: () => void;
}

export function MemoryFilterWorkbench({
  filterForm,
  semanticSearchEnabled,
  metadataLoading,
  filterMemoryTypeOptions,
  filterScopeOptions,
  onFilterInputChange,
  onSemanticToggle,
  onApplyFilters,
  onResetFilters,
}: MemoryFilterWorkbenchProps) {
  const { t } = useTranslation();

  return (
    <MemoryControlCenter
      title={t(I18N_KEYS.MEMORY_PAGE.SECTION_FILTERS_TITLE)}
      description={t(I18N_KEYS.MEMORY_PAGE.SECTION_FILTERS_DESC)}
    >
      <form onSubmit={onApplyFilters} className="space-y-4">
        <div className="space-y-1">
          <div className="flex items-center justify-between gap-3">
            <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
              {t(I18N_KEYS.MEMORY_PAGE.FILTER_KEYWORD)}
            </label>
            <button
              type="button"
              onClick={onSemanticToggle}
              className="inline-flex items-center gap-2 rounded-full border theme-border bg-[color:var(--bg-main)]/50 px-2.5 py-1 text-[11px] font-medium theme-text-secondary transition-colors hover:theme-text-primary"
            >
              <Brain className={cn('h-3.5 w-3.5', semanticSearchEnabled && 'text-sky-300')} />
              <span>{t(I18N_KEYS.MEMORY_PAGE.SEMANTIC_SEARCH_TOGGLE)}</span>
              <span
                className={cn(
                  'relative inline-flex h-5 w-9 shrink-0 rounded-full border transition-colors',
                  semanticSearchEnabled ? 'border-sky-400/50 bg-sky-500/30' : 'theme-border bg-[color:var(--bg-main)]/80',
                )}
              >
                <span
                  className={cn(
                    'absolute top-0.5 h-4 w-4 rounded-full bg-white shadow-sm transition-all',
                    semanticSearchEnabled ? 'left-[18px]' : 'left-0.5',
                  )}
                />
              </span>
            </button>
          </div>
          <Input
            value={filterForm.keyword}
            onChange={onFilterInputChange('keyword')}
            placeholder={t(
              semanticSearchEnabled
                ? I18N_KEYS.MEMORY_PAGE.SEARCH_PLACEHOLDER
                : I18N_KEYS.MEMORY_PAGE.FILTER_KEYWORD,
            )}
          />
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
            {t(I18N_KEYS.MEMORY_PAGE.FILTER_MEMORY_TYPE)}
          </label>
          <select
            className={MEMORY_FORM_SELECT_CLASS_NAME}
            value={filterForm.memoryType}
            onChange={onFilterInputChange('memoryType')}
            disabled={metadataLoading}
          >
            <option value="">{t(I18N_KEYS.MEMORY_PAGE.PRESET_ALL)}</option>
            {filterMemoryTypeOptions.map((option) => (
              <option key={option} value={option}>
                {getMemoryOptionLabel(t, option as MemoryType)}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
            {t(I18N_KEYS.MEMORY_PAGE.FILTER_SCOPE)}
          </label>
          <select
            className={MEMORY_FORM_SELECT_CLASS_NAME}
            value={filterForm.scope}
            onChange={onFilterInputChange('scope')}
            disabled={metadataLoading}
          >
            <option value="">{t(I18N_KEYS.MEMORY_PAGE.PRESET_ALL)}</option>
            {filterScopeOptions.map((option) => (
              <option key={option} value={option}>
                {getMemoryOptionLabel(t, option)}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide theme-text-secondary">
            {t(I18N_KEYS.MEMORY_PAGE.FILTER_STATUS)}
          </label>
          <select
            className={MEMORY_FORM_SELECT_CLASS_NAME}
            value={filterForm.enable}
            onChange={onFilterInputChange('enable')}
          >
            <option value={String(MEMORY_ENABLE.ENABLE)}>{t(I18N_KEYS.MEMORY_PAGE.STATUS_ENABLED)}</option>
            <option value={String(MEMORY_ENABLE.DISABLE)}>{t(I18N_KEYS.MEMORY_PAGE.STATUS_DISABLED)}</option>
          </select>
        </div>

        <div className="grid gap-2 pt-2">
          <Button type="submit">
            <Search className="mr-2 h-4 w-4" />
            {t(I18N_KEYS.MEMORY_PAGE.APPLY_FILTERS)}
          </Button>
          <Button type="button" variant="outline" onClick={onResetFilters}>
            {t(I18N_KEYS.MEMORY_PAGE.RESET_FILTERS)}
          </Button>
        </div>
      </form>
    </MemoryControlCenter>
  );
}
