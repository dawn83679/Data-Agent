import React from 'react';
import { StatusBlock, TextBlock, ThoughtBlock, ToolRunBlock } from '../blocks';
import { SqlExploreGroupBlock } from '../blocks/SqlExploreGroupBlock';
import type { Segment } from './types';
import { SegmentKind } from './types';

export function renderSegment(
  segment: Segment,
  index: number,
  isStreamingThought: boolean,
  allowAutoRetryForToolRun = false,
  showElapsedTextForSubAgent = true,
  isHistoricalMessage = false
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
    case SegmentKind.STATUS:
      return <StatusBlock key={key} statusKey={segment.statusKey} />;
    case SegmentKind.TOOL_GROUP:
      return (
        <SqlExploreGroupBlock
          key={key}
          nestedToolRuns={segment.nestedToolRuns}
          pending={segment.pending}
          isHistoricalMessage={isHistoricalMessage}
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
          toolCallId={segment.toolCallId}
          progressEvents={segment.progressEvents}
          nestedToolRuns={segment.nestedToolRuns}
          allowAutoRetry={allowAutoRetryForToolRun}
          showElapsedText={showElapsedTextForSubAgent}
          isHistoricalMessage={isHistoricalMessage}
        />
      );
  }
}
