import { MessageBlockType } from '../../../types/chat';
import type {
  ChatResponseBlock,
  MultiAgentTaskEventData,
  MultiAgentTaskTextData,
  ToolCallData,
  ToolResultData,
} from '../../../types/chat';
import type { SubAgentBlockModel, SubAgentTextEntry, SubAgentToolEntry } from './types';
import { ToolExecutionState } from './types';

const DELEGATION_TOOL_NAME = 'delegate';

function isDelegationToolName(toolName: string | undefined): boolean {
  return toolName === DELEGATION_TOOL_NAME;
}

function parseToolCall(block: ChatResponseBlock): ToolCallData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolCallData;
    return parsed?.toolName ? parsed : null;
  } catch {
    return null;
  }
}

function parseToolResult(block: ChatResponseBlock): ToolResultData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolResultData;
    return parsed?.toolName ? parsed : null;
  } catch {
    return null;
  }
}

function parseTaskPayload(block: ChatResponseBlock): MultiAgentTaskEventData | null {
  if (!block.data) return null;
  try {
    return JSON.parse(block.data) as MultiAgentTaskEventData;
  } catch {
    return null;
  }
}

function parseTaskTextPayload(block: ChatResponseBlock): MultiAgentTaskTextData | null {
  if (!block.data) return null;
  try {
    return JSON.parse(block.data) as MultiAgentTaskTextData;
  } catch {
    return null;
  }
}

function idStr(value: unknown): string {
  return value == null ? '' : String(value);
}

function isResultError(hasFailed: boolean, result: string | undefined): boolean {
  if (hasFailed) return true;
  if (!result) return false;
  try {
    const parsed = JSON.parse(result) as Record<string, unknown>;
    return parsed?.success === false;
  } catch {
    return false;
  }
}

function mergeConsecutiveToolCalls(
  blocks: ChatResponseBlock[],
  startIndex: number,
  firstCall: ToolCallData
): { endIndex: number; lastCall: ToolCallData; parametersData: string; isStreaming: boolean } {
  let j = startIndex;
  let lastCall: ToolCallData = firstCall;
  const firstId = idStr(firstCall.id);
  let isStreaming = firstCall.streaming === true;

  while (firstId !== '' && j + 1 < blocks.length && blocks[j + 1]?.type === MessageBlockType.TOOL_CALL) {
    const nextCall = parseToolCall(blocks[j + 1]!);
    if (!nextCall || idStr(nextCall.id) !== firstId) break;
    j++;
    isStreaming = isStreaming || nextCall.streaming === true;
    lastCall = {
      ...nextCall,
      arguments: (lastCall.arguments ?? '') + (nextCall.arguments ?? ''),
    };
  }

  return { endIndex: j, lastCall, parametersData: lastCall.arguments ?? '', isStreaming };
}

function findResultById(
  blocks: ChatResponseBlock[],
  afterIndex: number,
  callId: string | undefined
): { block: ChatResponseBlock; index: number } | undefined {
  const wanted = idStr(callId);
  if (wanted === '') return undefined;
  for (let i = afterIndex + 1; i < blocks.length; i++) {
    const block = blocks[i];
    if (block?.type !== MessageBlockType.TOOL_RESULT) continue;
    const result = parseToolResult(block);
    if (result && idStr(result.id) === wanted) {
      return { block, index: i };
    }
  }
  return undefined;
}

function parseJsonObject(raw: string | undefined): Record<string, unknown> | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as unknown;
    return parsed && typeof parsed === 'object' ? parsed as Record<string, unknown> : null;
  } catch {
    return null;
  }
}

function parseDelegationTitle(parametersData: string | undefined): string | undefined {
  const parsed = parseJsonObject(parametersData);
  const title = parsed?.title;
  return typeof title === 'string' && title.trim() !== '' ? title : undefined;
}

function parseDelegationResult(resultData: string | undefined): Record<string, unknown> | null {
  const parsed = parseJsonObject(resultData);
  if (!parsed) return null;
  const nested = parsed.result;
  return nested && typeof nested === 'object' ? nested as Record<string, unknown> : null;
}

function roleLabelFromDelegateArgs(parametersData: string | undefined): string | undefined {
  const parsed = parseJsonObject(parametersData);
  const role = parsed?.role;
  return typeof role === 'string' && role.trim() !== '' ? role : undefined;
}

