import { Infinity, ListTodo } from 'lucide-react';
import type { AgentType } from './agentTypes';

export interface AgentOption {
  type: AgentType;
  icon: typeof Infinity | typeof ListTodo;
  i18nKey: string;
}

/** Agent configuration mapping type to icon and i18n key */
export const AGENT_CONFIG: Record<AgentType, Omit<AgentOption, 'type'>> = {
  Agent: {
    icon: Infinity,
    i18nKey: 'ai.agent',
  },
  Plan: {
    icon: ListTodo,
    i18nKey: 'ai.plan',
  },
};
