import { useState } from 'react';
import { ChevronDown, ChevronRight, Trash2 } from 'lucide-react';

export interface MessageQueuePanelProps {
  queue: string[];
  onRemove: (index: number) => void;
  defaultExpanded?: boolean;
}

/** Presentational panel for the list of queued follow-up messages. */
export function MessageQueuePanel({
  queue,
  onRemove,
  defaultExpanded = true,
}: MessageQueuePanelProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  if (queue.length === 0) return null;

  return (
    <div className="shrink-0 border-t theme-border theme-bg-panel">
      <button
        type="button"
        onClick={() => setExpanded((e) => !e)}
        className="w-full flex items-center gap-2 px-3 py-2 text-left text-xs theme-text-secondary hover:theme-text-primary transition-colors"
      >
        {expanded ? (
          <ChevronDown className="w-3.5 h-3.5 shrink-0" />
        ) : (
          <ChevronRight className="w-3.5 h-3.5 shrink-0" />
        )}
        <span className="font-medium">{queue.length} Queued</span>
      </button>
      {expanded && (
        <ul className="px-3 pb-2 space-y-1 max-h-32 overflow-y-auto">
          {queue.map((text, i) => (
            <li
              key={`${i}-${text.slice(0, 20)}`}
              className="flex items-center gap-2 py-1.5 px-2 rounded theme-text-primary text-xs bg-black/5 dark:bg-white/5"
            >
              <span className="flex-1 min-w-0 truncate" title={text}>
                {text}
              </span>
              <button
                type="button"
                onClick={() => onRemove(i)}
                className="shrink-0 p-1 rounded theme-text-secondary hover:text-red-500 transition-colors"
                aria-label="Remove from queue"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
