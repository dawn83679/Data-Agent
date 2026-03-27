import { useState } from 'react';
import { CheckCircle, ChevronDown, ChevronRight, XCircle } from 'lucide-react';
import { cn } from '../../../lib/utils';
import { TOOL_RUN_LABEL_FAILED, TOOL_RUN_LABEL_RAN } from '../../../constants/chat';
import { ToolRunDetail } from './ToolRunDetail';
import {
  getToolCardClassName,
  TOOL_CARD_CONTENT_CLASSNAME,
  TOOL_CARD_HEADER_CLASSNAME,
  TOOL_CARD_META_CLASSNAME,
} from './toolRunStyles';

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
    <div className={getToolCardClassName(!collapsed)}>
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className={TOOL_CARD_HEADER_CLASSNAME}
      >
        {responseError ? (
          <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" aria-label="Failed" />
        ) : (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" aria-hidden />
        )}
        <span className="min-w-0 flex-1 truncate text-[12px] font-medium theme-text-primary">
          {responseError ? TOOL_RUN_LABEL_FAILED : TOOL_RUN_LABEL_RAN}
          {toolName}
        </span>
        <span className={cn(TOOL_CARD_META_CLASSNAME, 'shrink-0')}>
          {responseError ? 'Error' : 'Tool'}
        </span>
        <span className="shrink-0 theme-text-secondary">
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <div className={TOOL_CARD_CONTENT_CLASSNAME}>
          <ToolRunDetail
            formattedParameters={formattedParameters}
            responseData={responseData}
          />
        </div>
      )}
    </div>
  );
}
