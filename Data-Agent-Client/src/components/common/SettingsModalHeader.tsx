import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface SettingsModalHeaderProps {
  onClose: () => void;
}

export function SettingsModalHeader({ onClose }: SettingsModalHeaderProps) {
  const { t } = useTranslation();

  return (
    <div className="px-4 py-3 border-b theme-border font-semibold theme-text-primary flex justify-between items-center select-none">
      <span className="text-sm">{t(I18N_KEYS.COMMON.SETTINGS)}</span>
      <button onClick={onClose} className="theme-text-secondary hover:theme-text-primary transition-colors">
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
