/**
 * Stream parser for <think>...</think> tags.
 * Maintains state across chunks to properly handle tags split across multiple chunks.
 *
 * Key features:
 * 1. Handles tags split across chunks (e.g., "<thi" + "nk>") via buffer mechanism
 * 2. State machine approach - no full-text reparsing on each chunk
 * 3. Tags are never rendered - only content is emitted
 * 4. Handles incomplete streams (missing </think>) by flushing as THOUGHT
 */

export enum ParserState {
  TEXT = 'TEXT',
  THOUGHT = 'THOUGHT',
}

export type ParsedChunk =
  | { type: 'TEXT'; content: string }
  | { type: 'THOUGHT'; content: string }
  | { type: 'THOUGHT_START' }  // 遇到 <think>，打开思考框
  | { type: 'THOUGHT_END' };    // 遇到 </think>，关闭思考框

export class ThinkTagStreamParser {
  private state: ParserState = ParserState.TEXT;
  private buffer = '';
  private readonly OPEN_TAG = '<think>';
  private readonly CLOSE_TAG = '</think>';

  /**
   * Parse incoming text chunk and yield parsed segments.
   * Handles tags split across chunks by maintaining internal buffer.
   * Optimized for streaming: outputs content immediately without waiting.
   *
   * Emits events:
   * - THOUGHT_START: when <think> is detected (UI should open thinking box)
   * - THOUGHT: incremental thinking content (UI should append to thinking box)
   * - THOUGHT_END: when </think> is detected (UI should close thinking state)
   * - TEXT: normal text content
   */
  *parse(chunk: string): Generator<ParsedChunk> {
    this.buffer += chunk;

    while (this.buffer.length > 0) {
      if (this.state === ParserState.TEXT) {
        // Look for opening tag
        const openIndex = this.buffer.indexOf(this.OPEN_TAG);

        if (openIndex === -1) {
          // No opening tag found - check if buffer might contain partial tag at the end
          if (this.mightContainPartialTag(this.buffer, this.OPEN_TAG)) {
            // Keep only the potential partial tag in buffer, emit the rest
            const safeLength = this.buffer.length - (this.OPEN_TAG.length - 1);
            if (safeLength > 0) {
              yield { type: 'TEXT', content: this.buffer.slice(0, safeLength) };
              this.buffer = this.buffer.slice(safeLength);
            }
            break;
          } else {
            // Safe to emit all - no partial tag possible
            if (this.buffer.length > 0) {
              yield { type: 'TEXT', content: this.buffer };
              this.buffer = '';
            }
            break;
          }
        } else {
          // Found opening tag
          if (openIndex > 0) {
            yield { type: 'TEXT', content: this.buffer.slice(0, openIndex) };
          }
          // Emit THOUGHT_START event (UI should open thinking box)
          yield { type: 'THOUGHT_START' };
          this.buffer = this.buffer.slice(openIndex + this.OPEN_TAG.length);
          this.state = ParserState.THOUGHT;
        }
      } else {
        // state === THOUGHT
        // Look for closing tag
        const closeIndex = this.buffer.indexOf(this.CLOSE_TAG);

        if (closeIndex === -1) {
          // No closing tag found - emit content immediately for streaming
          // Only keep potential partial closing tag in buffer
          if (this.mightContainPartialTag(this.buffer, this.CLOSE_TAG)) {
            const safeLength = this.buffer.length - (this.CLOSE_TAG.length - 1);
            if (safeLength > 0) {
              yield { type: 'THOUGHT', content: this.buffer.slice(0, safeLength) };
              this.buffer = this.buffer.slice(safeLength);
            }
            break;
          } else {
            // No partial tag - emit everything immediately
            if (this.buffer.length > 0) {
              yield { type: 'THOUGHT', content: this.buffer };
              this.buffer = '';
            }
            break;
          }
        } else {
          // Found closing tag
          if (closeIndex > 0) {
            yield { type: 'THOUGHT', content: this.buffer.slice(0, closeIndex) };
          }
          // Emit THOUGHT_END event (UI should close thinking state)
          yield { type: 'THOUGHT_END' };
          this.buffer = this.buffer.slice(closeIndex + this.CLOSE_TAG.length);
          this.state = ParserState.TEXT;
        }
      }
    }
  }

  /**
   * Flush any remaining buffer content (call when stream ends).
   * Handles edge case: if stream ends while still in THOUGHT state (missing </think>),
   * treats remaining buffer as THOUGHT content and emits THOUGHT_END.
   */
  *flush(): Generator<ParsedChunk> {
    if (this.buffer.length > 0) {
      // If still in THOUGHT state, it means </think> was never received
      // Emit remaining content as THOUGHT (model output was incomplete)
      yield {
        type: this.state === ParserState.THOUGHT ? 'THOUGHT' : 'TEXT',
        content: this.buffer,
      };
      this.buffer = '';
    }
    // If we're still in THOUGHT state, emit THOUGHT_END to close the thinking box
    if (this.state === ParserState.THOUGHT) {
      yield { type: 'THOUGHT_END' };
    }
    // Reset state for next message
    this.state = ParserState.TEXT;
  }

  /**
   * Check if buffer end might contain a partial tag.
   */
  private mightContainPartialTag(buffer: string, tag: string): boolean {
    for (let i = 1; i < tag.length; i++) {
      if (buffer.endsWith(tag.slice(0, i))) {
        return true;
      }
    }
    return false;
  }

}
