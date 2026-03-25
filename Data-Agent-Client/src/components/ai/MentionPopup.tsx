import { useEffect, useRef } from 'react';
import { Database, Table, LayoutGrid, Server } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { MentionItem, MentionLevel } from './mentionTypes';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useTheme } from '../../hooks/useTheme';

const MENTION_LEVEL_COLORS: Record<MentionLevel, string> = {
  connection: 'text-emerald-400',
  database: 'text-blue-400',
  schema: 'text-amber-400',
  table: 'text-violet-400',
};

function MentionLevelIcon({ level, selected = false }: { level: MentionLevel; selected?: boolean }) {
  const colorClass = selected ? 'text-white' : (MENTION_LEVEL_COLORS[level] ?? 'opacity-70');
  switch (level) {
    case 'connection':
      return <Server className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    case 'database':
      return <Database className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    case 'schema':
      return <LayoutGrid className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    case 'table':
      return <Table className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    default:
      return <Database className={`w-3 h-3 shrink-0 ${colorClass}`} />;
  }
}

export interface MentionPopupProps {
  open: boolean;
  level: MentionLevel;
  levelLabel: string;
  items: MentionItem[];
  loading: boolean;
  error: string | null;
  highlightedIndex: number;
  onSelect: (item: MentionItem) => void;
  onHighlight: (index: number) => void;
  /** Highlight item background and text class to match Agent mode (e.g. 'bg-violet-500 text-white'). */
  highlightClassName?: string;
}

const DEFAULT_HIGHLIGHT = 'bg-[var(--accent-blue)] text-white shadow-sm';

export function MentionPopup({
  open,
  level,
  levelLabel,
  items,
  loading,
  error,
  highlightedIndex,
  onSelect,
  onHighlight,
  highlightClassName = DEFAULT_HIGHLIGHT,
}: MentionPopupProps) {
  const { t } = useTranslation();
  const { theme } = useTheme();
  const listContainerRef = useRef<HTMLDivElement>(null);

  // Scroll highlighted item into view when ArrowUp/ArrowDown changes selection
  useEffect(() => {
    if (!open || loading || error || items.length === 0) return;
    const container = listContainerRef.current;
    if (!container) return;
    const option = container.querySelector(`[data-mention-index="${highlightedIndex}"]`);
    if (option instanceof HTMLElement) {
      option.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  }, [open, loading, error, items.length, highlightedIndex]);

  if (!open) return null;

  return (
    <div
      ref={listContainerRef}
      className={`absolute bottom-full mb-1 left-0 w-full border rounded shadow-xl max-h-40 overflow-y-auto z-50 flex flex-col ${
        theme === 'light'
          ? 'bg-slate-900/92 border-slate-700/80 backdrop-blur-md shadow-[0_18px_48px_rgba(15,23,42,0.28)]'
          : 'theme-bg-popup theme-border'
      }`}
      role="listbox"
      aria-label={levelLabel}
    >
      <div className={`px-3 py-1.5 text-[10px] font-medium border-b shrink-0 flex items-center justify-between ${
        theme === 'light'
          ? 'text-slate-300 border-slate-700/80'
          : 'theme-text-secondary theme-border'
      }`}>
        <span>{levelLabel}</span>
        <span className="opacity-70 font-normal">{t(I18N_KEYS.AI.MENTION_NAV_HINT)}</span>
      </div>
      {loading ? (
        <div className={`px-3 py-2 text-xs ${theme === 'light' ? 'text-slate-300' : 'theme-text-secondary'}`}>{t(I18N_KEYS.EXPLORER.LOADING)}</div>
      ) : error ? (
        <div className="px-3 py-2 text-xs text-red-500">{error}</div>
      ) : items.length === 0 ? (
        <div className={`px-3 py-2 text-xs ${theme === 'light' ? 'text-slate-300' : 'theme-text-secondary'}`}>
          {level === 'connection'
            ? t(I18N_KEYS.COMMON.NO_CONNECTIONS)
            : level === 'database'
              ? t(I18N_KEYS.AI.MENTION_NO_DATABASES)
              : level === 'schema'
                ? t(I18N_KEYS.AI.MENTION_NO_SCHEMAS)
                : t(I18N_KEYS.AI.MENTION_NO_TABLES)}
        </div>
      ) : (
        items.map((item, index) => {
          const isSelected = index === highlightedIndex;
          return (
          <button
            key={item.id}
            type="button"
            role="option"
            data-mention-index={index}
            aria-selected={isSelected}
            className={`flex items-center space-x-2 w-full text-left px-3 py-1.5 text-xs cursor-pointer transition-colors ${
              isSelected
                ? highlightClassName
                : theme === 'light'
                  ? 'text-white/90 hover:bg-white/10'
                  : 'theme-text-primary hover:bg-[color:var(--bg-popup)]/70'
            }`}
            onMouseEnter={() => onHighlight(index)}
            onClick={() => onSelect(item)}
          >
            <MentionLevelIcon level={level} selected={isSelected} />
            <span className={isSelected ? 'text-white' : theme === 'light' ? 'text-white/90' : 'theme-text-primary'}>
              {item.label}
            </span>
          </button>
        )})
      )}
    </div>
  );
}
