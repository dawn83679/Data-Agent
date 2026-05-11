import { describe, expect, it } from 'vitest';
import { groupBackgroundToolSegments } from './groupBackgroundToolSegments';
import { SegmentKind, ToolExecutionState, type Segment } from './types';
import { PermissionGrantPreset, PermissionScopeType } from '../../../types/permission';

function toolRun(toolName: string, overrides?: Partial<Extract<Segment, { kind: SegmentKind.TOOL_RUN }>>): Segment {
  return {
    kind: SegmentKind.TOOL_RUN,
    toolName,
    parametersData: '{}',
    responseData: '{}',
    executionState: ToolExecutionState.COMPLETE,
    startedAt: 1_000,
    finishedAt: 2_000,
    ...overrides,
  };
}

describe('groupBackgroundToolSegments', () => {
  it('groups a single background tool into a summary group', () => {
    const grouped = groupBackgroundToolSegments([
      toolRun('getConnections'),
    ]);

    expect(grouped).toHaveLength(1);
    expect(grouped[0]).toMatchObject({
      kind: SegmentKind.TOOL_GROUP,
      groupType: 'background-tools',
      pending: false,
      startedAt: 1_000,
      finishedAt: 2_000,
    });
    expect(grouped[0]?.kind === SegmentKind.TOOL_GROUP ? grouped[0].nestedToolRuns : []).toHaveLength(1);
  });

  it('groups consecutive background tools into a single summary group', () => {
    const grouped = groupBackgroundToolSegments([
      toolRun('getDatabases'),
      toolRun('getSchemas', { startedAt: 2_000, finishedAt: 3_200 }),
      toolRun('searchObjects', { startedAt: 3_200, finishedAt: 5_200 }),
    ]);

    expect(grouped).toHaveLength(1);
    expect(grouped[0]).toMatchObject({
      kind: SegmentKind.TOOL_GROUP,
      groupType: 'background-tools',
      pending: false,
      startedAt: 1_000,
      finishedAt: 5_200,
    });
    expect(grouped[0]?.kind === SegmentKind.TOOL_GROUP ? grouped[0].nestedToolRuns : []).toHaveLength(3);
  });

  it('groups executeSelectSql and activateSkill with background tools but keeps final artifacts interactive', () => {
    const grouped = groupBackgroundToolSegments([
      toolRun('getDatabases'),
      toolRun('executeSelectSql'),
      toolRun('activateSkill'),
      toolRun('renderChart'),
      toolRun('exportFile'),
      toolRun('askUserQuestion'),
      toolRun('executeNonSelectSql', {
        responseData: JSON.stringify({
          status: 'REQUIRES_CONFIRMATION',
          ruleMatched: false,
          requiresConfirmation: true,
          confirmation: {
            conversationId: 1,
            connectionId: 8,
            sql: 'DELETE FROM users',
            sqlPreview: 'DELETE FROM users',
            availableGrantOptions: [{
              scopeType: PermissionScopeType.CONVERSATION,
              grantPreset: PermissionGrantPreset.EXACT_SCHEMA,
            }],
          },
        }),
      }),
      toolRun('callingExplorerSubAgent'),
    ]);

    expect(grouped.map((segment) => segment.kind)).toEqual([
      SegmentKind.TOOL_GROUP,
      SegmentKind.TOOL_RUN,
      SegmentKind.TOOL_RUN,
      SegmentKind.TOOL_RUN,
      SegmentKind.TOOL_RUN,
      SegmentKind.TOOL_RUN,
    ]);
    expect(grouped[0]?.kind === SegmentKind.TOOL_GROUP ? grouped[0].nestedToolRuns : []).toHaveLength(3);
  });

  it('does not group nested tool runs under a sub-agent parent', () => {
    const grouped = groupBackgroundToolSegments([
      toolRun('getDatabases', { parentToolCallId: 'parent-1' }),
      toolRun('getSchemas', { parentToolCallId: 'parent-1' }),
      toolRun('searchObjects', { parentToolCallId: 'parent-1' }),
    ]);

    expect(grouped).toHaveLength(3);
    expect(grouped.every((segment) => segment.kind === SegmentKind.TOOL_RUN)).toBe(true);
  });

  it('groups thinking with background tools', () => {
    const grouped = groupBackgroundToolSegments([
      toolRun('getDatabases'),
      toolRun('thinking'),
      toolRun('getSchemas'),
    ]);

    expect(grouped).toHaveLength(1);
    expect(grouped[0]).toMatchObject({
      kind: SegmentKind.TOOL_GROUP,
      groupType: 'background-tools',
    });
    expect(grouped[0]?.kind === SegmentKind.TOOL_GROUP
      ? grouped[0].nestedToolRuns
        .filter((segment) => segment.kind === SegmentKind.TOOL_RUN)
        .map((segment) => segment.toolName)
      : [])
      .toEqual(['getDatabases', 'thinking', 'getSchemas']);
  });

  it('marks the tail group as pending while the last assistant turn is still streaming', () => {
    const grouped = groupBackgroundToolSegments([
      toolRun('getDatabases'),
      toolRun('getSchemas', {
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
