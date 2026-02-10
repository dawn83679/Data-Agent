export interface ChatContext {
  connectionId?: number;
  databaseName?: string;
  schemaName?: string;
}

export interface ChatRequest {
  message: string;
  /** Model name for chat (e.g. qwen3-max, qwen3-max-thinking). */
  model?: string;
  conversationId?: number;
  connectionId?: number;
  databaseName?: string;
  schemaName?: string;
}

/** 与后端 MessageBlockEnum 一致 */
export const MessageBlockType = {
  TEXT: 'TEXT',
  THOUGHT: 'THOUGHT',
  TOOL_CALL: 'TOOL_CALL',
  TOOL_RESULT: 'TOOL_RESULT',
} as const;

export type MessageBlockType = (typeof MessageBlockType)[keyof typeof MessageBlockType];

/** 仅 TEXT/THOUGHT 累加到 content */
export function isContentBlockType(type: MessageBlockType | undefined): boolean {
  return type === MessageBlockType.TEXT || type === MessageBlockType.THOUGHT;
}

/** Parsed from TOOL_CALL block.data (id from LangChain4j ToolExecutionRequest for merging streaming chunks). */
export interface ToolCallData {
  id?: string;
  toolName: string;
  arguments: string;
}

/** Parsed from TOOL_RESULT block.data (id matches tool call for pairing). */
export interface ToolResultData {
  id?: string;
  toolName: string;
  result: string;
  /** True when tool execution failed (backend ToolExecution.hasFailed()). */
  error?: boolean;
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
  /** Send a specific message (e.g. from queue) without using input state. */
  submitMessage: (message: string) => Promise<void>;
  /** Submit user answer to askUserQuestion tool and continue (streams into last assistant message). */
  submitToolAnswer: (toolCallId: string, answer: string) => Promise<void>;
  isLoading: boolean;
  stop: () => void;
  reload: () => Promise<void>;
  error?: Error;
}
