export type MentionLevel = 'connection' | 'database' | 'schema' | 'table';

export interface MentionItem {
  id: string;
  label: string;
  payload?: { connectionId?: number; databaseName?: string; schemaName?: string };
}

/** Regex to match a single @mention token (e.g. @conn or @conn/db/schema/table). */
export const MENTION_PART_REGEX = /^@[^\s]+$/;

/** Split text into parts: alternating plain text and @mention segments (same regex used in ChatInput and MessageList). */
export function splitByMention(text: string): string[] {
  return text.split(/(@[^\s]+)/g);
}

export type MentionSegment =
  | { type: 'mention'; text: string }
  | { type: 'plain'; text: string };

/**
 * Parse text into alternating plain and @mention segments.
 * Shared by ChatInput (input highlight) and MessageBubble (content display).
 */
export function parseMentionSegments(text: string): MentionSegment[] {
  const parts = splitByMention(text);
  return parts.map((segText) =>
    MENTION_PART_REGEX.test(segText)
      ? { type: 'mention' as const, text: segText }
      : { type: 'plain' as const, text: segText }
  );
}
