export interface ChatContext {
  connectionId?: number;
  catalogName?: string;
  schemaName?: string;
}

export interface ChatUserMention {
  token: string;
  objectType: string;
  connectionId?: number;
  connectionName: string;
  catalogName?: string;
  schemaName?: string;
  objectName: string;
}

export interface ChatRequest {
  message: string;
  /** Model name for chat, e.g. qwen3.6-max-preview or qwen3-max-2026-01-23. */
  model?: string;
  /** Prompt language for backend system prompt routing (e.g. en, zh). */
  language?: string;
  /** Agent mode: "Agent" or "Plan". */
  agentType?: string;
  conversationId?: number;
  connectionId?: number;
  catalogName?: string;
  schemaName?: string;
  userMentions?: ChatUserMention[];
  /**
   * Mirrors workspace headers for /api/chat/stream when fetch cannot rely on custom headers.
   * Server validates org membership.
   */
  clientWorkspaceType?: 'PERSONAL' | 'ORGANIZATION';
  clientOrgId?: number;
}

export interface SubmitMessageOptions {
  /** Send the message to the backend without inserting a visible user bubble locally. */
  hideUserMessage?: boolean;
}

/** Aligned with backend MessageBlockEnum */
export const MessageBlockType = {
  TEXT: 'TEXT',
  THOUGHT: 'THOUGHT',
  TOOL_CALL: 'TOOL_CALL',
  TOOL_RESULT: 'TOOL_RESULT',
  SUB_AGENT_START: 'SUB_AGENT_START',
  SUB_AGENT_PROGRESS: 'SUB_AGENT_PROGRESS',
  SUB_AGENT_COMPLETE: 'SUB_AGENT_COMPLETE',
  SUB_AGENT_ERROR: 'SUB_AGENT_ERROR',
  STATUS: 'STATUS',
} as const;

export type MessageBlockType = (typeof MessageBlockType)[keyof typeof MessageBlockType];

/** Only TEXT and THOUGHT blocks are accumulated into content */
export function isContentBlockType(type: MessageBlockType | undefined): boolean {
  return type === MessageBlockType.TEXT || type === MessageBlockType.THOUGHT;
}

/** Parsed from TOOL_CALL block.data (id from LangChain4j ToolExecutionRequest for merging streaming chunks). */
export interface ToolCallData {
  id?: string;
  toolName: string;
  arguments: string;
  /** Optional model-authored UI label for this tool step. Never trusted as business input. */
  description?: string;
  /** Epoch milliseconds when backend first emitted this tool call. */
  startedAt?: number;
  /** True when arguments are still streaming (partial), false when complete, undefined for stored messages. */
  streaming?: boolean;
}

/** Parsed from TOOL_RESULT block.data (id matches tool call for pairing). */
export interface ToolResultData {
  id?: string;
  toolName: string;
  result: string;
  /** Optional model-authored UI label copied from the paired tool call when available. */
  description?: string;
  /** Epoch milliseconds when backend first emitted the paired tool call. */
  startedAt?: number;
  /** Epoch milliseconds when backend emitted this tool result. */
  finishedAt?: number;
  /** True when tool execution failed (backend ToolExecution.hasFailed()). */
  error?: boolean;
}

export interface ChatResponseBlock {
  type?: MessageBlockType;
  data?: string;
  conversationId?: number;
  parentToolCallId?: string;
  subAgentTaskId?: string;
  done: boolean;
}

export enum MessageRole {
  USER = 'user',
  ASSISTANT = 'assistant',
}

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  userMentions?: ChatUserMention[];
  blocks?: ChatResponseBlock[];
  doneMetadata?: DoneMetadata;
  messageStatus?: 'NORMAL' | 'DELETED' | 'COMPRESSED' | 'COMPRESSION_SUMMARY';
  /** Frontend-only marker for ephemeral UI messages that should not be persisted. */
  localKind?: 'compact-command' | 'compact-status' | 'compact-result' | 'hidden-user-boundary';
  createdAt?: Date;
}

export interface DoneMetadata {
  toolCount?: number;
  toolCounts?: Record<string, number>;
  totalTokens?: number;
  outputTokens?: number;
  memoryCompressed?: boolean;
  tokenCountBefore?: number;
  tokenCountAfter?: number;
  compressedMessageCount?: number;
  keptRecentCount?: number;
  compressionOutputTokens?: number;
  compressionTotalTokens?: number;
}

export interface SubAgentEventData {
  agentType?: string;
  message?: string;
  toolCount?: number;
  toolCounts?: Record<string, number>;
  taskId?: string;
  connectionId?: number;
  timeoutSeconds?: number;
  summaryText?: string;
  resultJson?: string;
}

export interface UseChatOptions {
  api?: string;
  id?: string;
  initialMessages?: ChatMessage[];
  onResponse?: (response: Response) => void;
  onFinish?: (message: ChatMessage) => void;
  onError?: (error: Error) => void;
  onConversationId?: (id: number) => void;
  body?: Record<string, unknown>;
}

export interface UseChatReturn {
  messages: ChatMessage[];
  setMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>;
  input: string;
  setInput: (value: string) => void;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  handleSubmit: (e: React.FormEvent) => Promise<void>;
  /** Send a specific message (e.g. from queue) without using input state.
   *  Optional bodyOverrides lets callers override body fields (e.g. agentType) for this request only. */
  submitMessage: (
    message: string,
    bodyOverrides?: Partial<ChatRequest>,
    submitOptions?: SubmitMessageOptions,
  ) => Promise<void>;
  isLoading: boolean;
  /** True when loading and no block received recently — used to show Planning indicator. */
  isWaiting: boolean;
  stop: () => void;
  reload: () => Promise<void>;
  error?: Error;
}
