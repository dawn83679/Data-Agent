import { describe, expect, it } from 'vitest';
import { MessageRole } from '../../../types/chat';
import { isCompactSummaryMessage } from './compactSummary';
import type { Message } from './types';

describe('isCompactSummaryMessage', () => {
  it('treats COMPRESSION_SUMMARY as the archived compaction placeholder regardless of role or content', () => {
    const message: Message = {
      id: 'summary-1',
      role: MessageRole.ASSISTANT,
      content: 'raw summary body should not be rendered',
      timestamp: new Date('2026-05-11T00:00:00Z'),
      messageStatus: 'COMPRESSION_SUMMARY',
    };

    expect(isCompactSummaryMessage(message)).toBe(true);
  });

  it('does not treat normal messages or legacy summary text as compaction placeholders', () => {
    const message: Message = {
      id: 'normal-1',
      role: MessageRole.USER,
      content: '[CONVERSATION_SUMMARY]\nlegacy text',
      timestamp: new Date('2026-05-11T00:00:00Z'),
      messageStatus: 'NORMAL',
    };

    expect(isCompactSummaryMessage(message)).toBe(false);
  });
});
