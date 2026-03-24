import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChatInputToolbar } from './ChatInputToolbar';
import { AGENT_COLORS, AGENT_TYPES } from './agentTypes';
import { AGENT_CONFIG } from './agentConfig';
import { useAIAssistantContext } from './AIAssistantContext';
import { ChatInputArea } from './ChatInputArea';
import { ChatInputPopups } from './ChatInputPopups';
import { TodoListBlock } from './blocks/TodoListBlock';
import { useTodoInMessages } from './messageListLib/useTodoInMessages';
import { chatMessagesToMessages } from './MessageList';
import { mergeAssistantToolPairs } from './messageListLib/mergeMessages';
import { useSlashCommandLogic } from './hooks/useSlashCommandLogic';
import { useMentionLogic } from './hooks/useMentionLogic';
import { useKeyboardHandler } from './hooks/useKeyboardHandler';
import { useInputChangeHandler } from './hooks/useInputChangeHandler';
import { getModelMemoryThreshold } from '../../constants/models';
import { I18N_KEYS } from '../../constants/i18nKeys';

function formatDetailedTokenCount(value: number | null): string {
  if (value == null || !Number.isFinite(value)) {
    return '--';
  }
  return Math.round(value).toLocaleString();
}

export function ChatInput() {
  const { t } = useTranslation();
  const [isComposing, setIsComposing] = useState(false);
  const {
    input,
    setInput,
    onSend,
    onStop,
    isLoading,
    conversationTokenCount,
    modelState,
    agentState,
    chatContextState,
    mentionState,
    onCommand,
    messages,
  } = useAIAssistantContext();
  const { model, setModel, modelOptions } = modelState;
  const { agent, setAgent } = agentState;
  const { setChatContext } = chatContextState;
  const { setUserMentions } = mentionState;
  const modelNames = useMemo(() => modelOptions.map((m) => m.modelName), [modelOptions]);

  // Track todos for display above input
  const convertedMessages = chatMessagesToMessages(messages || []);
  const displayMessages = mergeAssistantToolPairs(convertedMessages);
  const { latestTodoItems, allTodoCompleted } = useTodoInMessages(displayMessages);
  const showTodos = latestTodoItems && latestTodoItems.length > 0 && !allTodoCompleted;

  // Initialize hooks
  const slashCmd = useSlashCommandLogic({
    input,
    setInput,
    onCommand,
  });

  const mentionLogic = useMentionLogic({
    input,
    setChatContext,
    setInput,
    onMentionResolved: (mention) => {
      setUserMentions((prev) => {
        const withoutSameToken = prev.filter((item) => item.token !== mention.token);
        return [...withoutSameToken, mention];
      });
    },
  });

  const handleInputChange = useInputChangeHandler({
    setInput,
    mention: mentionLogic.mention,
    onSlashOpen: slashCmd.setSlashOpen,
    onSlashQuery: slashCmd.setSlashQuery,
    onSlashHighlight: slashCmd.setSlashHighlightedIndex,
    slashStateRef: slashCmd.slashStateRef,
  });

  const handleKeyDown = useKeyboardHandler({
    isComposing,
    slashOpen: slashCmd.slashOpen,
    slashHighlightedIndex: slashCmd.slashHighlightedIndex,
    filteredSlashCommands: slashCmd.filteredSlashCommands,
    mention: mentionLogic.mention,
    onSend,
    agent,
    model,
    modelNames,
    onSlashOpen: slashCmd.setSlashOpen,
    onSlashHighlight: slashCmd.setSlashHighlightedIndex,
    onRunSlashCommand: slashCmd.runSlashCommand,
    onSetAgent: setAgent,
    onSetModel: setModel,
  });

  // Build agent options
  const agents = useMemo(
    () =>
      AGENT_TYPES.map((type) => ({
        type,
        icon: AGENT_CONFIG[type].icon,
        label: t(AGENT_CONFIG[type].i18nKey),
      })),
    [t]
  );
  const CurrentAgentIcon = AGENT_CONFIG[agent].icon;
  const contextUsage = useMemo(() => {
    const threshold = getModelMemoryThreshold(model);
    if (threshold == null || threshold <= 0 || conversationTokenCount == null || conversationTokenCount <= 0) {
      return null;
    }

    const ratio = conversationTokenCount / threshold;
    const displayPercent = ratio > 1
      ? '100+'
      : String(Math.max(1, Math.round(ratio * 100)));
    const progressPercent = Math.min(100, Math.max(1, ratio * 100));
    const tone =
      ratio >= 0.9 ? 'danger'
        : ratio >= 0.7 ? 'warning'
          : 'normal';
    const ringColor = tone === 'danger'
      ? '#fb7185'
      : tone === 'warning'
        ? '#f59e0b'
        : '#94a3b8';
    const textClassName = tone === 'danger'
      ? 'text-rose-300'
      : tone === 'warning'
        ? 'text-amber-300'
        : 'theme-text-secondary';

    return {
      label: `${displayPercent}%`,
      progressPercent,
      ringColor,
      textClassName,
      title: `${t(I18N_KEYS.AI.CONTEXT_USAGE_TITLE)}\n${t(I18N_KEYS.AI.CONTEXT_USAGE_TOOLTIP, {
        used: formatDetailedTokenCount(conversationTokenCount),
        limit: formatDetailedTokenCount(threshold),
      })}`,
    } as const;
  }, [conversationTokenCount, model, t]);

  return (
    <>
      {showTodos && (
        <div className="px-3 pt-2 pb-1 border-t theme-border shrink-0 bg-[color:var(--bg-panel)]">
          <TodoListBlock items={latestTodoItems} />
        </div>
      )}

      <div className="p-3 border-t theme-border shrink-0 bg-[color:var(--bg-panel)]">
        <div
          className={`rounded-xl border relative transition-colors flex flex-col gap-3 p-3 bg-[color:var(--bg-popup)] border-[color:var(--border-color)] ${AGENT_COLORS[agent].focusBorder}`}
        >
          {contextUsage && (
            <div className="absolute right-3 top-3 z-20">
              <div
                className="relative h-8 w-8 rounded-full"
                title={contextUsage.title}
                style={{
                  background: `conic-gradient(${contextUsage.ringColor} ${contextUsage.progressPercent}%, rgba(255,255,255,0.08) 0)`,
                }}
              >
                <div className="absolute inset-[2px] rounded-full bg-[color:var(--bg-popup)] border theme-border flex items-center justify-center">
                  <span className={`text-[8px] font-semibold leading-none ${contextUsage.textClassName}`}>
                    {contextUsage.label}
                  </span>
                </div>
              </div>
            </div>
          )}

          <ChatInputPopups
            agent={agent}
            mention={mentionLogic.mention}
            slashOpen={slashCmd.slashOpen}
            slashQuery={slashCmd.slashQuery}
            slashHighlightedIndex={slashCmd.slashHighlightedIndex}
            filteredSlashCommands={slashCmd.filteredSlashCommands}
            onSlashSelect={slashCmd.runSlashCommand}
            onSlashHighlight={slashCmd.setSlashHighlightedIndex}
          />

          <ChatInputArea
            input={input}
            agent={agent}
            onChange={handleInputChange}
            onCompositionStateChange={setIsComposing}
            onKeyDown={handleKeyDown}
          />

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
    </>
  );
}
