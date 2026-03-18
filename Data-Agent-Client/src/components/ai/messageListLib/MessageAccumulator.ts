import { MessageBlockType } from '../../../types/chat';
import type { ChatResponseBlock, SubAgentEventData } from '../../../types/chat';
import { normalizeSubAgentType, type SubAgentProgressEvent } from '../blocks/subAgentTypes';
import { getToolType, ToolType } from '../blocks/toolTypes';
import { parseToolCall, parseToolResult, idStr } from './blockParsing';
import type { Segment } from './types';
import { SegmentKind, ToolExecutionState } from './types';

/**
 * Detect application-level tool errors.
 * LangChain4j only sets hasFailed=true on uncaught exceptions.
 * Tools that return structured error objects (AgentToolResult / AgentSqlResult with success:false)
 * come back as hasFailed=false, so we check the result JSON as well.
 */
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

interface ToolCallEntry {
  id: string;
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError: boolean;
  executionState: ToolExecutionState;
  parentToolCallId?: string;
  subAgentTaskId?: string;
  segmentIndex?: number;
}

/**
 * Incrementally processes ChatResponseBlocks into display Segments.
 *
 * Replaces the batch `blocksToSegments()` function with an O(1)-per-block
 * push model. Tool calls are paired with results via a Map lookup instead
 * of a linear scan, and sub-agent events are collected by parentToolCallId.
 */
export class MessageAccumulator {
  private textBuffer = '';
  private thoughtBuffer = '';

  /** O(1) tool pairing: toolCallId -> entry */
  private toolCallsById = new Map<string, ToolCallEntry>();

  /** O(1) sub-agent event collection: parentToolCallId -> events[] */
  private subAgentEvents = new Map<string, SubAgentProgressEvent[]>();

  /** O(1) nesting: parentToolCallId -> nested ToolCallEntry[] */
  private nestedByParent = new Map<string, ToolCallEntry[]>();

  /** Streaming merge: toolCallId -> accumulated arguments string */
  private streamingMerge = new Map<string, { toolName: string; segmentIndex?: number; args: string }>();

  /** Output segments */
  private segments: Segment[] = [];

  /** Track what the last segment type was for buffer flushing */
  private lastSegmentType: SegmentKind | null = null;

  /**
   * Process a single block and update internal segments.
   */
  pushBlock(block: ChatResponseBlock): void {
    const type = block.type;

    switch (type) {
      case MessageBlockType.TEXT:
        this.handleText(block);
        break;

      case MessageBlockType.THOUGHT:
        this.handleThought(block);
        break;

      case MessageBlockType.TOOL_CALL:
        this.handleToolCall(block);
        break;

      case MessageBlockType.TOOL_RESULT:
        this.handleToolResult(block);
        break;

      case MessageBlockType.SUB_AGENT_START:
      case MessageBlockType.SUB_AGENT_PROGRESS:
      case MessageBlockType.SUB_AGENT_COMPLETE:
      case MessageBlockType.SUB_AGENT_ERROR:
        this.handleSubAgentEvent(block);
        break;

      case MessageBlockType.STATUS:
        this.handleStatus(block);
        break;

      default:
        // Unknown block types: treat data as text (matches old behavior)
        if (block.data) {
          this.textBuffer += block.data;
          this.lastSegmentType = SegmentKind.TEXT;
        }
        break;
    }
  }

  /**
   * Return the current list of segments. The returned array is the internal
   * reference -- callers should treat it as read-only or shallow-copy if
   * needed for React state updates.
   */
  getSegments(): Segment[] {
    this.flushBuffers();

    return this.segments.map((segment) => {
      if (segment.kind !== SegmentKind.TOOL_RUN) return segment;
      if (getToolType(segment.toolName) !== ToolType.CALLING_SUB_AGENT) return segment;

      const parentId = idStr(segment.toolCallId);
      const progressEvents = parentId ? this.subAgentEvents.get(parentId) : undefined;
      const nestedToolRuns = parentId ? this.buildNestedSegments(parentId) : undefined;

      return {
        ...segment,
        progressEvents: progressEvents?.length ? progressEvents : undefined,
        nestedToolRuns: nestedToolRuns?.length ? nestedToolRuns : undefined,
      };
    });
  }

  // ── Private handlers ──────────────────────────────────────────────

