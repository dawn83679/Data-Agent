import { MessageBlockType } from '../../../types/chat';
import type { ChatResponseBlock } from '../../../types/chat';
import type { ToolCallData } from '../../../types/chat';
import { parseToolCall, parseToolResult, idStr, matchById } from './blockParsing';
import type { Segment } from './types';
import { SegmentKind } from './types';

/**
 * Merge consecutive TOOL_CALL blocks with the same id (streaming chunks) by concatenating arguments.
 */
function mergeConsecutiveToolCalls(
  blocks: ChatResponseBlock[],
  startIndex: number,
  firstCall: ToolCallData
): { endIndex: number; lastCall: ToolCallData; parametersData: string } {
  let j = startIndex;
  let lastCall: ToolCallData = firstCall;
  const hasId = firstCall.id != null && firstCall.id !== '';
  const firstIdStr = idStr(firstCall.id);

  while (hasId && j + 1 < blocks.length && blocks[j + 1]?.type === MessageBlockType.TOOL_CALL) {
    const nextCall = parseToolCall(blocks[j + 1]!);
    if (!nextCall || idStr(nextCall.id) !== firstIdStr) break;
    j++;
    lastCall = {
      ...nextCall,
      arguments: (lastCall.arguments ?? '') + (nextCall.arguments ?? ''),
    };
  }
  return { endIndex: j, lastCall, parametersData: lastCall.arguments ?? '' };
}

/**
 * Find TOOL_RESULT block with the same id as the given call (search from after callEndIndex).
 */
function findResultById(
  blocks: ChatResponseBlock[],
  afterIndex: number,
  callId: string | undefined
): ChatResponseBlock | undefined {
  if (callId == null || callId === '') return undefined;
  const wantId = idStr(callId);
  for (let k = afterIndex + 1; k < blocks.length; k++) {
    const b = blocks[k];
    if (b?.type === MessageBlockType.TOOL_RESULT) {
      const res = parseToolResult(b);
      if (res && wantId !== '' && idStr(res.id) === wantId) return b;
    }
  }
  return undefined;
}

/**
 * Convert raw blocks into display segments: merge TEXT, merge THOUGHT, pair TOOL_CALL+TOOL_RESULT by id.
 */
export function blocksToSegments(blocks: ChatResponseBlock[]): Segment[] {
  const segments: Segment[] = [];
  let textBuffer = '';

  const flushText = () => {
    if (textBuffer) {
      segments.push({ kind: SegmentKind.TEXT, data: textBuffer });
      textBuffer = '';
    }
  };

  for (let i = 0; i < blocks.length; i++) {
    const block = blocks[i]!;
    switch (block.type) {
      case MessageBlockType.TEXT:
        textBuffer += block.data ?? '';
        break;

      case MessageBlockType.THOUGHT: {
        flushText();
        const data = block.data ?? '';
        const last = segments[segments.length - 1];
        if (last?.kind === SegmentKind.THOUGHT) {
          last.data += data;
        } else {
          segments.push({ kind: SegmentKind.THOUGHT, data });
        }
        break;
      }

      case MessageBlockType.TOOL_CALL: {
        flushText();
        const firstCall = parseToolCall(block);
        if (!firstCall) {
          i++;
          break;
        }
        const { endIndex: j, lastCall, parametersData } = mergeConsecutiveToolCalls(blocks, i, firstCall);
        const resultBlock =
          firstCall.id != null && firstCall.id !== ''
            ? findResultById(blocks, j, firstCall.id)
            : blocks[j + 1]?.type === MessageBlockType.TOOL_RESULT
              ? blocks[j + 1]
              : undefined;
        const resultPayload = resultBlock ? parseToolResult(resultBlock) : null;
        const paired = resultBlock && matchById(lastCall, resultPayload, idStr);

        if (paired && resultPayload) {
          segments.push({
            kind: SegmentKind.TOOL_RUN,
            toolName: lastCall.toolName,
            parametersData,
            responseData: resultPayload.result ?? '',
            responseError: resultPayload.error ?? false,
            pending: false,
          });
          i = j;
        } else {
          segments.push({
            kind: SegmentKind.TOOL_RUN,
            toolName: lastCall.toolName,
            parametersData,
            responseData: '',
            responseError: false,
            pending: true,
          });
          i = j;
          i++;
        }
        break;
      }

      case MessageBlockType.TOOL_RESULT: {
        flushText();
        const resultPayload = parseToolResult(block);
        const hasId = resultPayload?.id != null && resultPayload.id !== '';
        if (!hasId && resultPayload) {
          segments.push({
            kind: SegmentKind.TOOL_RUN,
            toolName: resultPayload.toolName ?? '',
            parametersData: '',
            responseData: resultPayload.result ?? '',
            responseError: resultPayload.error ?? false,
            pending: false,
          });
        }
        break;
      }

      default:
        if (block.data) textBuffer += block.data;
        break;
    }
  }
  flushText();
  return segments;
}
