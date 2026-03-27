import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, ChevronDown, ChevronRight, Database, Loader2, XCircle } from 'lucide-react';
import { cn } from '../../../lib/utils';
import { useTheme } from '../../../hooks/useTheme';
import { getAgentTheming, getNestedToolStats } from './subAgentDataHelpers';
import { ToolRunBlock } from './ToolRunBlock';
import type { Segment } from '../messageListLib/types';
import { SegmentKind } from '../messageListLib/types';

export interface SqlExploreGroupBlockProps {
  nestedToolRuns: Segment[];
  pending?: boolean;
  isHistoricalMessage?: boolean;
}

type ToolRunSegment = Extract<Segment, { kind: SegmentKind.TOOL_RUN }>;

function formatToolSummary(segments: ToolRunSegment[]): string[] {
  const counts = new Map<string, number>();
  segments.forEach((segment) => {
    counts.set(segment.toolName, (counts.get(segment.toolName) ?? 0) + 1);
  });

  return [...counts.entries()].map(([toolName, count]) => (
    count > 1 ? `${toolName} ×${count}` : toolName
  ));
}

export function SqlExploreGroupBlock({
  nestedToolRuns,
  pending = false,
  isHistoricalMessage = false,
}: SqlExploreGroupBlockProps) {
  const { theme } = useTheme();
  const toolRuns = useMemo(
    () => nestedToolRuns.filter((segment): segment is ToolRunSegment => segment.kind === SegmentKind.TOOL_RUN),
    [nestedToolRuns]
  );
  const stats = useMemo(() => getNestedToolStats(toolRuns), [toolRuns]);
  const toolSummary = useMemo(() => formatToolSummary(toolRuns), [toolRuns]);
  const isError = !!stats.failedToolName;
  const isRunning = pending || !!stats.runningToolName;
  const [collapsed, setCollapsed] = useState(true);
  const { borderColor, bgColor, iconColor } = getAgentTheming('explorer', isError, theme);

  useEffect(() => {
    if (isRunning) {
      setCollapsed(false);
      return;
    }
    if (!isError) {
      setCollapsed(true);
    }
  }, [isError, isRunning]);

  const glassStyle =
    theme === 'dark'
      ? isError
        ? {
            background: 'linear-gradient(135deg, rgba(239,68,68,0.14), rgba(127,29,29,0.08) 32%, rgba(30,33,43,0.96) 100%)',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.05), 0 14px 30px rgba(2,6,23,0.28)',
          }
        : {
            background: 'linear-gradient(135deg, rgba(34,211,238,0.16), rgba(14,116,144,0.08) 28%, rgba(30,33,43,0.96) 100%)',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.05), 0 14px 30px rgba(8,47,73,0.24)',
          }
      : isError
        ? {
            background: 'linear-gradient(135deg, rgba(251,113,133,0.12), rgba(255,255,255,0.95) 42%, rgba(255,255,255,0.99) 100%)',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.96), 0 10px 24px rgba(244,63,94,0.08)',
          }
        : {
            background: 'linear-gradient(135deg, rgba(34,197,94,0.04), rgba(125,211,252,0.14) 20%, rgba(255,255,255,0.97) 58%, rgba(248,252,255,0.99) 100%)',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.96), 0 12px 28px rgba(14,165,233,0.08)',
          };

  return (
    <div className={cn('mb-2 overflow-hidden rounded-xl border', borderColor, bgColor)} style={glassStyle}>
      <button
        type="button"
        onClick={() => setCollapsed((current) => !current)}
        className="flex w-full items-start gap-2.5 px-3 py-2.5 text-left transition-colors hover:bg-black/5 dark:hover:bg-white/[0.04]"
      >
        <span className="pt-0.5">
          {isError ? (
            <XCircle className="h-4 w-4 shrink-0 text-red-500" aria-label="Failed" />
          ) : isRunning ? (
            <Loader2 className={cn('h-4 w-4 shrink-0 animate-spin', iconColor)} aria-hidden />
          ) : (
            <CheckCircle2 className="h-4 w-4 shrink-0 text-green-500" aria-hidden />
          )}
        </span>
        <Database className={cn('mt-0.5 h-4 w-4 shrink-0', iconColor)} aria-hidden />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[12px] font-semibold theme-text-primary">Explore</span>
            <span className="rounded-full border theme-border px-2 py-0.5 text-[10px] theme-text-secondary">
              {stats.completedCount}/{stats.totalCount} complete
            </span>
          </div>
        </div>
        <span className="mt-0.5 shrink-0 theme-text-secondary">
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </span>
      </button>

      {!collapsed && (
        <div className="border-t theme-border px-3 pb-3 pt-2">
          {toolSummary.length > 0 && (
            <div className="mb-2 flex flex-wrap gap-1.5">
              {toolSummary.map((label) => (
                <span
                  key={label}
                  className="rounded-full border theme-border bg-black/5 px-2 py-0.5 text-[10px] theme-text-secondary dark:bg-white/[0.04]"
                >
                  {label}
                </span>
              ))}
            </div>
          )}

          <div className="space-y-2 border-l theme-border pl-3">
            {toolRuns.map((segment, index) => (
              <ToolRunBlock
                key={segment.toolCallId ?? `${segment.toolName}-${index}`}
                toolName={segment.toolName}
                parametersData={segment.parametersData}
                responseData={segment.responseData}
                responseError={segment.responseError}
                pending={segment.pending}
                executionState={segment.executionState}
                toolCallId={segment.toolCallId}
                progressEvents={segment.progressEvents}
                nestedToolRuns={segment.nestedToolRuns}
                showElapsedText={false}
                isHistoricalMessage={isHistoricalMessage}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
