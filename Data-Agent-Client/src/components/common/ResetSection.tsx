import { RotateCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useToast } from '../../hooks/useToast';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface ResetSectionProps {
  onReset: () => void;
}

export function ResetSection({ onReset }: ResetSectionProps) {
  const { t } = useTranslation();
  const toast = useToast();

  const handleClick = () => {
    if (window.confirm(t(I18N_KEYS.SETTINGS_MODAL.RESET_CONFIRM))) {
      onReset();
      toast.success(t(I18N_KEYS.SETTINGS_MODAL.PREFERENCES_RESET));
    }
  };

  return (
    <div className="pt-4 border-t theme-border">
      <button
        onClick={handleClick}
        className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs rounded theme-bg-panel theme-text-secondary hover:theme-text-red-400 transition-all border theme-border hover:border-red-400/50"
      >
        <RotateCcw className="h-3 w-3" />
        <span>{t(I18N_KEYS.SETTINGS_MODAL.RESET_ALL)}</span>
      </button>
    </div>
  );
}
