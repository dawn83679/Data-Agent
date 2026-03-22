import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from '../ui/Input';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { ColumnMetadata } from '../../services/column.service';

interface TableDataInsertBarProps {
  isVisible: boolean;
  headers: string[];
  columnMetadata: ColumnMetadata[];
  loadingColumns: boolean;
  insertSubmitting: boolean;
  insertError: string | null;
  newRowValues: Record<string, string>;
  onNewRowValueChange: (column: string, value: string) => void;
  onConfirmInsert: () => void;
  onCancelInsert: () => void;
}

export function TableDataInsertBar({
  isVisible,
  headers,
  columnMetadata,
  loadingColumns,
  insertSubmitting,
  insertError,
  newRowValues,
  onNewRowValueChange,
  onConfirmInsert,
  onCancelInsert,
}: TableDataInsertBarProps) {
  const { t } = useTranslation();

  const editableHeaders = useMemo(
    () =>
      headers.filter((header) => {
        const meta = columnMetadata.find((item) => item.name === header);
        return meta && !meta.isAutoIncrement;
      }),
    [columnMetadata, headers],
  );

  if (!isVisible) {
    return null;
  }

  return (
    <div className="shrink-0 border-b theme-border bg-[color:var(--bg-panel)]/55 px-3 py-3">
      <div className="mb-3 flex items-center justify-between gap-3">
        <span className="text-[11px] font-medium uppercase tracking-[0.08em] theme-text-secondary">
          {t(I18N_KEYS.EXPLORER.INSERT_ROW)}
        </span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            className="h-7 rounded border border-primary/40 bg-primary/15 px-3 text-[11px] font-medium text-primary transition-colors hover:bg-primary/20 disabled:cursor-not-allowed disabled:opacity-50"
            onClick={onConfirmInsert}
            disabled={insertSubmitting || loadingColumns}
          >
            {insertSubmitting ? '...' : t(I18N_KEYS.EXPLORER.INSERT_ROW)}
          </button>
          <button
            type="button"
            className="h-7 rounded border theme-border px-3 text-[11px] theme-text-secondary transition-colors hover:bg-accent/30 hover:theme-text-primary disabled:cursor-not-allowed disabled:opacity-50"
            onClick={onCancelInsert}
            disabled={insertSubmitting}
          >
            {t(I18N_KEYS.CONNECTIONS.CANCEL)}
          </button>
        </div>
      </div>
      <div className="overflow-x-auto">
        <div className="flex min-w-max gap-2">
          {loadingColumns ? (
            <div className="rounded border theme-border bg-[color:var(--bg-main)]/70 px-3 py-2 text-[11px] theme-text-secondary">
              {t(I18N_KEYS.COMMON.LOADING)}...
            </div>
          ) : editableHeaders.length > 0 ? (
            editableHeaders.map((header, index) => {
              const meta = columnMetadata.find((item) => item.name === header);
              return (
                <div key={header} className="w-52 shrink-0">
                  <div className="mb-1 truncate text-[10px] theme-text-secondary">{header}</div>
                  <Input
                    value={newRowValues[header] ?? ''}
                    onChange={(e) => onNewRowValueChange(header, e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') onConfirmInsert();
                      if (e.key === 'Escape') onCancelInsert();
                    }}
                    placeholder={meta?.nullable ? 'NULL' : ''}
                    className="h-8 border-border/70 bg-[color:var(--bg-main)]/80 text-[11px] font-mono"
                    disabled={insertSubmitting}
                    autoFocus={index === 0}
                  />
                </div>
              );
            })
          ) : (
            <div className="rounded border theme-border bg-[color:var(--bg-main)]/70 px-3 py-2 text-[11px] theme-text-secondary">
              No editable columns
            </div>
          )}
        </div>
      </div>
      {insertError ? (
        <div className="mt-3 rounded border border-destructive/30 bg-destructive/10 px-3 py-2 text-[11px] text-destructive">
          {insertError}
        </div>
      ) : null}
    </div>
  );
}
