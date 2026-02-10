import type { ChatResponseBlock } from '../types/chat';

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
 * Parse SSE (Server-Sent Events) response and yield ChatResponseBlock objects.
 *
 * @param response - The fetch Response object
 * @returns AsyncGenerator that yields ChatResponseBlock objects
 */
export async function* parseSSEResponse(
  response: Response
): AsyncGenerator<ChatResponseBlock, void, unknown> {
  const reader = response.body?.getReader();
  if (!reader) return;

  yield* parseSSEStream(reader, (data) => JSON.parse(data) as ChatResponseBlock);
}

/**
 * Send a POST request with SSE streaming and parse the response.
 *
 * @param url - The API endpoint
 * @param body - The request body
 * @param options - Fetch options
 * @returns AsyncGenerator that yields parsed objects (default ChatResponseBlock)
 */
export async function* sendSSERequest<T = ChatResponseBlock>(
  url: string,
  body: unknown,
  options: RequestInit = {}
): AsyncGenerator<T, void, unknown> {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    body: JSON.stringify(body),
    ...options,
  });

  if (!response.ok) {
    throw new Error(`SSE request failed: ${response.status} ${response.statusText}`);
  }

  const reader = response.body?.getReader();
  if (!reader) return;

  yield* parseSSEStream(reader, (data) => JSON.parse(data) as T);
}
