export interface ToolRunPendingProps {
  toolName: string;
}

/**
 * Renders a pending tool execution state (waiting for TOOL_RESULT).
 */
export function ToolRunPending({ toolName }: ToolRunPendingProps) {
  return (
    <div className="mb-2 text-xs opacity-70 theme-text-secondary">
      <div className="w-full py-1.5 flex items-center gap-2 text-left rounded theme-text-primary">
        <span className="font-medium animate-pulse">{toolName}</span>
      </div>
    </div>
  );
}
