import { useState } from 'react';
import { CheckCircle, ChevronDown, ChevronRight, XCircle } from 'lucide-react';
import { TodoListBlock } from './TodoListBlock';
import { isTodoTool, parseTodoListResponse } from './todoTypes';

export interface ToolRunBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  /** True when tool execution failed (backend ToolExecution.hasFailed()). */
  responseError?: boolean;
  /** True while waiting for TOOL_RESULT (no icon, tool name blinks). */
  pending?: boolean;
}

/** One tool run: pending = tool name only (blink); completed = icon + Ran/Failed + expandable details. Todo tools render TodoListBlock when response is valid JSON array. Default is expanded. */
export function ToolRunBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
  pending = false,
}: ToolRunBlockProps) {
  const todoItems = !responseError && isTodoTool(toolName) ? parseTodoListResponse(responseData)?.items ?? null : null;
  const isTodoResult = todoItems !== null;
  const [collapsed, setCollapsed] = useState(false);

  if (pending) {
    return (
      <div className="mb-2 text-xs opacity-70 theme-text-secondary">
        <div className="w-full py-1.5 flex items-center gap-2 text-left rounded theme-text-primary">
          <span className="font-medium animate-pulse">{toolName}</span>
        </div>
      </div>
    );
  }

  if (isTodoResult) {
    return (
      <div className="mb-2">
        <TodoListBlock items={todoItems} />
      </div>
    );
  }

  return (
    <div className="mb-2 text-xs opacity-70 theme-text-secondary">
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className="w-full py-1.5 flex items-center gap-2 text-left rounded transition-colors theme-text-primary hover:bg-black/5 dark:hover:bg-white/5"
      >
        {responseError ? (
          <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" aria-label="Failed" />
        ) : (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" aria-hidden />
        )}
        <span className="font-medium">
          {responseError ? 'Failed ' : 'Ran '}
          {toolName}
        </span>
        <span className="ml-auto shrink-0 opacity-60">
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <div className="mt-1 space-y-2">
          <div>
            <div className="text-[10px] font-semibold uppercase tracking-wide theme-text-secondary mb-1 flex items-center gap-1">
              PARAMETERS
              <span className="opacity-50" aria-hidden>☰</span>
            </div>
            <pre className="py-1.5 px-2 rounded bg-black/10 dark:bg-black/20 font-mono text-[11px] overflow-x-auto whitespace-pre-wrap break-words">
              {parametersData || '—'}
            </pre>
          </div>
          <div>
            <div className="text-[10px] font-semibold uppercase tracking-wide theme-text-secondary mb-1">
              RESPONSE
            </div>
            <pre className="py-1.5 px-2 rounded bg-black/10 dark:bg-black/20 font-mono text-[11px] overflow-x-auto max-h-[220px] overflow-y-auto whitespace-pre-wrap break-words">
              {responseData || '—'}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
