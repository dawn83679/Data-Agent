import { useEffect, useRef } from 'react';
import { Database, Eye, FunctionSquare, LayoutGrid, Server, Table, Zap } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { MentionItem, MentionLevel, MentionObjectType } from './mentionTypes';
import { I18N_KEYS } from '../../constants/i18nKeys';

const MENTION_LEVEL_COLORS: Record<MentionLevel, string> = {
  connection: 'text-emerald-400',
  database: 'text-blue-400',
  schema: 'text-amber-400',
  object: 'text-violet-400',
};

const OBJECT_TYPE_COLORS: Record<MentionObjectType, string> = {
  TABLE: 'text-green-400',
  VIEW: 'text-indigo-400',
  FUNCTION: 'text-violet-400',
  PROCEDURE: 'text-fuchsia-400',
  TRIGGER: 'text-orange-400',
  COLUMN: 'text-sky-400',
  INDEX: 'text-slate-400',
  KEY: 'text-amber-500',
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
    case 'object':
      return <Table className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    default:
      return <Database className={`w-3 h-3 shrink-0 ${colorClass}`} />;
  }
}

function MentionObjectIcon({
  objectType,
  selected = false,
}: {
  objectType?: MentionObjectType;
  selected?: boolean;
}) {
  const colorClass = selected ? 'text-white' : (objectType ? OBJECT_TYPE_COLORS[objectType] : 'theme-text-secondary');

  switch (objectType) {
    case 'TABLE':
      return <Table className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    case 'VIEW':
      return <Eye className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    case 'FUNCTION':
      return <FunctionSquare className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    case 'PROCEDURE':
      return <FunctionSquare className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    case 'TRIGGER':
      return <Zap className={`w-3 h-3 shrink-0 ${colorClass}`} />;
    default:
      return <MentionLevelIcon level="object" selected={selected} />;
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
  const listContainerRef = useRef<HTMLDivElement>(null);
  const objectTypeLabel = (objectType?: MentionObjectType) => {
    switch (objectType) {
      case 'TABLE':
        return t(I18N_KEYS.AI.MENTION_TYPE_TABLE);
      case 'VIEW':
        return t(I18N_KEYS.AI.MENTION_TYPE_VIEW);
      case 'FUNCTION':
        return t(I18N_KEYS.AI.MENTION_TYPE_FUNCTION);
      case 'PROCEDURE':
        return t(I18N_KEYS.AI.MENTION_TYPE_PROCEDURE);
      case 'TRIGGER':
        return t(I18N_KEYS.AI.MENTION_TYPE_TRIGGER);
      default:
        return '';
    }
  };

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
      className="absolute bottom-full left-0 z-50 mb-1 flex max-h-40 w-full flex-col overflow-y-auto rounded border border-[color:var(--border-color)] bg-[color:var(--bg-panel)] shadow-xl"
      role="listbox"
      aria-label={levelLabel}
    >
      <div className="flex shrink-0 items-center justify-between border-b border-[color:var(--border-color)] px-3 py-1.5 text-[10px] font-medium theme-text-secondary">
        <span>{levelLabel}</span>
        <span className="opacity-70 font-normal">{t(I18N_KEYS.AI.MENTION_NAV_HINT)}</span>
      </div>
      {loading ? (
        <div className="px-3 py-2 text-xs theme-text-secondary">{t(I18N_KEYS.EXPLORER.LOADING)}</div>
      ) : error ? (
        <div className="px-3 py-2 text-xs text-red-500">{error}</div>
      ) : items.length === 0 ? (
        <div className="px-3 py-2 text-xs theme-text-secondary">
          {level === 'connection'
            ? t(I18N_KEYS.COMMON.NO_CONNECTIONS)
            : level === 'database'
              ? t(I18N_KEYS.AI.MENTION_NO_DATABASES)
              : level === 'schema'
                ? t(I18N_KEYS.AI.MENTION_NO_SCHEMAS)
                : t(I18N_KEYS.AI.MENTION_NO_OBJECTS)}
        </div>
      ) : (
        items.map((item, index) => {
          const isSelected = index === highlightedIndex;
          const detail = item.detail || objectTypeLabel(item.payload?.objectType);
          return (
          <button
            key={item.id}
            type="button"
            role="option"
            data-mention-index={index}
            aria-selected={isSelected}
            className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-xs transition-colors ${
              isSelected
                ? highlightClassName
                : 'theme-text-primary hover:bg-[color:var(--bg-hover)]'
            }`}
            onMouseEnter={() => onHighlight(index)}
            onClick={() => onSelect(item)}
          >
            {level === 'object'
              ? <MentionObjectIcon objectType={item.payload?.objectType} selected={isSelected} />
              : <MentionLevelIcon level={level} selected={isSelected} />}
            <span className="min-w-0 flex-1 truncate">
              <span className={isSelected ? 'text-white' : 'theme-text-primary'}>
                {item.label}
              </span>
            </span>
            {detail && (
              <span className={isSelected ? 'shrink-0 text-white/80' : 'shrink-0 theme-text-secondary'}>
                {detail}
              </span>
            )}
          </button>
        )})
      )}
    </div>
  );
}
