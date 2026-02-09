export interface ChatContext {
  connectionId?: number;
  databaseName?: string;
  schemaName?: string;
}

export interface ChatRequest {
  message: string;
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

/** Parsed from TOOL_CALL block.data */
export interface ToolCallData {
  toolName: string;
  arguments: string;
}

/** Parsed from TOOL_RESULT block.data */
export interface ToolResultData {
  toolName: string;
  result: string;
}

export interface ChatResponseBlock {
  type?: MessageBlockType;
  data?: string;
  conversationId?: number;
  done: boolean;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
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
  isLoading: boolean;
  stop: () => void;
  reload: () => Promise<void>;
  error?: Error;
}
