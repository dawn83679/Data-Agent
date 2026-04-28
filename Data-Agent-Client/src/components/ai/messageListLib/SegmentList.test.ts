import { describe, expect, it } from 'vitest';
import { shouldExpandThoughtSegment } from './SegmentList';
import { SegmentKind, type Segment } from './types';

describe('shouldExpandThoughtSegment', () => {
  it('keeps thought visible while the assistant message is still streaming even after text arrives', () => {
    const thought: Segment = { kind: SegmentKind.THOUGHT, data: 'reasoning...' };
    const segments: Segment[] = [
      thought,
      { kind: SegmentKind.TEXT, data: 'final answer starts' },
    ];

    expect(shouldExpandThoughtSegment(thought, segments, true)).toBe(true);
  });

  it('collapses thought after streaming finishes', () => {
    const thought: Segment = { kind: SegmentKind.THOUGHT, data: 'reasoning...' };
    const segments: Segment[] = [thought];

    expect(shouldExpandThoughtSegment(thought, segments, false)).toBe(false);
  });
});
