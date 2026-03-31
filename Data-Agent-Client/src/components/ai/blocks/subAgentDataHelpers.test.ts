import { describe, expect, it } from 'vitest';
import { getSubAgentStatusText } from './subAgentDataHelpers';
import { SegmentKind, ToolExecutionState, type Segment } from '../messageListLib/types';

function toolRun(toolName: string, executionState: ToolExecutionState, responseError = false): Segment {
  return {
    kind: SegmentKind.TOOL_RUN,
    toolName,
    parametersData: '{}',
    responseData: '{}',
    executionState,
    responseError,
  };
}

describe('getSubAgentStatusText', () => {
  it('uses readable tool labels while a nested discovery tool is running', () => {
    const text = getSubAgentStatusText({
      isComplete: false,
      isError: false,
      nestedToolRuns: [toolRun('getDatabases', ToolExecutionState.EXECUTING)],
    });

    expect(text).toBe('Calling Get Databases... (0/1)');
  });

  it('uses readable tool labels for failures too', () => {
    const text = getSubAgentStatusText({
      isComplete: false,
      isError: true,
      nestedToolRuns: [toolRun('getSchemas', ToolExecutionState.COMPLETE, true)],
    });

    expect(text).toBe('Failed at Get Schemas');
  });
});
