import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import en from './locales/en.json';
import zh from './locales/zh.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      zh: { translation: zh }
    },
    fallbackLng: 'zh',
    supportedLngs: ['en', 'zh'],
    // Don't auto-cache detected language, so changing OS/browser language takes effect on next visit.
    // Only the header language switcher writes to localStorage; when set, that choice wins.
    detection: {
      order: ['querystring', 'localStorage', 'navigator', 'htmlTag'],
      caches: [],
    },
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;
