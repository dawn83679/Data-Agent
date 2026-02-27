import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface SettingsModalFooterProps {
  onClose: () => void;
}

export function SettingsModalFooter({ onClose }: SettingsModalFooterProps) {
  const { t } = useTranslation();

  return (
    <div className="px-4 py-3 border-t theme-border flex justify-end items-center theme-bg-panel rounded-b-lg select-none">
      <button
        onClick={onClose}
        className="px-4 py-1.5 bg-blue-600 text-white hover:bg-blue-500 rounded transition-colors font-medium text-xs shadow-sm"
      >
        {t(I18N_KEYS.SETTINGS_MODAL.DONE)}
      </button>
    </div>
  );
}
