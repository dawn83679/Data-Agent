import { useState } from 'react';
import {
  CheckCircle,
  ChevronDown,
  ChevronRight,
  Circle,
  ListTodo,
  PauseCircle,
} from 'lucide-react';
import type { TodoItem } from './todoTypes';
import { isTodoCompleted, isTodoInProgress, isTodoPaused } from './todoTypes';

export interface TodoListBlockProps {
  items: TodoItem[];
}

export function TodoListBlock({ items }: TodoListBlockProps) {
  const [expanded, setExpanded] = useState(true);
  const completedCount = items.filter((i) => isTodoCompleted(i.status)).length;

  if (items.length === 0) {
    return null;
  }

  return (
    <div
      className="mt-1 rounded-lg border theme-border overflow-hidden theme-bg-panel"
      aria-label="To-dos"
    >
      <div className="px-3 pt-2.5 pb-2">
        <button
          type="button"
          onClick={() => setExpanded((e) => !e)}
          className="group w-full flex items-center gap-2 text-left rounded transition-colors hover:bg-black/5 dark:hover:bg-white/5 -mx-1 px-1 py-0.5"
          aria-expanded={expanded}
          aria-label={expanded ? 'Collapse todo list' : 'Expand todo list'}
        >
          <ListTodo className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
          <span className="text-[10px] font-semibold tracking-wide theme-text-secondary">
            To-dos
          </span>
          <span className="ml-auto shrink-0 opacity-0 group-hover:opacity-60 transition-opacity">
            {expanded ? (
              <ChevronDown className="w-3.5 h-3.5" aria-hidden />
            ) : (
              <ChevronRight className="w-3.5 h-3.5" aria-hidden />
            )}
          </span>
        </button>

        {expanded && (
          <>
            {completedCount > 0 && (
              <div
                className="flex items-center gap-2 py-1 theme-text-secondary text-[11px] mt-1"
                role="status"
                aria-label={`Completed ${completedCount} of ${items.length} to-dos`}
              >
                <span>Completed {completedCount} of {items.length} to-dos</span>
                <CheckCircle className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
              </div>
            )}
            <ul className="list-none p-0 m-0 space-y-0">
              {items.map((item, index) => (
                <li
                  key={index}
                  className="flex items-center gap-2 py-1.5 min-h-[1.5rem] theme-text-primary text-[11px]"
                  role="listitem"
                >
                  {/* Icon by backend-returned item.status (TodoTool: NOT_STARTED | IN_PROGRESS | PAUSED | COMPLETED) */}
                  {isTodoCompleted(item.status) ? (
                    <CheckCircle
                      className="w-3.5 h-3.5 text-green-500 shrink-0"
                      aria-label="Completed"
                    />
                  ) : isTodoInProgress(item.status) ? (
                    <span
                      className="inline-flex w-3.5 h-3.5 items-center justify-center text-blue-500 shrink-0 text-[14px] leading-none"
                      aria-label="In progress"
                    >
                      ◐
                    </span>
                  ) : isTodoPaused(item.status) ? (
                    <PauseCircle
                      className="w-3.5 h-3.5 text-amber-500 shrink-0"
                      aria-label="Paused"
                    />
                  ) : (
                    <Circle
                      className="w-3.5 h-3.5 theme-text-secondary shrink-0 opacity-70"
                      aria-label="Not started"
                    />
                  )}
                  <span
                    className={`truncate ${isTodoCompleted(item.status) ? 'line-through opacity-80' : ''}`}
                  >
                    {item.title || '—'}
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </div>
  );
}
