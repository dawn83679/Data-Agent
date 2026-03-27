import type { Segment } from './types';
import { SegmentKind } from './types';

const SQL_TOOL_GROUP_NAMES = new Set([
  'getEnvironmentOverview',
  'searchObjects',
  'getObjectDetail',
  'executeSelectSql',
]);

type ToolRunSegment = Extract<Segment, { kind: SegmentKind.TOOL_RUN }>;

function isSqlGroupCandidate(segment: Segment): segment is ToolRunSegment {
  return segment.kind === SegmentKind.TOOL_RUN
    && !segment.parentToolCallId
    && SQL_TOOL_GROUP_NAMES.has(segment.toolName);
}

export function groupSqlToolSegments(
  segments: Segment[],
  options?: { keepTailGroupOpen?: boolean }
): Segment[] {
  const keepTailGroupOpen = options?.keepTailGroupOpen === true;
  const grouped: Segment[] = [];

  for (let index = 0; index < segments.length;) {
    const segment = segments[index];

    if (!segment || !isSqlGroupCandidate(segment)) {
      grouped.push(segment);
      index += 1;
      continue;
    }

    let end = index + 1;
    while (end < segments.length && isSqlGroupCandidate(segments[end]!)) {
      end += 1;
    }

    const run = segments.slice(index, end);
    if (run.length >= 3) {
      grouped.push({
        kind: SegmentKind.TOOL_GROUP,
        groupType: 'sql-explore',
        nestedToolRuns: run,
        pending: keepTailGroupOpen && end === segments.length,
      });
    } else {
      grouped.push(...run);
    }

    index = end;
  }

  return grouped;
}
