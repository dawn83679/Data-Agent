import type { ChatResponseBlock } from '../../../types/chat';
import type { MessageRole } from '../../../types/chat';

export enum SegmentKind {
  TEXT = 'TEXT',
  THOUGHT = 'THOUGHT',
  TOOL_RUN = 'TOOL_RUN',
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
    };
