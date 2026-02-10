import { useEffect, useRef } from 'react';
import { cn } from '../../lib/utils';
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
      className="absolute bottom-full mb-1 left-0 w-full theme-bg-popup border theme-border rounded shadow-xl max-h-40 overflow-y-auto z-50 flex flex-col"
      role="listbox"
      aria-label="Commands"
    >
      <div className="px-3 py-1.5 text-[10px] theme-text-secondary font-medium border-b theme-border shrink-0">
        Commands
      </div>
      {filtered.length === 0 ? (
        <div className="px-3 py-2 text-xs theme-text-secondary">No matching command</div>
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
              index === highlightedIndex ? highlightClassName : 'theme-text-primary hover:bg-white/5'
            )}
            onMouseEnter={() => onHighlight(index)}
            onClick={() => onSelect(cmd)}
          >
            {cmd.icon ?? null}
            <span>/{cmd.slug}</span>
            <span className="opacity-70">— {cmd.label}</span>
          </button>
        ))
      )}
    </div>
  );
}
