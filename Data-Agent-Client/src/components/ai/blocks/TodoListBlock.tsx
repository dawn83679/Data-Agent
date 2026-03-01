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
  const [expanded, setExpanded] = useState(false); // 默认折叠
  const completedCount = items.filter((i) => isTodoCompleted(i.status)).length;

  // Find current in-progress or paused item for collapsed view
  const currentItem = items.find((i) => isTodoInProgress(i.status) || isTodoPaused(i.status)) || items[0];

  if (items.length === 0) {
    return null;
  }

  return (
      <div className="rounded border theme-border theme-bg-panel">
        <div className="px-2 py-1.5">
          {/* Compact header */}
          <button
              type="button"
              onClick={() => setExpanded((e) => !e)}
              className="w-full flex items-center gap-1.5 text-left hover:opacity-80 transition-opacity"
          >
            <ListTodo className="w-3 h-3 theme-text-secondary shrink-0" />
            <span className="text-[10px] font-medium theme-text-secondary">
            Todos
          </span>
            <span className="text-[9px] theme-text-secondary opacity-60">
            {completedCount}/{items.length}
          </span>
            <span className="ml-auto">
            {expanded ? (
                <ChevronDown className="w-3 h-3 theme-text-secondary" />
            ) : (
                <ChevronRight className="w-3 h-3 theme-text-secondary" />
            )}
          </span>
          </button>

          {/* Collapsed: show only current item */}
          {!expanded && currentItem && (
              <div className="flex items-center gap-1.5 text-[10px] theme-text-primary mt-1">
                {isTodoCompleted(currentItem.status) ? (
                    <CheckCircle className="w-3 h-3 text-green-500 shrink-0" />
                ) : isTodoInProgress(currentItem.status) ? (
                    <span className="inline-flex w-3 h-3 items-center justify-center theme-text-secondary shrink-0 text-[12px] leading-none opacity-70">
                ◐
              </span>
                ) : isTodoPaused(currentItem.status) ? (
                    <PauseCircle className="w-3 h-3 text-amber-500 shrink-0" />
                ) : (
                    <Circle className="w-3 h-3 theme-text-secondary shrink-0 opacity-70" />
                )}
                <span className="truncate text-[10px]">
              {currentItem.title || '—'}
            </span>
              </div>
          )}

          {/* Expanded: show all items */}
          {expanded && (
              <ul className="list-none p-0 m-0 mt-1 space-y-0.5">
                {items.map((item, index) => (
                    <li
                        key={index}
                        className="flex items-center gap-1.5 py-0.5 theme-text-primary text-[10px]"
                    >
                      {isTodoCompleted(item.status) ? (
                          <CheckCircle className="w-3 h-3 text-green-500 shrink-0" />
                      ) : isTodoInProgress(item.status) ? (
                          <span className="inline-flex w-3 h-3 items-center justify-center theme-text-secondary shrink-0 text-[12px] leading-none opacity-70">
                    ◐
                  </span>
                      ) : isTodoPaused(item.status) ? (
                          <PauseCircle className="w-3 h-3 text-amber-500 shrink-0" />
                      ) : (
                          <Circle className="w-3 h-3 theme-text-secondary shrink-0 opacity-70" />
                      )}
                      <span className={`truncate ${isTodoCompleted(item.status) ? 'line-through opacity-70' : ''}`}>
                  {item.title || '—'}
                </span>
                    </li>
                ))}
              </ul>
          )}
        </div>
      </div>
  );
}
