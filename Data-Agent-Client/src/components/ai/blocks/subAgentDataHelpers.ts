import type { Segment } from '../messageListLib/types';
import { SegmentKind, ToolExecutionState } from '../messageListLib/types';
import type { NestedToolCall } from '../../../types/tab';

/* ─── Theming by agent type ─── */

export interface AgentTheming {
  isExplorer: boolean;
  borderColor: string;
  bgColor: string;
  iconColor: string;
}

export function getAgentTheming(agentType: string, isError: boolean): AgentTheming {
  const isExplorer = agentType.toUpperCase() === 'EXPLORER' || agentType === 'explorer';
  const borderColor = isError
    ? 'border-red-300 dark:border-red-700'
    : isExplorer
      ? 'border-cyan-300 dark:border-cyan-700'
      : 'border-purple-300 dark:border-purple-700';
  const bgColor = isError
    ? 'bg-red-50/50 dark:bg-red-900/10'
    : isExplorer
      ? 'bg-cyan-50/50 dark:bg-cyan-900/10'
      : 'bg-purple-50/50 dark:bg-purple-900/10';
  const iconColor = isError
    ? 'text-red-500'
    : isExplorer
      ? 'text-cyan-600 dark:text-cyan-400'
      : 'text-purple-600 dark:text-purple-400';
  return { isExplorer, borderColor, bgColor, iconColor };
}

/* ─── Build NestedToolCall[] from Segment[] ─── */

export function buildNestedToolCalls(nestedToolRuns?: Segment[]): NestedToolCall[] | undefined {
  const calls = nestedToolRuns
    ?.filter((s): s is Extract<Segment, { kind: typeof SegmentKind.TOOL_RUN }> => s.kind === SegmentKind.TOOL_RUN)
    .map((seg) => {
      const execState = seg.executionState;
      const running = execState === ToolExecutionState.STREAMING_ARGUMENTS || execState === ToolExecutionState.EXECUTING;
      const complete = execState === ToolExecutionState.COMPLETE;
      const status = complete ? 'complete' : running ? 'running' : 'pending';
      return { toolName: seg.toolName, status, responseError: seg.responseError } as NestedToolCall;
    });
  return calls?.length ? calls : undefined;
}

/* ─── Console tab ID helper ─── */

export function subAgentConsoleTabId(toolCallId: string, taskKey?: string): string {
  return taskKey ? `subagent-console-${toolCallId}-${taskKey}` : `subagent-console-${toolCallId}`;
}

type ToolRunSeg = Extract<Segment, { kind: typeof SegmentKind.TOOL_RUN }>;

function asToolRunSegments(nestedToolRuns?: Segment[]): ToolRunSeg[] {
  return nestedToolRuns?.filter((s): s is ToolRunSeg => s.kind === SegmentKind.TOOL_RUN) ?? [];
}

function isRunningTool(segment: ToolRunSeg): boolean {
  return segment.executionState === ToolExecutionState.STREAMING_ARGUMENTS
    || segment.executionState === ToolExecutionState.EXECUTING;
}

export interface NestedToolStats {
  totalCount: number;
  completedCount: number;
  runningToolName?: string;
  lastCompletedToolName?: string;
  failedToolName?: string;
}

export function getNestedToolStats(nestedToolRuns?: Segment[]): NestedToolStats {
  const toolRunSegs = asToolRunSegments(nestedToolRuns);
  const totalCount = toolRunSegs.length;
  const completedCount = toolRunSegs.filter((segment) => segment.executionState === ToolExecutionState.COMPLETE).length;
  const runningTool = [...toolRunSegs].reverse().find(isRunningTool);
  const lastCompletedTool = [...toolRunSegs].reverse().find((segment) => segment.executionState === ToolExecutionState.COMPLETE);
  const failedTool = [...toolRunSegs].reverse().find((segment) => segment.responseError);

  return {
    totalCount,
    completedCount,
    runningToolName: runningTool?.toolName,
    lastCompletedToolName: lastCompletedTool?.toolName,
    failedToolName: failedTool?.toolName,
  };
}

export function getSubAgentStatusText(options: {
  isComplete: boolean;
  isError: boolean;
  nestedToolRuns?: Segment[];
  resultSummary?: string;
}): string {
  const { isComplete, isError, nestedToolRuns } = options;
  const stats = getNestedToolStats(nestedToolRuns);

  if (isError) {
    return stats.failedToolName ? `Failed at ${stats.failedToolName}` : 'Agent failed';
  }

  if (isComplete) {
    return 'Complete';
  }

  if (stats.runningToolName) {
    return `Calling ${stats.runningToolName}... (${stats.completedCount}/${stats.totalCount})`;
  }

  if (stats.totalCount > 0 && stats.completedCount === stats.totalCount) {
    return 'Starting summary...';
  }

  if (stats.lastCompletedToolName) {
    return `Called ${stats.lastCompletedToolName}... (${stats.completedCount}/${stats.totalCount})`;
  }

  return 'Starting Agent...';
}
