import { useCallback } from 'react';
import { AGENT_TYPES, type AgentType } from '../agentTypes';
import type { SlashCommandItem } from '../slashCommands';
import type { UseMentionReturn } from '../../../hooks/useMention';

interface UseKeyboardHandlerOptions {
  slashOpen: boolean;
  slashHighlightedIndex: number;
  filteredSlashCommands: SlashCommandItem[];
  mention: UseMentionReturn;
  onSend: () => void;
  agent: AgentType;
  model: string;
  modelNames: string[];
  onSlashOpen: (open: boolean) => void;
  onSlashHighlight: (index: number) => void;
  onRunSlashCommand: (cmd: SlashCommandItem) => void;
  onSetAgent: (agent: AgentType) => void;
  onSetModel: (model: string) => void;
}

/** Manages all keyboard event handling in chat input */
export function useKeyboardHandler({
  slashOpen,
  slashHighlightedIndex,
  filteredSlashCommands,
  mention,
  onSend,
  agent,
  model,
  modelNames,
  onSlashOpen,
  onSlashHighlight,
  onRunSlashCommand,
  onSetAgent,
  onSetModel,
}: UseKeyboardHandlerOptions) {
  return useCallback(
    (e: React.KeyboardEvent) => {
      // Handle slash command navigation
      if (slashOpen) {
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          onSlashHighlight((slashHighlightedIndex + 1) % Math.max(1, filteredSlashCommands.length));
          return;
        }
        if (e.key === 'ArrowUp') {
          e.preventDefault();
          onSlashHighlight(
            slashHighlightedIndex <= 0
              ? Math.max(0, filteredSlashCommands.length - 1)
              : slashHighlightedIndex - 1
          );
          return;
        }
        if (e.key === 'Enter') {
          e.preventDefault();
          const cmd = filteredSlashCommands[slashHighlightedIndex];
          if (cmd) onRunSlashCommand(cmd);
          return;
        }
        if (e.key === 'Escape') {
          e.preventDefault();
          onSlashOpen(false);
          return;
        }
      }

      // Handle mention navigation
      if (mention.mentionOpen && mention.handleMentionKeyDown(e)) return;

      // Handle send message (Ctrl+Enter / Cmd+Enter)
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        onSend();
        return;
      }

      // Handle model selection (Tab)
      if (e.key === 'Tab' && !e.shiftKey) {
        e.preventDefault();
        const nextIndex = (modelNames.indexOf(model) + 1) % Math.max(1, modelNames.length);
        onSetModel(modelNames[nextIndex] ?? model);
        return;
      }

      // Handle agent selection (Shift+Tab)
      if (e.key === 'Tab' && e.shiftKey) {
        e.preventDefault();
        const nextIndex = (AGENT_TYPES.indexOf(agent) + 1) % AGENT_TYPES.length;
        onSetAgent(AGENT_TYPES[nextIndex] ?? agent);
      }
    },
    [
      slashOpen,
      slashHighlightedIndex,
      filteredSlashCommands,
      mention,
      onSend,
      agent,
      model,
      modelNames,
      onSlashOpen,
      onSlashHighlight,
      onRunSlashCommand,
      onSetAgent,
      onSetModel,
    ]
  );
}
