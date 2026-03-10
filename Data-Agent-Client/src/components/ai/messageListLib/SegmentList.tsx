import React from 'react';
import ReactMarkdown from 'react-markdown';
import { useMarkdownComponents, markdownRemarkPlugins, TodoListBlock, PlanningIndicator } from '../blocks';
import { renderSegment } from './segmentRenderer';
import { findLastTodoSegmentIndex, isTodoSegment } from './segmentTodoUtils';
import type { TodoBoxSpec } from './types';
import type { Segment } from './types';
import { SegmentKind, ToolExecutionState } from './types';

function hasInFlightToolExecution(segments: Segment[]): boolean {
  return segments.some((seg) => {
    if (seg.kind === SegmentKind.TOOL_RUN) {
      return Boolean(
        seg.pending ||
        seg.executionState === ToolExecutionState.STREAMING_ARGUMENTS ||
        seg.executionState === ToolExecutionState.EXECUTING
      );
    }

    if (seg.kind === SegmentKind.SUB_AGENT) {
      return seg.block.entries.some((entry) =>
        entry.kind === 'tool' && Boolean(
          entry.pending ||
          entry.executionState === ToolExecutionState.STREAMING_ARGUMENTS ||
          entry.executionState === ToolExecutionState.EXECUTING
        )
      );
    }

    return false;
  });
}

export interface SegmentListProps {
  /** Segments to render in order (from blocksToSegments). Same pipeline for streaming and history. */
  segments: Segment[];
  /** Fallback when segments are empty (e.g. markdown content). */
  fallbackContent?: string;
  /** When true, skip rendering raw todo segments (boxes come from overrideTodoBoxes). */
  hideTodoSegments?: boolean;
  /** Todo boxes to show in place of todo segments (one per todoId for this message). */
  overrideTodoBoxes?: TodoBoxSpec[];
  /** When true and the last segment is THOUGHT, pass true to ThoughtBlock defaultExpanded. */
  isLastAssistantStreaming?: boolean;
  /** When true, show Planning indicator (no block received recently). */
  isWaiting?: boolean;
}

/**
 * Generic component: render a list of segments (TEXT, THOUGHT, TOOL_RUN) in order.
 * Used for both streaming and history; only the segments and options differ.
 */
export function SegmentList({
  segments,
  fallbackContent = '',
  hideTodoSegments = false,
  overrideTodoBoxes = [],
  isLastAssistantStreaming = false,
  isWaiting = false,
}: SegmentListProps): React.ReactElement {
  const markdownComponents = useMarkdownComponents();

  // Phase B: empty assistant message, waiting for first block
  if (segments.length === 0) {
    if (isWaiting) {
      return <PlanningIndicator />;
    }
    return (
      <div className="space-y-1">
        <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
          {fallbackContent || ''}
        </ReactMarkdown>
      </div>
    );
  }

  const lastSeg = segments[segments.length - 1];
  const lastTodoIndex = findLastTodoSegmentIndex(segments);
  const hasToolExecuting = hasInFlightToolExecution(segments);

  return (
    <div className="space-y-0">
      {segments.map((seg, i) => {
        if (seg.kind === SegmentKind.TOOL_RUN && isTodoSegment(seg)) {
          if (hideTodoSegments && overrideTodoBoxes.length === 0) return null;
          if (lastTodoIndex >= 0 && i !== lastTodoIndex) return null;
          if (overrideTodoBoxes.length > 0) {
            return (
              <div key={i} className="space-y-2">
                {overrideTodoBoxes.map((box) => (
                  <TodoListBlock key={box.todoId || 'legacy'} items={box.items} />
                ))}
              </div>
            );
          }
          return renderSegment(seg, i, false, isLastAssistantStreaming);
        }
        const isStreamingThought =
          isLastAssistantStreaming &&
          seg.kind === SegmentKind.THOUGHT &&
          seg === lastSeg;
        return renderSegment(seg, i, isStreamingThought, isLastAssistantStreaming);
      })}
      {/* Phase C: show Planning only when the stream is idle and no tool is currently executing. */}
      {isLastAssistantStreaming && isWaiting &&
        !hasToolExecuting &&
        <PlanningIndicator />}
    </div>
  );
}
