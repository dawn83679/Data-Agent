import { useCallback, useMemo, useRef } from 'react';
import { Infinity, ListTodo } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useMention } from '../../hooks/useMention';
import { MentionPopup } from './MentionPopup';
import { ChatInputToolbar } from './ChatInputToolbar';
import { splitByMention, MENTION_PART_REGEX } from './mentionTypes';
import type { ChatContext } from '../../types/chat';

export type AgentType = 'Agent' | 'Plan';

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

interface ChatInputProps {
  input: string;
  setInput: (value: string) => void;
  onSend: () => void;
  agent: AgentType;
  setAgent: (agent: AgentType) => void;
  model: string;
  setModel: (model: string) => void;
  chatContext: ChatContext;
  setChatContext: React.Dispatch<React.SetStateAction<ChatContext>>;
}

const MODELS = ['Gemini 3 Pro', 'GPT-4o', 'Claude 3.5'];

export function ChatInput({
  input,
  setInput,
  onSend,
  agent,
  setAgent,
  model,
  setModel,
  setChatContext,
}: ChatInputProps) {
  const { t } = useTranslation();
  const inputRef = useRef(input);
  inputRef.current = input;

  /** Parse last-segment names from existing @mentions for duplicate detection. */
  const parseExistingShortNames = useCallback((text: string): Set<string> => {
    const parts = splitByMention(text).filter((p) => MENTION_PART_REGEX.test(p));
    return new Set(
      parts.map((m) => {
        const path = m.slice(1);
        return path.includes('/') ? (path.split('/').pop() ?? path) : path;
      })
    );
  }, []);

  const mention = useMention({
    setChatContext,
    onConfirmDisplay: ({ short: shortName, full: fullPath }) => {
      const prev = inputRef.current;
      const idx = prev.lastIndexOf('@');
      const beforeCurrentAt = idx >= 0 ? prev.slice(0, idx) : '';
      const existingShortNames = parseExistingShortNames(beforeCurrentAt);
      const hasDuplicate = existingShortNames.has(shortName);
      const displayText = hasDuplicate ? fullPath : `@${shortName}`;

      if (idx === -1) {
        setInput(prev + (prev && !prev.endsWith(' ') ? ' ' : '') + displayText);
      } else {
        setInput(prev.slice(0, idx) + displayText);
      }
    },
  });

  const agents = [
    { type: 'Agent' as const, icon: Infinity, label: t('ai.agent') },
    { type: 'Plan' as const, icon: ListTodo, label: t('ai.plan') },
  ];
  const CurrentAgentIcon = agents.find((a) => a.type === agent)?.icon ?? Infinity;

  /** Split input by @mention for inline highlight. */
  const inputSegments = useMemo(() => {
    const parts = splitByMention(input);
    return parts.map((text) =>
      MENTION_PART_REGEX.test(text) ? { type: 'mention' as const, text } : { type: 'plain' as const, text }
    );
  }, [input]);

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const value = e.target.value;
      setInput(value);
      const cursorPos = e.target.selectionStart ?? value.length;
      const beforeCursor = value.slice(0, cursorPos);
      const lastAt = beforeCursor.lastIndexOf('@');
      const afterAt = lastAt !== -1 ? beforeCursor.slice(lastAt + 1) : '';
      // Only treat as "typing mention" when @ is at start or after space; e.g. "@log_table@" does not trigger
      const charBeforeAt = lastAt > 0 ? beforeCursor[lastAt - 1] : ' ';
      const atIsAtStartOrAfterSpace = lastAt === 0 || /\s/.test(charBeforeAt);
      const isValidMentionTrigger =
        lastAt !== -1 &&
        atIsAtStartOrAfterSpace &&
        (afterAt.length === 0 || !/[\s]/.test(afterAt));
      if (isValidMentionTrigger) {
        mention.openMention();
        return;
      }
      if (!value.includes('@') || (value.length > 0 && !value.trim().includes('@'))) {
        mention.closeMention();
      }
    },
    [setInput, mention.openMention, mention.closeMention]
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (mention.mentionOpen && mention.handleMentionKeyDown(e)) return;

      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        onSend();
        return;
      }
      if (e.key === 'Tab' && !e.shiftKey) {
        e.preventDefault();
        const nextIndex = (MODELS.indexOf(model) + 1) % MODELS.length;
        setModel(MODELS[nextIndex] ?? model);
        return;
      }
      if (e.key === 'Tab' && e.shiftKey) {
        e.preventDefault();
        const agentsList: AgentType[] = ['Agent', 'Plan'];
        const nextIndex = (agentsList.indexOf(agent) + 1) % agentsList.length;
        setAgent(agentsList[nextIndex] ?? agent);
      }
    },
    [mention.mentionOpen, mention.handleMentionKeyDown, onSend, model, setModel, agent, setAgent]
  );

  return (
    <div className="p-2 theme-bg-panel border-t theme-border shrink-0">
      <div
        className={`rounded-lg border theme-border theme-bg-main relative transition-colors flex flex-col ${AGENT_COLORS[agent].focusBorder}`}
      >
        <MentionPopup
          open={mention.mentionOpen}
          level={mention.mentionLevel}
          levelLabel={mention.mentionLevelLabel}
          items={mention.mentionItems}
          loading={mention.mentionLoading}
          error={mention.mentionError}
          highlightedIndex={mention.mentionHighlightedIndex}
          onSelect={mention.handleMentionSelect}
          onHighlight={mention.setMentionHighlightedIndex}
          highlightClassName={AGENT_COLORS[agent].popupHighlight}
        />

        <div className="relative min-h-24">
          {/* Mirror layer: same layout as textarea, shows colored @mention text */}
          <div
            className="absolute inset-0 overflow-hidden pointer-events-none text-xs p-3 whitespace-pre-wrap break-words theme-text-primary"
            aria-hidden
          >
            {inputSegments.map((seg, i) =>
              seg.type === 'mention' ? (
                <span key={i} className={AGENT_COLORS[agent].mentionText}>
                  {seg.text}
                </span>
              ) : (
                <span key={i}>{seg.text}</span>
              )
            )}
          </div>
          <textarea
            data-ai-input
            value={input}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            placeholder={t('ai.placeholder_mention')}
            className={`relative z-10 w-full h-24 bg-transparent text-xs p-3 focus:outline-none resize-none placeholder:text-muted-foreground/50 text-transparent min-h-0 ${agent === 'Agent' ? 'caret-violet-400' : 'caret-amber-400'}`}
          />
        </div>

        <ChatInputToolbar
          agent={agent}
          setAgent={setAgent}
          model={model}
          setModel={setModel}
          onSend={onSend}
          agents={agents}
          CurrentAgentIcon={CurrentAgentIcon}
        />
      </div>
    </div>
  );
}
