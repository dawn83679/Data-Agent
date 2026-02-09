import { useState } from 'react';
import { CheckCircle, ChevronDown, ChevronRight } from 'lucide-react';
import { cn } from '../../../lib/utils';

export interface ToolRunBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
}

/** One tool run: toolName + PARAMETERS (top) + RESPONSE (bottom). Collapsed by default. */
export function ToolRunBlock({ toolName, parametersData, responseData }: ToolRunBlockProps) {
  const [collapsed, setCollapsed] = useState(true);

  return (
    <div className="mb-2 text-xs opacity-70 theme-text-secondary">
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className={cn(
          'w-full py-1.5 flex items-center gap-2 text-left rounded',
          'theme-text-primary hover:bg-black/5 dark:hover:bg-white/5 transition-colors'
        )}
      >
        <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" />
        <span className="font-medium">Ran {toolName}</span>
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