  private flushText(): void {
    if (this.textBuffer) {
      // If the last committed segment is TEXT, append to it
      const last = this.segments[this.segments.length - 1];
      if (last && last.kind === SegmentKind.TEXT) {
        (last as { kind: SegmentKind.TEXT; data: string }).data += this.textBuffer;
      } else {
        this.segments.push({ kind: SegmentKind.TEXT, data: this.textBuffer });
      }
      this.textBuffer = '';
    }
  }

  private flushThought(): void {
    if (this.thoughtBuffer) {
      const last = this.segments[this.segments.length - 1];
      if (last && last.kind === SegmentKind.THOUGHT) {
        (last as { kind: SegmentKind.THOUGHT; data: string }).data += this.thoughtBuffer;
      } else {
        this.segments.push({ kind: SegmentKind.THOUGHT, data: this.thoughtBuffer });
      }
      this.thoughtBuffer = '';
    }
  }

  private flushBuffers(): void {
    this.flushText();
    this.flushThought();
  }

  private handleText(block: ChatResponseBlock): void {
    const data = block.data ?? '';
    this.textBuffer += data;
    // If we were accumulating thought, flush thought first
    if (this.lastSegmentType === SegmentKind.THOUGHT && this.thoughtBuffer) {
      this.flushThought();
    }
    this.lastSegmentType = SegmentKind.TEXT;
  }

  private handleThought(block: ChatResponseBlock): void {
    const data = block.data ?? '';
    // If we were accumulating text, flush text first
    if (this.lastSegmentType === SegmentKind.TEXT && this.textBuffer) {
      this.flushText();
    }
    this.thoughtBuffer += data;
    this.lastSegmentType = SegmentKind.THOUGHT;
  }

  private handleToolCall(block: ChatResponseBlock): void {
    const call = parseToolCall(block);
    if (!call) return;

    const callId = idStr(call.id);
    const isStreaming = call.streaming === true;

    // Check if this is a streaming continuation of an existing tool call
    if (callId && this.streamingMerge.has(callId)) {
      const existing = this.streamingMerge.get(callId)!;
      existing.args += call.arguments ?? '';

      // Update the segment in place
      const seg = existing.segmentIndex != null ? this.segments[existing.segmentIndex] : undefined;
      if (seg && seg.kind === SegmentKind.TOOL_RUN) {
        (seg as any).parametersData = existing.args;
        // Update execution state: still streaming if flag set
        if (!isStreaming) {
          (seg as any).executionState = ToolExecutionState.EXECUTING;
        }
      }

      // Update the tool call entry if it exists
      const entry = this.toolCallsById.get(callId);
      if (entry) {
        entry.parametersData = existing.args;
        if (!isStreaming) {
          entry.executionState = ToolExecutionState.EXECUTING;
        }
      }

      // If streaming is done, remove from merge buffer
      if (!isStreaming) {
        this.streamingMerge.delete(callId);
      }
      return;
    }

    const executionState = isStreaming
      ? ToolExecutionState.STREAMING_ARGUMENTS
      : ToolExecutionState.EXECUTING;
    const parametersData = call.arguments ?? '';
    const parentToolCallId = block.parentToolCallId ? idStr(block.parentToolCallId) : undefined;
    let segmentIndex: number | undefined;

    // Only top-level tools appear in the main message flow.
    if (!parentToolCallId) {
      this.flushBuffers();
      segmentIndex = this.segments.length;

      this.segments.push({
        kind: SegmentKind.TOOL_RUN,
        toolName: call.toolName,
        parametersData,
        responseData: '',
        responseError: false,
        pending: true,
        executionState,
        toolCallId: call.id,
      });
    }

    // Create the entry for O(1) lookup
    const entry: ToolCallEntry = {
      id: callId,
      toolName: call.toolName,
      parametersData,
      responseData: '',
      responseError: false,
      executionState,
      parentToolCallId,
      subAgentTaskId: block.subAgentTaskId,
      segmentIndex,
    };

    if (callId) {
      this.toolCallsById.set(callId, entry);
    }

    // Track streaming merge for incremental args
    if (isStreaming && callId) {
      this.streamingMerge.set(callId, {
        toolName: call.toolName,
        segmentIndex,
        args: parametersData,
      });
    }

    // Track nesting by parentToolCallId
    if (parentToolCallId) {
      const parentId = parentToolCallId;
      if (!this.nestedByParent.has(parentId)) {
        this.nestedByParent.set(parentId, []);
      }
      this.nestedByParent.get(parentId)!.push(entry);
    }

    if (!parentToolCallId) {
      this.lastSegmentType = SegmentKind.TOOL_RUN;
    }
  }

