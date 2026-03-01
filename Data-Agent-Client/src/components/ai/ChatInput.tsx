import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { ChatInputToolbar } from './ChatInputToolbar';
import { AGENT_COLORS, AGENT_TYPES } from './agentTypes';
import { AGENT_CONFIG } from './agentConfig';
import { useAIAssistantContext } from './AIAssistantContext';
import { ChatInputArea } from './ChatInputArea';
import { ChatInputPopups } from './ChatInputPopups';
import { ChatInputQuestion } from './ChatInputQuestion';
import { ChatInputWriteConfirm } from './ChatInputWriteConfirm';
import { TodoListBlock } from './blocks/TodoListBlock';
import { useTodoInMessages } from './messageListLib/useTodoInMessages';
import { chatMessagesToMessages } from './MessageList';
import { mergeAssistantToolPairs } from './messageListLib/mergeMessages';
import { useSlashCommandLogic } from './hooks/useSlashCommandLogic';
import { useMentionLogic } from './hooks/useMentionLogic';
import { useKeyboardHandler } from './hooks/useKeyboardHandler';
import { useInputChangeHandler } from './hooks/useInputChangeHandler';

export function ChatInput() {
  const { t } = useTranslation();
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
    conversationId,
    messages,
  } = useAIAssistantContext();
  const { model, setModel, modelOptions } = modelState;
  const { agent, setAgent } = agentState;
  const { setChatContext } = chatContextState;
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

  return (
      <>
        {/* Todo list above input */}
        {showTodos && (
            <div className="px-2 pt-1.5 pb-1 theme-bg-panel border-t theme-border shrink-0">
              <TodoListBlock items={latestTodoItems} />
            </div>
        )}

        {/* Question form above input */}
        <ChatInputQuestion conversationId={conversationId} />

        {/* Write confirmation panel above input */}
        <ChatInputWriteConfirm conversationId={conversationId} />

        {/* Main input area */}
        <div className="p-2 theme-bg-panel border-t theme-border shrink-0">
          <div
              className={`rounded-lg border theme-border theme-bg-main relative transition-colors flex flex-col ${AGENT_COLORS[agent].focusBorder}`}
          >
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
