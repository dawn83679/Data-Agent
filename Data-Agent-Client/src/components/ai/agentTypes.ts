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
    icon: 'text-[var(--accent-blue-subtle)]',
    bg: 'bg-[var(--bg-panel)]',
    border: 'border-[color:var(--border-color)]',
    focusBorder: 'focus-within:border-[color:var(--accent-blue)]',
    sendBtn: 'text-white bg-[color:var(--accent-blue)] hover:bg-[color:var(--accent-hover)]',
    popupHighlight: 'bg-[color:var(--accent-blue)] text-white',
    mentionText: 'text-[var(--accent-blue-subtle)]',
  },
  Plan: {
    icon: 'text-[var(--text-secondary)]',
    bg: 'bg-[var(--bg-panel)]',
    border: 'border-[color:var(--border-color)]',
    focusBorder: 'focus-within:border-[color:var(--accent-blue)]',
    sendBtn: 'text-white bg-[color:var(--accent-blue)] hover:bg-[color:var(--accent-hover)]',
    popupHighlight: 'bg-[color:var(--accent-blue)] text-white',
    mentionText: 'text-[var(--accent-blue-subtle)]',
  },
};
