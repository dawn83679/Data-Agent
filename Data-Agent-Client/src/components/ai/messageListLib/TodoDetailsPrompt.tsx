import { useState } from 'react';
import { CheckCircle, ChevronDown, ChevronRight } from 'lucide-react';
import type { TodoItem } from '../blocks';

export interface TodoDetailsPromptProps {
  items: TodoItem[];
}

export function TodoDetailsPrompt({ items }: TodoDetailsPromptProps) {
  const [expanded, setExpanded] = useState(false);
  const count = items.length;

  return (
    <div className="flex flex-col w-fit items-start">
      <button
        type="button"
        onClick={() => setExpanded((e) => !e)}
        className="flex items-center gap-2 py-1 theme-text-secondary text-[11px] rounded transition-colors hover:bg-black/5 dark:hover:bg-white/5 -mx-1 px-1 w-fit text-left"
        aria-expanded={expanded}
        aria-label={`Completed ${count} of ${count} to-dos. View details.`}
      >
        <span>Completed {count} of {count} to-dos</span>
        <CheckCircle className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
        {expanded ? (
          <ChevronDown className="w-3.5 h-3.5 shrink-0" aria-hidden />
        ) : (
          <ChevronRight className="w-3.5 h-3.5 shrink-0" aria-hidden />
        )}
      </button>
      {expanded && (
        <ul className="list-none p-0 m-0 mt-1 ml-0 space-y-2 text-sm theme-text-primary border-l-2 theme-border pl-3">
          {items.map((item, index) => (
            <li key={index}>
              <div className="font-medium">{item.title || '—'}</div>
              {item.description != null && item.description !== '' && (
                <div className="theme-text-secondary text-xs mt-0.5">{item.description}</div>
              )}
              <div className="flex gap-3 mt-0.5 theme-text-secondary text-xs">
                <span>Status: {item.status ?? '—'}</span>
                {item.priority != null && item.priority !== '' && (
                  <span>Priority: {item.priority}</span>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
