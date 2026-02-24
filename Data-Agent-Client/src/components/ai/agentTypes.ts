/** List of available agent types */
export const AGENT_TYPES = ['Agent', 'Plan'] as const;

/** Agent type definition derived from AGENT_TYPES */
export type AgentType = (typeof AGENT_TYPES)[number];

/** Theme colors per Agent mode: icon, border, background, focus border, send button, mention popup highlight, mention text. */
export const AGENT_COLORS: Record<
  AgentType,
  {
    icon: string;
    bg: string;
    border: string;
    focusBorder: string;
    sendBtn: string;
    popupHighlight: string;
    mentionText: string;
  }
> = {
  Agent: {
    icon: 'text-violet-400',
    bg: 'bg-violet-500/20',
    border: 'border-violet-400/50',
    focusBorder: 'focus-within:border-violet-400/50',
    sendBtn: 'text-violet-400 hover:text-violet-300',
    popupHighlight: 'bg-violet-500 text-white',
    mentionText: 'text-violet-400',
  },
  Plan: {
    icon: 'text-amber-400',
    bg: 'bg-amber-500/20',
    border: 'border-amber-400/50',
    focusBorder: 'focus-within:border-amber-400/50',
    sendBtn: 'text-amber-400 hover:text-amber-300',
    popupHighlight: 'bg-amber-500 text-white',
    mentionText: 'text-amber-400',
  },
};
