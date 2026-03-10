import type { ChatResponseBlock } from '../../../types/chat';
import type { MessageRole } from '../../../types/chat';
import type { TodoItem } from '../blocks';

export enum SegmentKind {
  TEXT = 'TEXT',
  THOUGHT = 'THOUGHT',
  TOOL_RUN = 'TOOL_RUN',
  STATUS = 'STATUS',
  SUB_AGENT = 'SUB_AGENT',
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
  timestamp: Date;
  blocks?: ChatResponseBlock[];
}

export interface SubAgentTextEntry {
  kind: 'text';
  data: string;
  streaming?: boolean;
}

export interface SubAgentToolEntry {
  kind: 'tool';
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError?: boolean;
  pending?: boolean;
  executionState?: ToolExecutionState;
  toolCallId?: string;
}

export interface SubAgentBlockModel {
  key: string;
  toolCallId?: string;
  runId?: number;
  taskId?: number;
  agentRole?: string;
  title?: string;
  status?: string;
  summary?: string;
  entries: Array<SubAgentTextEntry | SubAgentToolEntry>;
}

export type Segment =
  | { kind: SegmentKind.TEXT; data: string }
  | { kind: SegmentKind.THOUGHT; data: string }
  | { kind: SegmentKind.STATUS; statusKey: string }
  | { kind: SegmentKind.SUB_AGENT; block: SubAgentBlockModel }
  | {
      kind: SegmentKind.TOOL_RUN;
      toolName: string;
      parametersData: string;
      responseData: string;
      responseError?: boolean;
      pending?: boolean;
      executionState?: ToolExecutionState;
      toolCallId?: string;
    };

/** One todo box to show in the list: todoId and latest items for that list. */
export interface TodoBoxSpec {
  todoId: string;
  items: TodoItem[];
}
