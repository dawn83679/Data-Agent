export interface ChatContext {
  connectionId?: number;
  databaseName?: string;
  schemaName?: string;
}

export interface ChatRequest {
  message: string;
  /** Model name for chat (e.g. qwen3-max, qwen3-max-thinking). */
  model?: string;
  /** Prompt language for backend system prompt routing (e.g. en, zh). */
  language?: string;
  /** Agent mode: "Agent", "Plan", or "Multi-Agent". */
  agentType?: string;
  conversationId?: number;
  connectionId?: number;
  databaseName?: string;
  schemaName?: string;
}

/** Aligned with backend MessageBlockEnum */
export const MessageBlockType = {
  TEXT: 'TEXT',
  THOUGHT: 'THOUGHT',
  TOOL_CALL: 'TOOL_CALL',
  TOOL_RESULT: 'TOOL_RESULT',
  STATUS: 'STATUS',
  TASK_PLAN: 'TASK_PLAN',
  TASK_START: 'TASK_START',
  TASK_STATUS: 'TASK_STATUS',
  TASK_TEXT: 'TASK_TEXT',
  TASK_RESULT: 'TASK_RESULT',
  TASK_APPROVAL: 'TASK_APPROVAL',
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
  /** True when arguments are still streaming (partial), false when complete, undefined for stored messages. */
  streaming?: boolean;
  runId?: number;
  taskId?: number;
  agentRole?: string;
}

/** Parsed from TOOL_RESULT block.data (id matches tool call for pairing). */
export interface ToolResultData {
  id?: string;
  toolName: string;
  result: string;
  /** True when tool execution failed (backend ToolExecution.hasFailed()). */
  error?: boolean;
  runId?: number;
  taskId?: number;
  agentRole?: string;
}

export interface MultiAgentTaskPlanItem {
  taskId?: number;
  agentRole?: string;
  title?: string;
  status?: string;
  goal?: string;
}

export interface MultiAgentTaskPlanData {
  runId?: number;
  title?: string;
  tasks?: MultiAgentTaskPlanItem[];
}

export interface MultiAgentTaskEventData {
  runId?: number;
  taskId?: number;
  agentRole?: string;
  title?: string;
  status?: string;
  summary?: string;
  details?: string;
  requiresApproval?: boolean;
}

export interface MultiAgentTaskTextData {
  runId?: number;
  taskId?: number;
  agentRole?: string;
  content?: string;
  streaming?: boolean;
}

export interface ChatResponseBlock {
  type?: MessageBlockType;
  data?: string;
  conversationId?: number;
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
  blocks?: ChatResponseBlock[];
  createdAt?: Date;
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
  submitMessage: (message: string, bodyOverrides?: Partial<ChatRequest>) => Promise<void>;
  isLoading: boolean;
  /** True when loading and no block received recently — used to show Planning indicator. */
  isWaiting: boolean;
  stop: () => void;
  reload: () => Promise<void>;
  error?: Error;
}
