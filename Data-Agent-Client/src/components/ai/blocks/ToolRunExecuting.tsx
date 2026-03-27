import { Loader2 } from 'lucide-react';
import {
  getToolCardClassName,
  TOOL_CARD_HEADER_CLASSNAME,
  TOOL_CARD_META_CLASSNAME,
} from './toolRunStyles';

export interface ToolRunExecutingProps {
  toolName: string;
}

/**
 * Renders a tool call that is currently executing (arguments complete, waiting for result).
 */
export function ToolRunExecuting({ toolName }: ToolRunExecutingProps) {
  return (
    <div className={getToolCardClassName(false)}>
      <div className={TOOL_CARD_HEADER_CLASSNAME}>
        <Loader2 className="h-3.5 w-3.5 animate-spin text-cyan-500" aria-hidden />
        <span className="min-w-0 flex-1 truncate text-[12px] font-medium theme-text-primary">
          {toolName}
        </span>
        <span className={TOOL_CARD_META_CLASSNAME}>Running</span>
      </div>
    </div>
  );
}
