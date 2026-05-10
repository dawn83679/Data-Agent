import { describe, expect, it } from 'vitest';
import { MessageRole } from '../../../types/chat';
import { mergeAssistantToolPairs } from './mergeMessages';
import type { Message } from './types';

describe('mergeAssistantToolPairs', () => {
  it('keeps a hidden user boundary between assistant turns', () => {
    const messages: Message[] = [
      {
        id: 'assistant-1',
        role: MessageRole.ASSISTANT,
        content: '',
        timestamp: new Date(),
        blocks: [{ type: 'TOOL_CALL', data: '{"toolName":"askUserQuestion"}', done: false }],
      },
      {
        id: 'hidden-answer',
        role: MessageRole.USER,
        content: '',
        localKind: 'hidden-user-boundary',
        timestamp: new Date(),
      },
      {
        id: 'assistant-2',
        role: MessageRole.ASSISTANT,
        content: '继续处理结果',
        timestamp: new Date(),
      },
    ];

    const merged = mergeAssistantToolPairs(messages);

    expect(merged).toHaveLength(3);
    expect(merged[0]?.id).toBe('assistant-1');
    expect(merged[1]?.localKind).toBe('hidden-user-boundary');
    expect(merged[2]?.id).toBe('assistant-2');
  });
});
