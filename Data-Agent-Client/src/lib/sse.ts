import type { ChatResponseBlock } from '../types/chat';
import { MessageBlockType } from '../types/chat';
import { ThinkTagStreamParser } from './thinkTagParser';

/**
 * Shared SSE parsing: read stream, split by double newlines, yield JSON from "data:" lines.
 */
async function* parseSSEStream<T>(
    reader: ReadableStreamDefaultReader<Uint8Array>,
    parse: (data: string) => T
): AsyncGenerator<T, void, unknown> {
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (line.startsWith('data:')) {
        const data = line.slice(5).trim();
        try {
          yield parse(data);
        } catch {
          // Ignore parse errors for malformed JSON
        }
      }
    }
  }
}

/**
 * Parse a Response object as an SSE stream. Generic, no business logic.
 * Use this when you already have a Response (e.g. after auth retry).
 *
 * @param response - The fetch Response object
 * @returns AsyncGenerator that yields parsed objects (default ChatResponseBlock)
 */
export async function* sendSSERequest<T = ChatResponseBlock>(
    response: Response
): AsyncGenerator<T, void, unknown> {
  if (!response.ok) {
    throw new Error(`SSE request failed: ${response.status} ${response.statusText}`);
  }

  const reader = response.body?.getReader();
  if (!reader) return;

  yield* parseSSEStream(reader, (data) => JSON.parse(data) as T);
}

/**
 * Parse SSE response and yield ChatResponseBlock objects.
 * Builds on sendSSERequest and adds <think>...</think> tag parsing:
 * TEXT blocks containing <think> tags are split into THOUGHT + TEXT blocks automatically.
 * THOUGHT blocks from backend are passed through unchanged.
 *
 * @param response - The fetch Response object
 * @returns AsyncGenerator that yields ChatResponseBlock objects
 */
export async function* parseSSEResponse(
    response: Response
): AsyncGenerator<ChatResponseBlock, void, unknown> {
  const parser = new ThinkTagStreamParser();

  for await (const block of sendSSERequest<ChatResponseBlock>(response)) {
    if (block.type === MessageBlockType.TEXT && block.data) {
      for (const parsed of parser.parse(block.data)) {
        if (parsed.type === 'THOUGHT_START' || parsed.type === 'THOUGHT_END') continue;
        yield {
          ...block,
          type: parsed.type === 'THOUGHT' ? MessageBlockType.THOUGHT : MessageBlockType.TEXT,
          data: parsed.content,
        };
      }
    } else {
      yield block;
    }

    if (block.done) {
      for (const parsed of parser.flush()) {
        if (parsed.type === 'THOUGHT_START' || parsed.type === 'THOUGHT_END') continue;
        yield {
          ...block,
          type: parsed.type === 'THOUGHT' ? MessageBlockType.THOUGHT : MessageBlockType.TEXT,
          data: parsed.content,
          done: true,
        };
      }
    }
  }
}
