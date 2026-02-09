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

export interface ChatResponseBlock {
  type: 'TEXT' | 'THOUGHT' | 'TOOL_CALL' | 'TOOL_RESULT';
  content?: string;
  toolName?: string;
  toolArguments?: string;
  toolResult?: string;
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
