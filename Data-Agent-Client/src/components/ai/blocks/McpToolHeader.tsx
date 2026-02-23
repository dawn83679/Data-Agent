import { CheckCircle, XCircle, ChevronDown, ChevronRight } from 'lucide-react';
import { cn } from '../../../lib/utils';
import type { RenderConfig } from './McpToolConfig';

export interface McpToolHeaderProps {
  config: RenderConfig;
  collapsed: boolean;
  onToggle: () => void;
  toolName: string;
  serverName?: string;
  responseError?: boolean;
}

/**
 * Format chart tool name to human-readable type
 * @example "generate_bar_chart" → "Bar Chart"
 */
function formatChartType(toolName: string): string {
  const type = toolName.replace(/^generate_/, '').replace(/_/g, ' ');
  return type
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Unified header component for MCP tools
 * Renders simple or full header based on config
 */
export function McpToolHeader({
  config,
  collapsed,
  onToggle,
  toolName,
  serverName,
  responseError = false,
}: McpToolHeaderProps) {
  // Simple header (chart-server)
  if (config.headerStyle === 'simple') {
    return (
      <button
        type="button"
        onClick={onToggle}
        className="w-full py-1.5 flex items-center gap-2 text-left rounded transition-colors theme-text-primary hover:bg-black/5 dark:hover:bg-white/5"
      >
        <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" aria-hidden />
        <span className="font-medium">{formatChartType(toolName)}</span>
        <span className={cn('ml-auto shrink-0', collapsed ? 'opacity-60' : 'opacity-80')}>
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>
    );
  }

  // Full header (detailed mode)
  return (
    <button
      type="button"
      onClick={onToggle}
      className={cn(
        'w-full px-4 py-3 flex items-center gap-3 text-left transition-colors',
        'hover:bg-gray-50 dark:hover:bg-gray-800',
        responseError ? 'bg-red-50 dark:bg-red-900/20' : 'bg-gray-50 dark:bg-gray-800'
      )}
    >
      {responseError ? (
        <XCircle className="w-4 h-4 text-red-500 shrink-0" aria-label="Failed" />
      ) : (
        <CheckCircle className="w-4 h-4 text-green-500 shrink-0" aria-hidden />
      )}
      <div className="flex-1 min-w-0">
        <div className="font-medium text-sm text-gray-900 dark:text-gray-100">
          {responseError ? 'Failed ' : 'Ran '}
          {toolName}
          {serverName && (
            <span className="ml-2 text-xs text-gray-500 dark:text-gray-400">
              ({serverName})
            </span>
          )}
        </div>
        {responseError && (
          <div className="text-xs text-red-600 dark:text-red-400 mt-0.5">
            Tool execution failed
          </div>
        )}
      </div>
      <span className="shrink-0 text-gray-400">
        {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
      </span>
    </button>
  );
}
