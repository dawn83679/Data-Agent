export interface ToolRunExecutingProps {
  toolName: string;
  parametersData: string;
}

/**
 * Renders a tool call that is currently executing (arguments complete, waiting for result).
 */
export function ToolRunExecuting({ toolName }: ToolRunExecutingProps) {
  return (
    <div className="mb-2 text-[12px] theme-text-secondary">
      <div className="flex items-center gap-2">
        <span className="font-medium animate-pulse theme-text-primary">{toolName}</span>
      </div>
    </div>
  );
}
