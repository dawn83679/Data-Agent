import { cn } from '../../../lib/utils';
import { PLANNING_LABEL } from '../../../constants/chat';

export interface PlanningIndicatorProps {
  className?: string;
}

/** Pulsing "Planning..." shown while waiting for AI response or during inter-block gaps. Matches ToolRunPending style. */
export function PlanningIndicator({ className }: PlanningIndicatorProps) {
  return (
    <div className={cn('mb-2 text-xs opacity-70 theme-text-secondary', className)}>
      <div className="w-full py-1.5 flex items-center gap-2 text-left rounded theme-text-primary">
        <span className="font-medium animate-pulse">{PLANNING_LABEL}</span>
      </div>
    </div>
  );
}
