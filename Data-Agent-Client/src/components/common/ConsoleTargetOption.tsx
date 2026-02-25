import { useTranslation } from 'react-i18next';
import { TableDblClickConsoleTargetEnum } from '../../constants/workspacePreferences';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { RadioGroup } from './RadioGroup';

interface ConsoleTargetOptionProps {
  value: string;
  onChange: (val: any) => void;
}

export function ConsoleTargetOption({ value, onChange }: ConsoleTargetOptionProps) {
  const { t } = useTranslation();

  return (
    <div className="ml-6 mt-2 p-2 rounded theme-bg-panel border theme-border space-y-2 animate-in slide-in-from-top-1 duration-200">
      <span className="text-[10px] theme-text-secondary uppercase font-bold">{t(I18N_KEYS.SETTINGS_MODAL.CONSOLE_TARGET)}</span>
      <RadioGroup
        name="console-target"
        value={value}
        onChange={onChange}
        options={[
          { value: TableDblClickConsoleTargetEnum.REUSE, label: t(I18N_KEYS.SETTINGS_MODAL.CONSOLE_REUSE) },
          { value: TableDblClickConsoleTargetEnum.NEW, label: t(I18N_KEYS.SETTINGS_MODAL.CONSOLE_NEW) },
        ]}
        variant="vertical"
      />
    </div>
  );
}