function roleTitle(role: string | undefined): string | undefined {
  if (!role) return undefined;
  return role
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function buildTaskEventText(blockType: MessageBlockType, payload: MultiAgentTaskEventData): string | null {
  const summary = payload.summary?.trim() ?? '';
  const details = payload.details?.trim() ?? '';
  switch (blockType) {
    case MessageBlockType.TASK_START:
    case MessageBlockType.TASK_STATUS:
      return null;
    case MessageBlockType.TASK_RESULT:
    case MessageBlockType.TASK_APPROVAL:
      if (summary && details && summary !== details) {
        return `${summary}\n\n${details}`;
      }
      return details || summary || null;
    default:
      return null;
  }
}

function appendTextEntry(target: SubAgentBlockModel, data: string | undefined, streaming = false) {
  const text = data ?? '';
  if (!text) return;
  const last = target.entries[target.entries.length - 1];
  if (last?.kind === 'text' && streaming && last.streaming) {
    last.data += text;
    return;
  }
  const entry: SubAgentTextEntry = { kind: 'text', data: text, streaming };
  target.entries.push(entry);
}

function appendToolEntry(target: SubAgentBlockModel, entry: SubAgentToolEntry) {
  if (entry.toolCallId) {
    for (let i = target.entries.length - 1; i >= 0; i--) {
      const existing = target.entries[i];
      if (existing?.kind !== 'tool' || existing.toolCallId !== entry.toolCallId) continue;

      existing.toolName = entry.toolName || existing.toolName;
      existing.parametersData = entry.parametersData || existing.parametersData;
      existing.responseData = entry.responseData || existing.responseData;
      existing.responseError = entry.responseError ?? existing.responseError;
      existing.pending = entry.pending;
      existing.executionState = entry.executionState ?? existing.executionState;
      return;
    }
  }

  target.entries.push(entry);
}

function createAnchor(toolCallId: string | undefined, parametersData: string | undefined): SubAgentBlockModel {
  const role = roleLabelFromDelegateArgs(parametersData);
  return {
    key: toolCallId || `sub-agent-${Math.random().toString(36).slice(2)}`,
    toolCallId,
    agentRole: role,
    title: parseDelegationTitle(parametersData) ?? roleTitle(role) ?? 'Calling Sub Agent',
    status: 'running',
    entries: [],
  };
}

function ensureAnchor(
  anchors: SubAgentBlockModel[],
  anchorByToolCallId: Map<string, SubAgentBlockModel>,
  toolCallId: string | undefined,
  parametersData: string | undefined
): SubAgentBlockModel {
  const key = toolCallId ?? `synthetic-${anchors.length}`;
  const existing = anchorByToolCallId.get(key);
  if (existing) return existing;
  const anchor = createAnchor(toolCallId, parametersData);
  anchor.key = key;
  anchors.push(anchor);
  anchorByToolCallId.set(key, anchor);
  return anchor;
}

function assignTaskToAnchor(
  anchor: SubAgentBlockModel,
  payload: MultiAgentTaskEventData | Record<string, unknown>
) {
  const taskId = payload.taskId;
  if (typeof taskId === 'number') {
    anchor.taskId = taskId;
  }
  const runId = payload.runId;
  if (typeof runId === 'number') {
    anchor.runId = runId;
  }
  const agentRole = payload.agentRole;
  if (typeof agentRole === 'string' && agentRole.trim() !== '') {
    anchor.agentRole = agentRole;
  }
  const title = payload.title;
  if (typeof title === 'string' && title.trim() !== '') {
    anchor.title = title;
  }
  const status = payload.status;
  if (typeof status === 'string' && status.trim() !== '') {
    anchor.status = status;
  }
  const summary = payload.summary;
  if (typeof summary === 'string' && summary.trim() !== '') {
    anchor.summary = summary;
  }
}

export function projectSubAgentBlocks(blocks: ChatResponseBlock[]): {
  anchors: SubAgentBlockModel[];
  anchorByToolCallId: Map<string, SubAgentBlockModel>;
} {
  const anchors: SubAgentBlockModel[] = [];
  const anchorByToolCallId = new Map<string, SubAgentBlockModel>();
  const anchorByTaskId = new Map<number, SubAgentBlockModel>();
  const pendingAnchors: SubAgentBlockModel[] = [];
  const processedResultIndices = new Set<number>();

  for (let i = 0; i < blocks.length; i++) {
    const block = blocks[i]!;

    if (block.type === MessageBlockType.TOOL_CALL) {
      const firstCall = parseToolCall(block);
      if (!firstCall) continue;

      const { endIndex, lastCall, parametersData, isStreaming } = mergeConsecutiveToolCalls(blocks, i, firstCall);
      const resultInfo = findResultById(blocks, endIndex, lastCall.id);
      const resultPayload = resultInfo?.block ? parseToolResult(resultInfo.block) : null;

      if (isDelegationToolName(lastCall.toolName) && (lastCall.taskId == null && resultPayload?.taskId == null)) {
        const anchor = ensureAnchor(anchors, anchorByToolCallId, lastCall.id, parametersData);
        anchor.status = isStreaming ? 'running' : (anchor.status ?? 'running');
        anchor.summary = anchor.summary ?? 'Delegated by orchestrator.';

        const nestedResult = parseDelegationResult(resultPayload?.result);
        if (nestedResult) {
          assignTaskToAnchor(anchor, nestedResult);
          // For history: show the sub-agent's report as a text entry
          const details = nestedResult.details;
          if (typeof details === 'string' && details.trim() !== '') {
            appendTextEntry(anchor, details, false);
          }
          if (typeof anchor.taskId === 'number') {
            anchorByTaskId.set(anchor.taskId, anchor);
            const pendingIndex = pendingAnchors.indexOf(anchor);
            if (pendingIndex >= 0) pendingAnchors.splice(pendingIndex, 1);
          }
        } else if (!pendingAnchors.includes(anchor)) {
          pendingAnchors.push(anchor);
        }

        if (resultInfo) {
          processedResultIndices.add(resultInfo.index);
        }

        i = endIndex;
        continue;
      }

      const taskId = lastCall.taskId ?? resultPayload?.taskId;
      if (typeof taskId === 'number') {
        const anchor = anchorByTaskId.get(taskId) ?? pendingAnchors[0];
        if (!anchor) {
          i = endIndex;
          continue;
        }
        if (anchor.taskId == null) {
          anchor.taskId = taskId;
          anchorByTaskId.set(taskId, anchor);
          pendingAnchors.shift();
        }

        let executionState: ToolExecutionState;
        if (resultPayload) {
          executionState = ToolExecutionState.COMPLETE;
        } else if (isStreaming || lastCall.streaming === true) {
          executionState = ToolExecutionState.STREAMING_ARGUMENTS;
        } else {
          executionState = ToolExecutionState.EXECUTING;
        }

        appendToolEntry(anchor, {
          kind: 'tool',
          toolName: lastCall.toolName,
          parametersData,
          responseData: resultPayload?.result ?? '',
          responseError: resultPayload
            ? isResultError(resultPayload.error ?? false, resultPayload.result)
            : false,
          pending: !resultPayload,
          executionState,
          toolCallId: lastCall.id,
        });

        if (resultInfo) {
          processedResultIndices.add(resultInfo.index);
        }
      }

      i = endIndex;
      continue;
    }

    if (block.type === MessageBlockType.TOOL_RESULT) {
      if (processedResultIndices.has(i)) continue;
      const resultPayload = parseToolResult(block);
      if (!resultPayload || typeof resultPayload.taskId !== 'number') continue;
      const anchor = anchorByTaskId.get(resultPayload.taskId) ?? pendingAnchors[0];
      if (!anchor) continue;
      if (anchor.taskId == null) {
        anchor.taskId = resultPayload.taskId;
        anchorByTaskId.set(resultPayload.taskId, anchor);
        pendingAnchors.shift();
      }
      appendToolEntry(anchor, {
        kind: 'tool',
        toolName: resultPayload.toolName,
        parametersData: '',
        responseData: resultPayload.result ?? '',
        responseError: isResultError(resultPayload.error ?? false, resultPayload.result),
        pending: false,
        executionState: ToolExecutionState.COMPLETE,
        toolCallId: resultPayload.id,
      });
      continue;
    }

    if (
      block.type === MessageBlockType.TASK_START ||
      block.type === MessageBlockType.TASK_STATUS ||
      block.type === MessageBlockType.TASK_RESULT ||
      block.type === MessageBlockType.TASK_APPROVAL
    ) {
      const payload = parseTaskPayload(block);
      if (!payload || typeof payload.taskId !== 'number') continue;
      let anchor = anchorByTaskId.get(payload.taskId);
      if (!anchor && pendingAnchors.length > 0) {
        anchor = pendingAnchors.shift()!;
        anchor.taskId = payload.taskId;
        anchorByTaskId.set(payload.taskId, anchor);
      }
      if (!anchor) continue;

      assignTaskToAnchor(anchor, payload);
      const eventText = buildTaskEventText(block.type, payload);
      if (eventText) {
        appendTextEntry(anchor, eventText, false);
      }
      continue;
    }

    if (block.type === MessageBlockType.TASK_TEXT) {
      const payload = parseTaskTextPayload(block);
      if (!payload || typeof payload.taskId !== 'number') continue;
      let anchor = anchorByTaskId.get(payload.taskId);
      if (!anchor && pendingAnchors.length > 0) {
        anchor = pendingAnchors.shift()!;
        anchor.taskId = payload.taskId;
        anchorByTaskId.set(payload.taskId, anchor);
      }
      if (!anchor) continue;
      appendTextEntry(anchor, payload.content, payload.streaming === true);
      continue;
    }
  }

  return { anchors, anchorByToolCallId };
}

export function isDelegationTool(toolName: string | undefined): boolean {
  return isDelegationToolName(toolName);
}
