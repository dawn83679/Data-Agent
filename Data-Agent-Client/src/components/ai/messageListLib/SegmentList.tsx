import React from 'react';
import ReactMarkdown from 'react-markdown';
import { markdownComponents, markdownRemarkPlugins, TodoListBlock } from '../blocks';
import { renderSegment } from './segmentRenderer';
import { findLastTodoSegmentIndex, isTodoSegment } from './segmentTodoUtils';
import type { TodoBoxSpec } from './types';
import type { Segment } from './types';
import { SegmentKind } from './types';
import { PLANNING_LABEL } from '../../../constants/chat';

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
  /** When true, show "planning..." with blink (no backend data for 100ms). */
  showPlanningIndicator?: boolean;
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
  showPlanningIndicator = false,
}: SegmentListProps): React.ReactElement {
  if (segments.length === 0) {
    return (
      <div className="space-y-1">
        {showPlanningIndicator && (
          <span className="inline-block text-xs theme-text-secondary animate-blink">
            {PLANNING_LABEL}
          </span>
        )}
        <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
          {fallbackContent || ''}
        </ReactMarkdown>
      </div>
    );
  }

  const lastSeg = segments[segments.length - 1];
  const lastTodoIndex = findLastTodoSegmentIndex(segments);

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
          return renderSegment(seg, i, false, segments.length);
        }
        const isStreamingThought =
          isLastAssistantStreaming &&
          seg.kind === SegmentKind.THOUGHT &&
          seg === lastSeg;
        return renderSegment(seg, i, isStreamingThought, segments.length);
      })}
      {showPlanningIndicator && (
        <span className="inline-block text-xs theme-text-secondary animate-blink mt-1">
          {PLANNING_LABEL}
        </span>
      )}
    </div>
  );
}
