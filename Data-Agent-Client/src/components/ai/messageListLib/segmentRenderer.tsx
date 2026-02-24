import React from 'react';
import { TextBlock, ThoughtBlock, ToolRunBlock } from '../blocks';
import type { Segment } from './types';
import { SegmentKind } from './types';

export function renderSegment(
  segment: Segment,
  index: number,
  isStreamingThought: boolean
): React.ReactNode {
  const key = `seg-${index}-${segment.kind}`;
  switch (segment.kind) {
    case SegmentKind.TEXT:
      return <TextBlock key={key} data={segment.data} />;
    case SegmentKind.THOUGHT:
      return (
        <ThoughtBlock
          key={key}
          data={segment.data}
          defaultExpanded={isStreamingThought}
        />
      );
    case SegmentKind.TOOL_RUN:
      return (
        <ToolRunBlock
          key={key}
          toolName={segment.toolName}
          parametersData={segment.parametersData}
          responseData={segment.responseData}
          responseError={segment.responseError}
          pending={segment.pending}
          serverName={segment.serverName}
        />
      );
  }
}
