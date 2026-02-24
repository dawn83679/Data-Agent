import { MessageBlockType } from '../../../types/chat';
import type { ChatResponseBlock } from '../../../types/chat';
import type { ToolCallData } from '../../../types/chat';
import { parseToolCall, parseToolResult, idStr, matchById } from './blockParsing';
import type { Segment } from './types';
import { SegmentKind, ToolExecutionState } from './types';
import { ASK_USER_QUESTION_TOOL_NAME } from '../blocks/askUserQuestionTypes';

/**
 * Merge consecutive TOOL_CALL blocks with the same id (streaming chunks) by concatenating arguments.
 */
function mergeConsecutiveToolCalls(
  blocks: ChatResponseBlock[],
  startIndex: number,
  firstCall: ToolCallData
): { endIndex: number; lastCall: ToolCallData; parametersData: string; isStreaming: boolean } {
  let j = startIndex;
  let lastCall: ToolCallData = firstCall;
  const hasId = firstCall.id != null && firstCall.id !== '';
  const firstIdStr = idStr(firstCall.id);
  let isStreaming = firstCall.streaming === true;

  while (hasId && j + 1 < blocks.length && blocks[j + 1]?.type === MessageBlockType.TOOL_CALL) {
    const nextCall = parseToolCall(blocks[j + 1]!);
    if (!nextCall || idStr(nextCall.id) !== firstIdStr) break;
    j++;
    isStreaming = isStreaming || nextCall.streaming === true;
    lastCall = {
      ...nextCall,
      arguments: (lastCall.arguments ?? '') + (nextCall.arguments ?? ''),
    };
  }
  return { endIndex: j, lastCall, parametersData: lastCall.arguments ?? '', isStreaming };
}

/**
 * Find TOOL_RESULT block with the same id as the given call (search from after callEndIndex).
 * Returns both the block and its index.
 */
function findResultById(
  blocks: ChatResponseBlock[],
  afterIndex: number,
  callId: string | undefined
): { block: ChatResponseBlock; index: number } | undefined {
  if (callId == null || callId === '') return undefined;
  const wantId = idStr(callId);
  for (let k = afterIndex + 1; k < blocks.length; k++) {
    const b = blocks[k];
    if (b?.type === MessageBlockType.TOOL_RESULT) {
      const res = parseToolResult(b);
      if (res && wantId !== '' && idStr(res.id) === wantId) {
        return { block: b, index: k };
      }
    }
  }
  return undefined;
}

/**
 * Convert raw blocks into display segments: merge TEXT, merge THOUGHT, pair TOOL_CALL+TOOL_RESULT by id.
 * @param blocks - Raw blocks to convert
 * @param filterAskUserQuestion - When true, skip askUserQuestion tool calls (for history rendering)
 */
export function blocksToSegments(blocks: ChatResponseBlock[], filterAskUserQuestion: boolean = false): Segment[] {
  const segments: Segment[] = [];
  let textBuffer = '';
  const processedIndices = new Set<number>(); // Track processed blocks to avoid duplicates

  const flushText = () => {
    if (textBuffer) {
      segments.push({ kind: SegmentKind.TEXT, data: textBuffer });
      textBuffer = '';
    }
  };

  for (let i = 0; i < blocks.length; i++) {
    if (processedIndices.has(i)) continue; // Skip already processed blocks
    
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
          break;
        }
        // Skip askUserQuestion tool when filtering for history rendering
        if (filterAskUserQuestion && firstCall.toolName === ASK_USER_QUESTION_TOOL_NAME) {
          // Mark this block as processed and skip it
          processedIndices.add(i);
          break;
        }
        const { endIndex: j, lastCall, parametersData, isStreaming } = mergeConsecutiveToolCalls(blocks, i, firstCall);

        // Mark all merged TOOL_CALL blocks as processed
        for (let k = i; k <= j; k++) {
          processedIndices.add(k);
        }

        const resultInfo =
          firstCall.id != null && firstCall.id !== ''
            ? findResultById(blocks, j, firstCall.id)
            : (blocks[j + 1]?.type === MessageBlockType.TOOL_RESULT
              ? { block: blocks[j + 1]!, index: j + 1 }
              : undefined);

        const resultBlock = resultInfo?.block;
        const resultPayload = resultBlock ? parseToolResult(resultBlock) : null;
        const paired = resultBlock && matchById(lastCall, resultPayload, idStr);

        // Determine execution state
        let executionState: ToolExecutionState;
        if (isStreaming || lastCall.streaming === true) {
          executionState = ToolExecutionState.STREAMING_ARGUMENTS;
        } else if (!paired || !resultPayload) {
          executionState = ToolExecutionState.EXECUTING;
        } else {
          executionState = ToolExecutionState.COMPLETE;
        }

        if (paired && resultPayload) {
          segments.push({
            kind: SegmentKind.TOOL_RUN,
            toolName: lastCall.toolName,
            parametersData,
            responseData: resultPayload.result ?? '',
            responseError: resultPayload.error ?? false,
            pending: false,
            executionState,
            toolCallId: lastCall.id,
            serverName: lastCall.serverName ?? resultPayload.serverName,
          });
          // Mark the paired TOOL_RESULT as processed
          if (resultInfo) {
            processedIndices.add(resultInfo.index);
          }
        } else {
          segments.push({
            kind: SegmentKind.TOOL_RUN,
            toolName: lastCall.toolName,
            parametersData,
            responseData: '',
            responseError: false,
            pending: true,
            executionState,
            toolCallId: lastCall.id,
            serverName: lastCall.serverName,
          });
        }
        i = j; // Skip to the end of merged blocks
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
            toolCallId: resultPayload.id,
            serverName: resultPayload.serverName,
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
