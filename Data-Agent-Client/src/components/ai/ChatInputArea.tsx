import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { parseMentionSegments } from './mentionTypes';
import { AGENT_COLORS, type AgentType } from './agentTypes';

interface ChatInputAreaProps {
  input: string;
  agent: AgentType;
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  onKeyDown: (e: React.KeyboardEvent) => void;
}

/** Textarea input area with @mention highlighting mirror layer */
export function ChatInputArea({ input, agent, onChange, onKeyDown }: ChatInputAreaProps) {
  const { t } = useTranslation();
  const inputSegments = useMemo(() => parseMentionSegments(input), [input]);

  return (
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
        onChange={onChange}
        onKeyDown={onKeyDown}
        placeholder={t('ai.placeholder_mention')}
        className={`relative z-10 w-full h-24 bg-transparent text-xs p-3 focus:outline-none resize-none placeholder:text-muted-foreground/50 text-transparent min-h-0 ${agent === 'Agent' ? 'caret-violet-400' : 'caret-amber-400'}`}
      />
    </div>
  );
}
