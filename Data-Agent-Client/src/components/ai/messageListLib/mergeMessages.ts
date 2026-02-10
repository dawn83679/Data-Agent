import { MessageRole } from '../../../types/chat';
import type { Message } from './types';

/**
 * Merge all consecutive assistant messages into one so each assistant "turn" is a single bubble.
 * History API returns TOOL_CALL, TOOL_RESULT, TEXT as separate messages; merging here produces
 * one message with blocks [TOOL_CALL, TOOL_RESULT, ..., TEXT] so blocksToSegments can pair and order correctly.
 */
export function mergeAssistantToolPairs(messages: Message[]): Message[] {
  const result: Message[] = [];
  let i = 0;
  while (i < messages.length) {
    const msg = messages[i]!;
    if (msg.role !== MessageRole.ASSISTANT) {
      result.push(msg);
      i++;
      continue;
    }
    const mergedBlocks: Message['blocks'] = [];
    let lastContent = msg.content ?? '';
    let lastId = msg.id;
    while (i < messages.length && messages[i]?.role === MessageRole.ASSISTANT) {
      const m = messages[i]!;
      if (m.blocks?.length) mergedBlocks.push(...m.blocks);
      if (m.content) lastContent = m.content;
      lastId = m.id;
      i++;
    }
    const merged = {
      ...msg,
      id: lastId,
      blocks: mergedBlocks.length > 0 ? mergedBlocks : msg.blocks,
      content: lastContent,
    };
    result.push(merged);
  }

  return result;
}
