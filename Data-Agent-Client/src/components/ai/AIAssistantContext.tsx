import { createContext, useContext, type ReactNode } from 'react';
import type { ChatContext, ChatRequest, ChatUserMention } from '../../types/chat';
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

export interface MentionState {
  userMentions: ChatUserMention[];
  setUserMentions: React.Dispatch<React.SetStateAction<ChatUserMention[]>>;
}

export interface AIAssistantContextValue {
  input: string;
  setInput: (value: string) => void;
  onSend: () => void;
  onStop?: () => void;
  /** Send a message as the user (e.g. answer to askUserQuestion); uses current conversationId.
   *  Optional bodyOverrides lets callers override body fields (e.g. agentType) for this request only. */
  submitMessage: (message: string, bodyOverrides?: Partial<ChatRequest>) => Promise<void>;
  /** Queue a message to be sent right after current streaming ends. */
  enqueueMessage?: (message: string) => void;
  isLoading: boolean;
  /** Current conversation ID (null for new conversations) */
  conversationId: number | null;
  /** Latest known tokenCount for the active conversation. */
  conversationTokenCount: number | null;
  modelState: ModelState;
  agentState: AgentState;
  chatContextState: ChatContextState;
  mentionState: MentionState;
  onCommand?: (commandId: string) => void;
  /** Raw messages for todo tracking */
  messages?: any[];
  /** Tab ID of the latest (most recent) plan in the conversation; null if no plans exist.
   *  Used by PlanCompleteHandler to only auto-open the latest plan on history load. */
  latestPlanTabId?: string | null;
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
