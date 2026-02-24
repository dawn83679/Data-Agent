import { MentionPopup } from './MentionPopup';
import { SlashCommandPopup } from './SlashCommandPopup';
import { SLASH_COMMANDS, type SlashCommandItem } from './slashCommands';
import type { UseMentionReturn } from '../../hooks/useMention';
import { AGENT_COLORS, type AgentType } from './agentTypes';

interface ChatInputPopupsProps {
  agent: AgentType;
  mention: UseMentionReturn;
  slashOpen: boolean;
  slashQuery: string;
  slashHighlightedIndex: number;
  filteredSlashCommands: SlashCommandItem[];
  onSlashSelect: (cmd: SlashCommandItem) => void;
  onSlashHighlight: (index: number) => void;
}

/** Popup components for @mentions and /slash commands */
export function ChatInputPopups({
  agent,
  mention,
  slashOpen,
  slashQuery,
  slashHighlightedIndex,
  filteredSlashCommands,
  onSlashSelect,
  onSlashHighlight,
}: ChatInputPopupsProps) {
  return (
    <>
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
        onSelect={onSlashSelect}
        onHighlight={onSlashHighlight}
        highlightClassName={AGENT_COLORS[agent].popupHighlight}
      />
    </>
  );
}
