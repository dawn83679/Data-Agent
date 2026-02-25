import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import { LANGUAGES } from '../../constants/languages';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { STORAGE_KEYS } from '../../constants/storageKeys';
import { ButtonRadioGroup } from './RadioGroup';

export function LanguageSection() {
  const { t } = useTranslation();
  const currentLang = i18n.language?.startsWith('zh') ? LANGUAGES.ZH : LANGUAGES.EN;

  const handleLanguageChange = (code: string) => {
    i18n.changeLanguage(code);
    localStorage.setItem(STORAGE_KEYS.I18N_LANGUAGE, code);
  };

  return (
    <section className="space-y-3">
      <label className="block theme-text-secondary text-[11px] uppercase font-bold tracking-wider">
        {t(I18N_KEYS.COMMON.LANGUAGE)}
      </label>
      <ButtonRadioGroup
        value={currentLang}
        onChange={handleLanguageChange}
        options={[
          { value: LANGUAGES.ZH, label: t(I18N_KEYS.SETTINGS_MODAL.LANG_ZH) },
          { value: LANGUAGES.EN, label: t(I18N_KEYS.SETTINGS_MODAL.LANG_EN) },
        ]}
      />
    </section>
  );
}
