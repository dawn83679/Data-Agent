import { ExecuteNonSelectToolStatus, parseExecuteNonSelectToolResult } from '../blocks/executeNonSelectTypes';
import { getToolType, ToolType } from '../blocks/toolTypes';
import type { Segment } from './types';
import { SegmentKind } from './types';

type ToolRunSegment = Extract<Segment, { kind: SegmentKind.TOOL_RUN }>;

function isWriteConfirmation(segment: ToolRunSegment): boolean {
  if (segment.toolName !== 'executeNonSelectSql') return false;
  return parseExecuteNonSelectToolResult(segment.responseData)?.status === ExecuteNonSelectToolStatus.REQUIRES_CONFIRMATION;
}

function isBackgroundToolCandidate(segment: Segment): segment is ToolRunSegment {
  if (segment.kind !== SegmentKind.TOOL_RUN || segment.parentToolCallId) {
    return false;
  }

  if (isWriteConfirmation(segment)) {
    return false;
  }

  const toolType = getToolType(segment.toolName);
  return toolType !== ToolType.TODO
    && toolType !== ToolType.ASK_USER
    && toolType !== ToolType.CHART
    && toolType !== ToolType.THINKING
    && toolType !== ToolType.EXIT_PLAN
    && toolType !== ToolType.FILE_EXPORT
    && toolType !== ToolType.CALLING_SUB_AGENT;
}

function firstTimestamp(run: ToolRunSegment[], key: 'startedAt' | 'finishedAt'): number | undefined {
  return run
    .map((segment) => segment[key])
    .find((value): value is number => typeof value === 'number' && Number.isFinite(value));
}

function lastTimestamp(run: ToolRunSegment[], key: 'startedAt' | 'finishedAt'): number | undefined {
  for (let index = run.length - 1; index >= 0; index -= 1) {
    const value = run[index]?.[key];
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }
  }
  return undefined;
}

export function groupBackgroundToolSegments(
  segments: Segment[],
  options?: { keepTailGroupOpen?: boolean }
): Segment[] {
  const keepTailGroupOpen = options?.keepTailGroupOpen === true;
  const grouped: Segment[] = [];

  for (let index = 0; index < segments.length;) {
    const segment = segments[index];

    if (!segment || !isBackgroundToolCandidate(segment)) {
      grouped.push(segment);
      index += 1;
      continue;
    }

    let end = index + 1;
    while (end < segments.length && isBackgroundToolCandidate(segments[end]!)) {
      end += 1;
    }

    const run = segments.slice(index, end) as ToolRunSegment[];
    const startedAt = firstTimestamp(run, 'startedAt');
    grouped.push({
      kind: SegmentKind.TOOL_GROUP,
      groupType: 'background-tools',
      nestedToolRuns: run,
      pending: keepTailGroupOpen && end === segments.length,
      startedAt,
      finishedAt: lastTimestamp(run, 'finishedAt') ?? lastTimestamp(run, 'startedAt'),
    });

    index = end;
  }

  return grouped;
}
