import { cn } from '../../../lib/utils';

export function getToolCardClassName(expanded = false): string {
  return cn(
    'mb-2 overflow-hidden rounded-xl border theme-border',
    'bg-[color:var(--bg-panel)] shadow-[0_8px_22px_rgba(15,23,42,0.08)]',
    expanded && 'shadow-[0_14px_30px_rgba(15,23,42,0.12)]'
  );
}

export const TOOL_CARD_HEADER_CLASSNAME =
  'flex w-full items-center gap-2.5 px-3 py-2 text-left transition-colors hover:bg-black/5 dark:hover:bg-white/[0.04]';

export const TOOL_CARD_CONTENT_CLASSNAME = 'border-t theme-border px-3 py-3';

export const TOOL_CARD_META_CLASSNAME =
  'rounded-full border theme-border px-2 py-0.5 text-[10px] font-medium theme-text-secondary';

export const TOOL_SECTION_TITLE_CLASSNAME =
  'mb-1.5 flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.16em] theme-text-secondary';

export function getToolSectionShellClassName(theme: 'light' | 'dark'): string {
  return theme === 'dark'
    ? 'rounded-lg border border-white/8 bg-black/18'
    : 'rounded-lg border border-slate-200 bg-slate-50/90';
}
