import type { ChatResponseBlock, ChatUserMention, DoneMetadata } from '../../../types/chat';
import type { MessageRole } from '../../../types/chat';
import type { TodoItem } from '../blocks';
import type { SubAgentProgressEvent } from '../blocks/subAgentTypes';

export enum SegmentKind {
  TEXT = 'TEXT',
  THOUGHT = 'THOUGHT',
  TOOL_RUN = 'TOOL_RUN',
  STATUS = 'STATUS',
}

export enum ToolExecutionState {
  STREAMING_ARGUMENTS = 'streaming_arguments',
  EXECUTING = 'executing',
  COMPLETE = 'complete',
}

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  userMentions?: ChatUserMention[];
  timestamp: Date;
  blocks?: ChatResponseBlock[];
  doneMetadata?: DoneMetadata;
  messageStatus?: 'NORMAL' | 'DELETED' | 'COMPRESSED' | 'COMPRESSION_SUMMARY';
  localKind?: 'compact-command' | 'compact-status' | 'compact-summary' | 'compact-result';
}

export type Segment =
  | { kind: SegmentKind.TEXT; data: string }
  | { kind: SegmentKind.THOUGHT; data: string }
  | { kind: SegmentKind.STATUS; statusKey: string }
  | {
      kind: SegmentKind.TOOL_RUN;
      toolName: string;
      parametersData: string;
      responseData: string;
      responseError?: boolean;
      pending?: boolean;
      executionState?: ToolExecutionState;
      toolCallId?: string;
      progressEvents?: SubAgentProgressEvent[];
      /** Parent tool call ID for nesting under a SubAgent orchestrator segment. */
      parentToolCallId?: string;
      /** Nested tool runs from SubAgent (getEnvironmentOverview, searchObjects, etc.). */
      nestedToolRuns?: Segment[];
      /** Identifies which parallel SubAgent task this segment belongs to. */
      subAgentTaskId?: string;
    };

/** One todo box to show in the list: todoId and latest items for that list. */
export interface TodoBoxSpec {
  todoId: string;
  items: TodoItem[];
}
