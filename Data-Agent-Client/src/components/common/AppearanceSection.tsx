import { useTranslation } from 'react-i18next';
import { useTheme } from '../../hooks/useTheme';
import { THEMES } from '../../constants/themes';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { ButtonRadioGroup } from './RadioGroup';

export function AppearanceSection() {
  const { t } = useTranslation();
  const { theme, setTheme } = useTheme();

  return (
    <section className="space-y-3 pt-4 border-t theme-border">
      <label className="block theme-text-secondary text-[11px] uppercase font-bold tracking-wider">
        {t(I18N_KEYS.SETTINGS_MODAL.APPEARANCE)}
      </label>
      <ButtonRadioGroup
        value={theme}
        onChange={(val) => setTheme(val as any)}
        options={[
          {
            value: THEMES.DARK,
            label: t(I18N_KEYS.SETTINGS_MODAL.DARK),
            icon: <div className="w-3 h-3 rounded-full bg-slate-800 border border-slate-700" />,
          },
          {
            value: THEMES.LIGHT,
            label: t(I18N_KEYS.SETTINGS_MODAL.LIGHT),
            icon: <div className="w-3 h-3 rounded-full bg-slate-100 border border-slate-300" />,
          },
        ]}
      />
    </section>
  );
}
