import { useTranslation } from 'react-i18next';
import { cn } from '../../../lib/utils';
import { STATUS_LABEL_KEYS } from '../../../constants/chat';

export interface StatusBlockProps {
  statusKey: string;
  className?: string;
}

/** Pulsing status indicator for system notifications (e.g. memory compression). */
export function StatusBlock({ statusKey, className }: StatusBlockProps) {
  const { t } = useTranslation();
  const labelKey = STATUS_LABEL_KEYS[statusKey];
  const label = labelKey ? t(labelKey) : statusKey;
  return (
    <div className={cn('mb-2 text-xs opacity-70 theme-text-secondary', className)}>
      <div className="w-full py-1.5 flex items-center gap-2 text-left rounded theme-text-primary">
        <span className="font-medium animate-pulse">{label}</span>
      </div>
    </div>
  );
}
