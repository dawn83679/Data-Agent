import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../lib/utils';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { SLASH_COMMANDS, type SlashCommandItem } from './slashCommands';

export type { SlashCommandItem };

export interface SlashCommandPopupProps {
  open: boolean;
  query: string;
  commands?: SlashCommandItem[];
  highlightedIndex: number;
  onSelect: (command: SlashCommandItem) => void;
  onHighlight: (index: number) => void;
  highlightClassName?: string;
}

export function SlashCommandPopup({
  open,
  query,
  commands = SLASH_COMMANDS,
  highlightedIndex,
  onSelect,
  onHighlight,
  highlightClassName = 'bg-violet-500 text-white',
}: SlashCommandPopupProps) {
  const { t } = useTranslation();
  const listRef = useRef<HTMLDivElement>(null);
  const q = query.toLowerCase().trim();
  const filtered = q
    ? commands.filter((c) => c.slug.toLowerCase().startsWith(q))
    : commands;

  useEffect(() => {
    if (!open || filtered.length === 0) return;
    const el = listRef.current?.querySelector(`[data-cmd-index="${highlightedIndex}"]`);
    if (el instanceof HTMLElement) el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }, [open, filtered.length, highlightedIndex]);

  if (!open) return null;

  return (
    <div
      ref={listRef}
      className="absolute bottom-full left-0 z-50 mb-1 flex max-h-40 w-full flex-col overflow-y-auto rounded border border-[color:var(--border-color)] bg-[color:var(--bg-panel)] shadow-xl"
      role="listbox"
      aria-label={t(I18N_KEYS.AI.SLASH_COMMAND.POPUP_TITLE)}
    >
      <div className="shrink-0 border-b border-[color:var(--border-color)] px-3 py-1.5 text-[10px] font-medium theme-text-secondary">
        {t(I18N_KEYS.AI.SLASH_COMMAND.POPUP_TITLE)}
      </div>
      {filtered.length === 0 ? (
        <div className="px-3 py-2 text-xs theme-text-secondary">{t(I18N_KEYS.AI.SLASH_COMMAND.EMPTY)}</div>
      ) : (
        filtered.map((cmd, index) => (
          <button
            key={cmd.id}
            type="button"
            role="option"
            data-cmd-index={index}
            aria-selected={index === highlightedIndex}
            className={cn(
              'flex items-center gap-2 w-full text-left px-3 py-1.5 text-xs cursor-pointer transition-colors',
              index === highlightedIndex ? highlightClassName : 'theme-text-primary hover:bg-[color:var(--bg-hover)]'
            )}
            onMouseEnter={() => onHighlight(index)}
            onClick={() => onSelect(cmd)}
          >
            {cmd.icon ?? null}
            <span>/{cmd.slug}</span>
            <span className="opacity-70">— {t(cmd.labelKey)}</span>
          </button>
        ))
      )}
    </div>
  );
}
