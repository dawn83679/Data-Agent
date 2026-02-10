import { isTodoTool, parseTodoListResponse } from '../blocks';
import type { Segment } from './types';
import { SegmentKind } from './types';

export function isTodoSegment(seg: Segment): boolean {
  if (seg.kind !== SegmentKind.TOOL_RUN) return false;
  return isTodoTool(seg.toolName) && parseTodoListResponse(seg.responseData) != null;
}

export function segmentsHaveTodo(segments: Segment[]): boolean {
  return segments.some(isTodoSegment);
}

export function findLastTodoSegmentIndex(segments: Segment[]): number {
  for (let j = segments.length - 1; j >= 0; j--) {
    if (segments[j] && isTodoSegment(segments[j]!)) return j;
  }
  return -1;
}
