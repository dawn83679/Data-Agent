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

export function getAgentTheming(agentType: string, isError: boolean, theme: 'light' | 'dark'): AgentTheming {
  const isExplorer = agentType.toUpperCase() === 'EXPLORER' || agentType === 'explorer';
  const isDark = theme === 'dark';
  const borderColor = isError
    ? (isDark ? 'border-red-700/70' : 'border-rose-300')
    : isExplorer
      ? (isDark ? 'border-cyan-700/70' : 'border-cyan-300')
      : (isDark ? 'border-purple-700/70' : 'border-violet-300');
  const bgColor = isError
    ? (isDark ? 'bg-red-950/20' : 'bg-gradient-to-r from-rose-50 via-white to-white')
    : isExplorer
      ? (isDark ? 'bg-cyan-950/20' : 'bg-gradient-to-r from-cyan-50 via-white to-white')
      : (isDark ? 'bg-purple-950/20' : 'bg-gradient-to-r from-violet-50 via-white to-white');
  const iconColor = isError
    ? 'text-red-500'
    : isExplorer
      ? (isDark ? 'text-cyan-400' : 'text-cyan-600')
      : (isDark ? 'text-purple-400' : 'text-violet-600');
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
