import { ChevronDown, ChevronUp } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Input } from '../ui/Input';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface TableDataFilterBarProps {
  columns: string[];
  whereClause: string;
  orderByColumn: string;
  orderByDirection: 'asc' | 'desc';
  orderControlsDisabled?: boolean;
  onWhereClauseChange: (value: string) => void;
  onRun: () => void;
  onOrderByColumnChange: (column: string) => void;
  onToggleOrderByDirection: () => void;
}

export function TableDataFilterBar({
  columns,
  whereClause,
  orderByColumn,
  orderByDirection,
  orderControlsDisabled = false,
  onWhereClauseChange,
  onRun,
  onOrderByColumnChange,
  onToggleOrderByDirection,
}: TableDataFilterBarProps) {
  const { t } = useTranslation();

  return (
    <div className="grid grid-cols-2 gap-3 px-2 py-1.5 border-b theme-border shrink-0 text-[11px]">
      <div className="flex items-center gap-2 min-w-0">
        <span className="shrink-0 theme-text-secondary font-medium">{t(I18N_KEYS.EXPLORER.WHERE_LABEL)}</span>
        <Input
          value={whereClause}
          onChange={(e) => onWhereClauseChange(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && onRun()}
          placeholder="status = 1 AND name LIKE '%a%'"
          className="h-7 text-[11px] font-mono flex-1 min-w-0"
        />
      </div>
      <div className="flex items-center gap-2 min-w-0">
        <span className="shrink-0 theme-text-secondary font-medium">{t(I18N_KEYS.EXPLORER.ORDER_BY_LABEL)}</span>
        <select
          value={orderByColumn}
          onChange={(e) => onOrderByColumnChange(e.target.value)}
          disabled={orderControlsDisabled}
          className="h-7 px-2 rounded border theme-border theme-bg-main theme-text-primary text-[11px] font-mono flex-1 min-w-0 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <option value="">--</option>
          {columns.map((col) => (
            <option key={col} value={col}>
              {col}
            </option>
          ))}
        </select>
        {orderByColumn ? (
          <button
            type="button"
            onClick={onToggleOrderByDirection}
            disabled={orderControlsDisabled}
            className="h-7 px-2 rounded border theme-border theme-bg-main theme-text-primary text-[11px] hover:bg-accent/50 flex items-center gap-1 shrink-0 disabled:opacity-50 disabled:cursor-not-allowed"
            title={orderByDirection === 'asc' ? t(I18N_KEYS.EXPLORER.ORDER_ASC) : t(I18N_KEYS.EXPLORER.ORDER_DESC)}
          >
            {orderByDirection === 'asc' ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
            {orderByDirection === 'asc' ? t(I18N_KEYS.EXPLORER.ORDER_ASC) : t(I18N_KEYS.EXPLORER.ORDER_DESC)}
          </button>
        ) : null}
      </div>
    </div>
  );
}
