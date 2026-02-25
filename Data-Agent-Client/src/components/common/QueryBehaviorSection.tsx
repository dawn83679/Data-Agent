import { useTranslation } from 'react-i18next';
import { ResultBehaviorEnum, TableDblClickModeEnum } from '../../constants/workspacePreferences';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { RadioGroup } from './RadioGroup';
import { ConsoleTargetOption } from './ConsoleTargetOption';

interface QueryBehaviorSectionProps {
  resultBehavior: string;
  tableDblClickMode: string;
  tableDblClickConsoleTarget: string;
  onResultBehaviorChange: (val: any) => void;
  onTableDblClickModeChange: (val: any) => void;
  onConsoleTargetChange: (val: any) => void;
}

export function QueryBehaviorSection({
  resultBehavior,
  tableDblClickMode,
  tableDblClickConsoleTarget,
  onResultBehaviorChange,
  onTableDblClickModeChange,
  onConsoleTargetChange,
}: QueryBehaviorSectionProps) {
  const { t } = useTranslation();

  return (
    <section className="space-y-3 pt-4 border-t theme-border">
      <label className="block theme-text-secondary text-[11px] uppercase font-bold tracking-wider">
        {t(I18N_KEYS.SETTINGS_MODAL.QUERY_RESULTS)}
      </label>

      <div className="space-y-4">
        {/* Result Behavior */}
        <div className="space-y-2">
          <span className="text-xs theme-text-primary font-medium">
            {t(I18N_KEYS.SETTINGS_MODAL.RESULT_TABS_BEHAVIOR)}
          </span>
          <RadioGroup
            name="result-behavior"
            value={resultBehavior}
            onChange={onResultBehaviorChange}
            options={[
              { value: ResultBehaviorEnum.MULTI, label: t(I18N_KEYS.SETTINGS_MODAL.RESULT_MULTI) },
              { value: ResultBehaviorEnum.OVERWRITE, label: t(I18N_KEYS.SETTINGS_MODAL.RESULT_OVERWRITE) },
            ]}
          />
        </div>

        {/* Table Double Click */}
        <div className="space-y-2">
          <span className="text-xs theme-text-primary font-medium">
            {t(I18N_KEYS.SETTINGS_MODAL.TABLE_DBLCLICK)}
          </span>
          <RadioGroup
            name="table-dblclick"
            value={tableDblClickMode}
            onChange={onTableDblClickModeChange}
            options={[
              { value: TableDblClickModeEnum.TABLE, label: t(I18N_KEYS.SETTINGS_MODAL.TABLE_DBLCLICK_TABLE) },
              { value: TableDblClickModeEnum.CONSOLE, label: t(I18N_KEYS.SETTINGS_MODAL.TABLE_DBLCLICK_CONSOLE) },
            ]}
          />

          {/* Console Target (conditional) */}
          {tableDblClickMode === TableDblClickModeEnum.CONSOLE && (
            <ConsoleTargetOption value={tableDblClickConsoleTarget} onChange={onConsoleTargetChange} />
          )}
        </div>
      </div>
    </section>
  );
}
