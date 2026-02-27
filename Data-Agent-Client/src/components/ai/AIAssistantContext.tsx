import { createContext, useContext, type ReactNode } from 'react';
import type { ChatContext } from '../../types/chat';
import type { ModelOption } from '../../types/ai';
import type { AgentType } from './agentTypes';

export interface ModelState {
  model: string;
  setModel: (model: string) => void;
  modelOptions: ModelOption[];
}

export interface AgentState {
  agent: AgentType;
  setAgent: (agent: AgentType) => void;
}

export interface ChatContextState {
  chatContext: ChatContext;
  setChatContext: React.Dispatch<React.SetStateAction<ChatContext>>;
}

export interface AIAssistantContextValue {
  input: string;
  setInput: (value: string) => void;
  onSend: () => void;
  onStop?: () => void;
  /** Send a message as the user (e.g. answer to askUserQuestion); uses current conversationId. */
  submitMessage: (message: string) => Promise<void>;
  isLoading: boolean;
  /** Current conversation ID (null for new conversations) */
  conversationId: number | null;
  modelState: ModelState;
  agentState: AgentState;
  chatContextState: ChatContextState;
  onCommand?: (commandId: string) => void;
}

const AIAssistantContext = createContext<AIAssistantContextValue | null>(null);

export function AIAssistantProvider({
  value,
  children,
}: {
  value: AIAssistantContextValue;
  children: ReactNode;
}) {
  return (
    <AIAssistantContext.Provider value={value}>{children}</AIAssistantContext.Provider>
  );
}

export function useAIAssistantContext(): AIAssistantContextValue {
  const ctx = useContext(AIAssistantContext);
  if (!ctx) {
    // Development error - this indicates incorrect component tree structure
    throw new Error('useAIAssistantContext must be used within AIAssistantProvider');
  }
  return ctx;
}
