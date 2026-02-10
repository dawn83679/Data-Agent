import { useState, useEffect } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { cn } from '../../../lib/utils';
import { THOUGHT_LABEL_THINKING, THOUGHT_LABEL_THOUGHT } from '../../../constants/chat';
import { markdownComponents, markdownRemarkPlugins } from './markdownComponents';

export interface ThoughtBlockProps {
  data: string;
  /** When true, start expanded (e.g. while thinking); when it becomes false, collapse (thought done). */
  defaultExpanded?: boolean;
}

/** Renders a THOUGHT block (reasoning content). Shows "Thinking" while streaming (defaultExpanded), "Thought" when done. Expanded area has fixed height and scroll. */
export function ThoughtBlock({ data, defaultExpanded = false }: ThoughtBlockProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  useEffect(() => {
    if (!defaultExpanded) setExpanded(false);
  }, [defaultExpanded]);

  if (!data) return null;

  const label = defaultExpanded ? THOUGHT_LABEL_THINKING : THOUGHT_LABEL_THOUGHT;

  return (
    <div className="mb-1.5 last:mb-0 text-[11px] opacity-60 theme-text-secondary">
      <button
        type="button"
        onClick={() => setExpanded((e) => !e)}
        className={cn(
          'flex items-center gap-1.5 text-left w-full py-0.5',
          'hover:opacity-80 transition-opacity'
        )}
      >
        <span className="opacity-80">{expanded ? <ChevronDown className="w-3 h-3 shrink-0" aria-hidden /> : <ChevronRight className="w-3 h-3 shrink-0" aria-hidden />}</span>
        <span className="font-normal">{label}</span>
      </button>
      {expanded && (
        <div className="pl-4 pr-0 py-1 border-l border-current/20 max-h-[280px] overflow-y-auto">
          <div className="opacity-90">
            <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>{data}</ReactMarkdown>
          </div>
        </div>
      )}
    </div>
  );
}
