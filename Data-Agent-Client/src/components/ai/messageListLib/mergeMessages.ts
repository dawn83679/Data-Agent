import { MessageBlockType } from '../../../types/chat';
import { MessageRole } from '../../../types/chat';
import type { Message } from './types';

/**
 * Merge consecutive assistant messages so TOOL_CALL and TOOL_RESULT live in one bubble.
 * History API returns them as separate messages; merging here matches streaming behaviour.
 */
export function mergeAssistantToolPairs(messages: Message[]): Message[] {
  const result: Message[] = [];
  for (let i = 0; i < messages.length; i++) {
    const msg = messages[i]!;
    if (msg.role !== MessageRole.ASSISTANT || !msg.blocks?.length) {
      result.push(msg);
      continue;
    }
    const hasUnpairedToolCall = msg.blocks.some(
      (b, j) =>
        b.type === MessageBlockType.TOOL_CALL &&
        msg.blocks![j + 1]?.type !== MessageBlockType.TOOL_RESULT
    );
    const next = messages[i + 1];
    if (
      hasUnpairedToolCall &&
      next?.role === MessageRole.ASSISTANT &&
      next.blocks?.length &&
      next.blocks.every((b) => b.type === MessageBlockType.TOOL_RESULT)
    ) {
      result.push({
        ...msg,
        blocks: [...(msg.blocks ?? []), ...next.blocks],
        content: msg.content || next.content,
      });
      i++;
    } else {
      result.push(msg);
    }
  }
  return result;
}