  private handleToolResult(block: ChatResponseBlock): void {
    const result = parseToolResult(block);
    if (!result) return;

    const resultId = idStr(result.id);
    const hasId = resultId !== '';

    // Try O(1) lookup by id
    if (hasId && this.toolCallsById.has(resultId)) {
      const entry = this.toolCallsById.get(resultId)!;
      const error = isResultError(result.error ?? false, result.result);

      // Update the entry
      entry.responseData = result.result ?? '';
      entry.responseError = error;
      entry.executionState = ToolExecutionState.COMPLETE;

      // Update the segment in place
      const seg = entry.segmentIndex != null ? this.segments[entry.segmentIndex] : undefined;
      if (seg && seg.kind === SegmentKind.TOOL_RUN) {
        (seg as any).responseData = result.result ?? '';
        (seg as any).responseError = error;
        (seg as any).pending = false;
        (seg as any).executionState = ToolExecutionState.COMPLETE;
      }
      return;
    }

    // Orphan result (no matching call) -- create a standalone segment
    // This matches old blocksToSegments behavior for TOOL_RESULT without matching call
    if (!hasId) {
      this.flushBuffers();
      this.segments.push({
        kind: SegmentKind.TOOL_RUN,
        toolName: result.toolName ?? '',
        parametersData: '',
        responseData: result.result ?? '',
        responseError: isResultError(result.error ?? false, result.result),
        pending: false,
        toolCallId: result.id,
      });
      this.lastSegmentType = SegmentKind.TOOL_RUN;
    }
    // If has id but no matching call, we just ignore it (it might arrive before call in rare cases)
    // The old code also effectively ignored such orphans when they had ids
  }

  private handleSubAgentEvent(block: ChatResponseBlock): void {
    let eventData: SubAgentEventData | null = null;
    if (block.data) {
      try {
        eventData = JSON.parse(block.data) as SubAgentEventData;
      } catch {
        // ignore parse errors
      }
    }

    const parentId = block.parentToolCallId;
    if (!parentId) return;

    const event: SubAgentProgressEvent = {
      phase: this.blockTypeToPhase(block.type),
      agentType: normalizeSubAgentType(eventData?.agentType) ?? 'explorer',
      message: eventData?.message,
      toolCount: eventData?.toolCount,
      toolCounts: eventData?.toolCounts,
      taskId: eventData?.taskId ?? block.subAgentTaskId,
      connectionId: eventData?.connectionId,
      timeoutSeconds: eventData?.timeoutSeconds,
      summaryText: eventData?.summaryText,
      resultJson: eventData?.resultJson,
    };

    // Collect events by parent
    if (!this.subAgentEvents.has(parentId)) {
      this.subAgentEvents.set(parentId, []);
    }
    this.subAgentEvents.get(parentId)!.push(event);
  }

  private handleStatus(block: ChatResponseBlock): void {
    this.flushBuffers();
    this.segments.push({ kind: SegmentKind.STATUS, statusKey: block.data ?? '' });
    this.lastSegmentType = SegmentKind.STATUS;
  }

  private buildNestedSegments(parentId: string): Segment[] | undefined {
    const entries = this.nestedByParent.get(parentId);
    if (!entries || entries.length === 0) return undefined;

    return entries.map((entry) => ({
      kind: SegmentKind.TOOL_RUN,
      toolName: entry.toolName,
      parametersData: entry.parametersData,
      responseData: entry.responseData,
      responseError: entry.responseError,
      pending: entry.executionState !== ToolExecutionState.COMPLETE,
      executionState: entry.executionState,
      toolCallId: entry.id,
      parentToolCallId: entry.parentToolCallId,
      subAgentTaskId: entry.subAgentTaskId,
    }));
  }

  private blockTypeToPhase(type: ChatResponseBlock['type']): SubAgentProgressEvent['phase'] {
    switch (type) {
      case MessageBlockType.SUB_AGENT_START:
        return 'start';
      case MessageBlockType.SUB_AGENT_COMPLETE:
        return 'complete';
      case MessageBlockType.SUB_AGENT_ERROR:
        return 'error';
      case MessageBlockType.SUB_AGENT_PROGRESS:
      default:
        return 'progress';
    }
  }
}
