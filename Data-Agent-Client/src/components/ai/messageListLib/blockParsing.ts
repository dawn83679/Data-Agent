import type { ChatResponseBlock } from '../../../types/chat';
import type { ToolCallData, ToolResultData } from '../../../types/chat';

export function parseToolCall(block: ChatResponseBlock): ToolCallData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolCallData;
    return parsed?.toolName != null ? parsed : null;
  } catch {
    return null;
  }
}

export function parseToolResult(block: ChatResponseBlock): ToolResultData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolResultData;
    return parsed?.toolName != null ? parsed : null;
  } catch {
    return null;
  }
}

/** Normalize id for comparison (backend may send number or string). */
export function idStr(v: unknown): string {
  return v == null ? '' : String(v);
}

export function matchById(
  call: ToolCallData | null,
  result: ToolResultData | null,
  idStrFn: (v: unknown) => string
): boolean {
  const cid = call?.id != null && call.id !== '' ? idStrFn(call.id) : '';
  const rid = result?.id != null && result.id !== '' ? idStrFn(result.id) : '';
  return cid !== '' && rid !== '' && cid === rid;
}
