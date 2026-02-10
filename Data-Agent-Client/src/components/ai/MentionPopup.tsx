import { useEffect, useRef } from 'react';
import { Database, Table, LayoutGrid, Server } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { MentionItem, MentionLevel } from './mentionTypes';

const MENTION_LEVEL_COLORS: Record<MentionLevel, string> = {
  connection: 'text-emerald-400',
  database: 'text-blue-400',
  schema: 'text-amber-400',
  table: 'text-violet-400',
};

function MentionLevelIcon({ level }: { level: MentionLevel }) {
  const colorClass = MENTION_LEVEL_COLORS[level] ?? 'opacity-70';
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

const DEFAULT_HIGHLIGHT = 'bg-[#3574f0] text-white';

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
      className="absolute bottom-full mb-1 left-0 w-full theme-bg-popup border theme-border rounded shadow-xl max-h-40 overflow-y-auto z-50 flex flex-col"
      role="listbox"
      aria-label={levelLabel}
    >
      <div className="px-3 py-1.5 text-[10px] theme-text-secondary font-medium border-b theme-border shrink-0 flex items-center justify-between">
        <span>{levelLabel}</span>
        <span className="opacity-70 font-normal">{t('ai.mention_nav_hint')}</span>
      </div>
      {loading ? (
        <div className="px-3 py-2 text-xs theme-text-secondary">{t('explorer.loading')}</div>
      ) : error ? (
        <div className="px-3 py-2 text-xs text-red-500">{error}</div>
      ) : items.length === 0 ? (
        <div className="px-3 py-2 text-xs theme-text-secondary">
          {level === 'connection'
            ? t('common.no_connections')
            : level === 'database'
              ? t('ai.mention_no_databases')
              : level === 'schema'
                ? t('ai.mention_no_schemas')
                : t('ai.mention_no_tables')}
        </div>
      ) : (
        items.map((item, index) => (
          <button
            key={item.id}
            type="button"
            role="option"
            data-mention-index={index}
            aria-selected={index === highlightedIndex}
            className={`flex items-center space-x-2 w-full text-left px-3 py-1.5 text-xs cursor-pointer transition-colors ${
              index === highlightedIndex
                ? highlightClassName
                : 'theme-text-primary hover:bg-white/5'
            }`}
            onMouseEnter={() => onHighlight(index)}
            onClick={() => onSelect(item)}
          >
            <MentionLevelIcon level={level} />
            <span className={index !== highlightedIndex ? MENTION_LEVEL_COLORS[level] : undefined}>
              {item.label}
            </span>
          </button>
        ))
      )}
    </div>
  );
}
