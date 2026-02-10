import { useCallback, useMemo, useRef, useState } from 'react';
import { Infinity, ListTodo } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useMention } from '../../hooks/useMention';
import { MentionPopup } from './MentionPopup';
import { SlashCommandPopup } from './SlashCommandPopup';
import { SLASH_COMMANDS, type SlashCommandItem } from './slashCommands';
import { ChatInputToolbar } from './ChatInputToolbar';
import { parseMentionSegments, splitByMention, MENTION_PART_REGEX } from './mentionTypes';
import { AGENT_COLORS, type AgentType } from './agentTypes';
import { useAIAssistantContext } from './AIAssistantContext';

export function ChatInput() {
  const {
    input,
    setInput,
    onSend,
    onStop,
    isLoading,
    modelState,
    agentState,
    chatContextState,
    onCommand,
  } = useAIAssistantContext();
  const { model, setModel, modelOptions } = modelState;
  const { agent, setAgent } = agentState;
  const { setChatContext } = chatContextState;
  const modelNames = useMemo(() => modelOptions.map((m) => m.modelName), [modelOptions]);
  const { t } = useTranslation();
  const inputRef = useRef(input);
  inputRef.current = input;

  const [slashOpen, setSlashOpen] = useState(false);
  const [slashQuery, setSlashQuery] = useState('');
  const [slashHighlightedIndex, setSlashHighlightedIndex] = useState(0);
  const slashStateRef = useRef<{ start: number; query: string }>({ start: 0, query: '' });

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
  const inputSegments = useMemo(() => parseMentionSegments(input), [input]);

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const value = e.target.value;
      setInput(value);
      const cursorPos = e.target.selectionStart ?? value.length;
      const beforeCursor = value.slice(0, cursorPos);
      const lastAt = beforeCursor.lastIndexOf('@');
      const afterAt = lastAt !== -1 ? beforeCursor.slice(lastAt + 1) : '';
      const charBeforeAt = lastAt > 0 ? beforeCursor[lastAt - 1] : ' ';
      const atIsAtStartOrAfterSpace = lastAt === 0 || /\s/.test(charBeforeAt);
      const isValidMentionTrigger =
        lastAt !== -1 &&
        atIsAtStartOrAfterSpace &&
        (afterAt.length === 0 || !/[\s]/.test(afterAt));
      if (isValidMentionTrigger) {
        mention.openMention();
        setSlashOpen(false);
        return;
      }
      if (!value.includes('@') || (value.length > 0 && !value.trim().includes('@'))) {
        mention.closeMention();
      }

      // Slash command: "/" at start or after space, query = text after "/" until cursor (no space)
      const lastSlash = beforeCursor.lastIndexOf('/');
      const charBeforeSlash = lastSlash > 0 ? beforeCursor[lastSlash - 1] : ' ';
      const slashAtStartOrAfterSpace = lastSlash === 0 || /\s/.test(charBeforeSlash);
      const query = lastSlash !== -1 ? beforeCursor.slice(lastSlash + 1, cursorPos) : '';
      const queryHasSpace = /\s/.test(query);
      if (lastSlash !== -1 && slashAtStartOrAfterSpace && !queryHasSpace && cursorPos > lastSlash) {
        slashStateRef.current = { start: lastSlash, query };
        setSlashQuery(query);
        setSlashOpen(true);
        setSlashHighlightedIndex(0);
      } else {
        setSlashOpen(false);
      }
    },
    [setInput, mention.openMention, mention.closeMention]
  );

  const filteredSlashCommands = useMemo(() => {
    const q = slashQuery.toLowerCase().trim();
    return q
      ? SLASH_COMMANDS.filter((c) => c.slug.toLowerCase().startsWith(q))
      : SLASH_COMMANDS;
  }, [slashQuery]);

  const runSlashCommand = useCallback(
    (cmd: SlashCommandItem) => {
      const { start, query } = slashStateRef.current;
      const current = inputRef.current;
      setInput(current.slice(0, start) + current.slice(start + 1 + query.length));
      setSlashOpen(false);
      onCommand?.(cmd.id);
    },
    [setInput, onCommand]
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (slashOpen) {
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          setSlashHighlightedIndex((i) => (i + 1) % Math.max(1, filteredSlashCommands.length));
          return;
        }
        if (e.key === 'ArrowUp') {
          e.preventDefault();
          setSlashHighlightedIndex((i) =>
            i <= 0 ? Math.max(0, filteredSlashCommands.length - 1) : i - 1
          );
          return;
        }
        if (e.key === 'Enter') {
          e.preventDefault();
          const cmd = filteredSlashCommands[slashHighlightedIndex];
          if (cmd) runSlashCommand(cmd);
          return;
        }
        if (e.key === 'Escape') {
          e.preventDefault();
          setSlashOpen(false);
          return;
        }
      }

      if (mention.mentionOpen && mention.handleMentionKeyDown(e)) return;

      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        onSend();
        return;
      }
      if (e.key === 'Tab' && !e.shiftKey) {
        e.preventDefault();
        const nextIndex = (modelNames.indexOf(model) + 1) % Math.max(1, modelNames.length);
        setModel(modelNames[nextIndex] ?? model);
        return;
      }
      if (e.key === 'Tab' && e.shiftKey) {
        e.preventDefault();
        const agentsList: AgentType[] = ['Agent', 'Plan'];
        const nextIndex = (agentsList.indexOf(agent) + 1) % agentsList.length;
        setAgent(agentsList[nextIndex] ?? agent);
      }
    },
    [
      slashOpen,
      slashHighlightedIndex,
      filteredSlashCommands,
      runSlashCommand,
      mention.mentionOpen,
      mention.handleMentionKeyDown,
      onSend,
      model,
      modelNames,
      setModel,
      agent,
      setAgent,
    ]
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

        <SlashCommandPopup
          open={slashOpen}
          query={slashQuery}
          commands={SLASH_COMMANDS}
          highlightedIndex={Math.min(slashHighlightedIndex, Math.max(0, filteredSlashCommands.length - 1))}
          onSelect={runSlashCommand}
          onHighlight={setSlashHighlightedIndex}
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
          modelOptions={modelOptions}
          onSend={onSend}
          onStop={onStop}
          isLoading={isLoading}
          agents={agents}
          CurrentAgentIcon={CurrentAgentIcon}
        />
      </div>
    </div>
  );
}
