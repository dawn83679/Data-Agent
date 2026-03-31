import { describe, expect, it } from 'vitest';
import { groupSqlToolSegments } from './groupSqlToolSegments';
import { SegmentKind, ToolExecutionState, type Segment } from './types';

function toolRun(toolName: string, overrides?: Partial<Extract<Segment, { kind: SegmentKind.TOOL_RUN }>>): Segment {
  return {
    kind: SegmentKind.TOOL_RUN,
    toolName,
    parametersData: '{}',
    responseData: '{}',
    executionState: ToolExecutionState.COMPLETE,
    ...overrides,
  };
}

describe('groupSqlToolSegments', () => {
  it('groups consecutive top-level SQL discovery tools including databases and schemas', () => {
    const grouped = groupSqlToolSegments([
      toolRun('getDatabases'),
      toolRun('getSchemas'),
      toolRun('searchObjects'),
    ]);

    expect(grouped).toHaveLength(1);
    expect(grouped[0]).toMatchObject({
      kind: SegmentKind.TOOL_GROUP,
      groupType: 'sql-explore',
      pending: false,
    });
    expect(grouped[0]?.kind === SegmentKind.TOOL_GROUP ? grouped[0].nestedToolRuns.map((segment) => segment.kind === SegmentKind.TOOL_RUN ? segment.toolName : segment.kind) : []).toEqual([
      'getDatabases',
      'getSchemas',
      'searchObjects',
    ]);
  });

  it('does not group nested tool runs under a sub-agent parent', () => {
    const grouped = groupSqlToolSegments([
      toolRun('getDatabases', { parentToolCallId: 'parent-1' }),
      toolRun('getSchemas', { parentToolCallId: 'parent-1' }),
      toolRun('searchObjects', { parentToolCallId: 'parent-1' }),
    ]);

    expect(grouped).toHaveLength(3);
    expect(grouped.every((segment) => segment.kind === SegmentKind.TOOL_RUN)).toBe(true);
  });

  it('marks the tail group as pending while the last assistant turn is still streaming', () => {
    const grouped = groupSqlToolSegments([
      toolRun('getDatabases'),
      toolRun('getSchemas'),
      toolRun('searchObjects', {
        pending: true,
        executionState: ToolExecutionState.EXECUTING,
      }),
    ], { keepTailGroupOpen: true });

    expect(grouped[0]).toMatchObject({
      kind: SegmentKind.TOOL_GROUP,
      pending: true,
    });
  });
});
