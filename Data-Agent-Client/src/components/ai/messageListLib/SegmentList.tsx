import React from 'react';
import ReactMarkdown from 'react-markdown';
import { useMarkdownComponents, markdownRemarkPlugins, TodoListBlock } from '../blocks';
import { renderSegment } from './segmentRenderer';
import { findLastTodoSegmentIndex, isTodoSegment } from './segmentTodoUtils';
import type { TodoBoxSpec } from './types';
import type { Segment } from './types';
import { SegmentKind } from './types';

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
}: SegmentListProps): React.ReactElement {
  const markdownComponents = useMarkdownComponents();
  if (segments.length === 0) {
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
          return renderSegment(seg, i, false);
        }
        const isStreamingThought =
          isLastAssistantStreaming &&
          seg.kind === SegmentKind.THOUGHT &&
          seg === lastSeg;
        return renderSegment(seg, i, isStreamingThought);
      })}
    </div>
  );
}
