import type { ChatResponseBlock } from '../../../types/chat';
import type { MessageRole } from '../../../types/chat';
import type { TodoItem } from '../blocks';

export enum SegmentKind {
  TEXT = 'TEXT',
  THOUGHT = 'THOUGHT',
  TOOL_RUN = 'TOOL_RUN',
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

export type Segment =
  | { kind: SegmentKind.TEXT; data: string }
  | { kind: SegmentKind.THOUGHT; data: string }
  | {
      kind: SegmentKind.TOOL_RUN;
      toolName: string;
      parametersData: string;
      responseData: string;
      responseError?: boolean;
      pending?: boolean;
      executionState?: ToolExecutionState;
      toolCallId?: string;
      /** MCP server name (e.g., "chart-server") for server-specific rendering */
      serverName?: string;
    };

/** One todo box to show in the list: todoId and latest items for that list. */
export interface TodoBoxSpec {
  todoId: string;
  items: TodoItem[];
}
