import { ListTodo, Loader2 } from 'lucide-react';
import { SqlCodeBlock } from '../common/SqlCodeBlock';
import type { ExitPlanPayload } from '../ai/blocks/exitPlanModeTypes';

export interface PlanConsoleProps {
  tabId: string;
  payload: ExitPlanPayload;
  /** True while the plan is still being streamed from the model. */
  isStreaming?: boolean;
}

/**
 * Read-only plan viewer rendered in the editor tab area.
 * Shows plan title + steps with SQL. Handles partial/streaming data gracefully.
 * Action buttons (Execute/Exit/Continue) and risks remain in the chat sidebar card.
 */
export function PlanConsole({ payload, isStreaming }: PlanConsoleProps) {
  return (
    <div className="flex-1 overflow-auto theme-bg-main p-6">
      <div className="max-w-4xl mx-auto flex flex-col gap-5">
        {/* Header */}
        <div className="flex items-center gap-3 text-amber-600 dark:text-amber-400">
          <ListTodo className="w-5 h-5" />
          <h2 className="text-[16px] font-semibold">{payload.title}</h2>
          {isStreaming && (
            <Loader2 className="w-4 h-4 animate-spin text-amber-500" />
          )}
        </div>

        {/* Steps */}
        {payload.steps.length > 0 && (
          <div className="flex flex-col gap-3">
            {payload.steps.map((step) => (
              <div key={step.order} className="rounded-lg border theme-border theme-bg-panel overflow-hidden">
                <div className="px-4 py-2.5 text-[13px] font-medium border-b theme-border flex justify-between items-center">
                  <span>
                    <span className="text-amber-600 dark:text-amber-400 font-mono mr-2">#{step.order}</span>
                    {step.description}
                  </span>
                  {step.objectName && (
                    <span className="opacity-60 font-mono text-[12px]">{step.objectName}</span>
                  )}
                </div>
                {step.sql && (
                  <SqlCodeBlock variant="compact" sql={step.sql} wrapLongLines={true} />
                )}
              </div>
            ))}
          </div>
        )}

        {/* Streaming placeholder when no steps yet */}
        {isStreaming && payload.steps.length === 0 && (
          <div className="flex items-center gap-2 text-[13px] theme-text-secondary py-4">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Generating plan...</span>
          </div>
        )}
      </div>
    </div>
  );
}
