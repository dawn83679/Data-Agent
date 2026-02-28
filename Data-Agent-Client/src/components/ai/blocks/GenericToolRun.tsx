import { useState } from 'react';
import { CheckCircle, ChevronDown, ChevronRight, XCircle } from 'lucide-react';
import { cn } from '../../../lib/utils';
import { TOOL_RUN_LABEL_FAILED, TOOL_RUN_LABEL_RAN } from '../../../constants/chat';
import { ToolRunDetail } from './ToolRunDetail';

export interface GenericToolRunProps {
  toolName: string;
  formattedParameters: string;
  responseData: string;
  responseError: boolean;
}

/**
 * Renders a generic tool execution result with collapsible details.
 * Used for built-in database tools and other non-specialized tools.
 */
export function GenericToolRun({
  toolName,
  formattedParameters,
  responseData,
  responseError,
}: GenericToolRunProps) {
  const [collapsed, setCollapsed] = useState(true);

  return (
    <div
      className={cn(
        'mb-2 text-xs rounded transition-colors',
        collapsed ? 'opacity-70 theme-text-secondary' : 'opacity-100 theme-text-primary'
      )}
    >
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
          {responseError ? TOOL_RUN_LABEL_FAILED : TOOL_RUN_LABEL_RAN}
          {toolName}
        </span>
        <span className={cn('ml-auto shrink-0', collapsed ? 'opacity-60' : 'opacity-80')}>
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <ToolRunDetail
          formattedParameters={formattedParameters}
          responseData={responseData}
        />
      )}
    </div>
  );
}
