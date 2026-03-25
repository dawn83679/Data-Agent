import type { ChatUserMention } from '../../types/chat';

export type MentionObjectType =
  | 'TABLE'
  | 'VIEW'
  | 'FUNCTION'
  | 'PROCEDURE'
  | 'TRIGGER'
  | 'COLUMN'
  | 'INDEX'
  | 'KEY';

export interface MentionPayload {
  connectionId?: number;
  connectionName?: string;
  catalogName?: string;
  schemaName?: string;
  objectName?: string;
  objectType?: MentionObjectType;
}

export type MentionLevel = 'connection' | 'database' | 'schema' | 'object';

export interface MentionItem {
  id: string;
  label: string;
  detail?: string;
  payload?: MentionPayload;
}

/** Regex to match a single @mention token (e.g. @conn or @conn/db/schema/table). */
export const MENTION_PART_REGEX = /^@[^\s]+$/;

const MENTION_PREFIX_BOUNDARY = /[\s([{"'`]/;
const MENTION_SUFFIX_BOUNDARY = /[\s.,!?;:)\]}"'`]/;

/** Split text into parts: alternating plain text and @mention segments (same regex used in ChatInput and MessageList). */
export function splitByMention(text: string): string[] {
  return text.split(/(@[^\s]+)/g);
}

function hasMentionPrefixBoundary(text: string, start: number): boolean {
  if (start === 0) return true;
  return MENTION_PREFIX_BOUNDARY.test(text[start - 1] ?? '');
}

function hasMentionSuffixBoundary(text: string, end: number): boolean {
  if (end >= text.length) return true;
  return MENTION_SUFFIX_BOUNDARY.test(text[end] ?? '');
}

/**
 * Finds which known mention tokens are still present in the current text.
 * Matches exact tokens, prefers longer tokens first to avoid prefix collisions,
 * and tolerates common trailing punctuation such as "." or ",".
 */
export function findMentionTokens(text: string, candidateTokens: string[]): string[] {
  const tokens = Array.from(new Set(candidateTokens.filter(Boolean))).sort((a, b) => b.length - a.length);
  const matches: Array<{ token: string; start: number; end: number }> = [];

  for (const token of tokens) {
    let searchFrom = 0;
    while (searchFrom < text.length) {
      const start = text.indexOf(token, searchFrom);
      if (start === -1) break;

      const end = start + token.length;
      const overlaps = matches.some((match) => start < match.end && end > match.start);
      if (!overlaps && hasMentionPrefixBoundary(text, start) && hasMentionSuffixBoundary(text, end)) {
        matches.push({ token, start, end });
      }

      searchFrom = start + token.length;
    }
  }

  return matches
    .sort((a, b) => a.start - b.start)
    .map((match) => match.token)
    .filter((token, index, list) => list.indexOf(token) === index);
}

export function buildMentionFullPath(
  mention: Pick<ChatUserMention, 'token' | 'connectionName' | 'catalogName' | 'schemaName' | 'objectName'>
): string {
  const pathSegments = [
    mention.connectionName,
    mention.catalogName,
    mention.schemaName,
    mention.objectName,
  ].filter((segment): segment is string => Boolean(segment && segment.trim()));

  if (pathSegments.length === 0) {
    return mention.token;
  }
  return `@${pathSegments.join('/')}`;
}

export function expandMentionTokensForCopy(text: string, mentions: ChatUserMention[]): string {
  if (text === '' || mentions.length === 0) {
    return text;
  }

  const mentionMap = new Map(
    mentions
      .filter((mention) => mention.token)
      .map((mention) => [mention.token, buildMentionFullPath(mention)])
  );
  const tokens = Array.from(mentionMap.keys()).sort((a, b) => b.length - a.length);
  const matches: Array<{ token: string; start: number; end: number }> = [];

  for (const token of tokens) {
    let searchFrom = 0;
    while (searchFrom < text.length) {
      const start = text.indexOf(token, searchFrom);
      if (start === -1) break;

      const end = start + token.length;
      const overlaps = matches.some((match) => start < match.end && end > match.start);
      if (!overlaps && hasMentionPrefixBoundary(text, start) && hasMentionSuffixBoundary(text, end)) {
        matches.push({ token, start, end });
      }
      searchFrom = start + token.length;
    }
  }

  if (matches.length === 0) {
    return text;
  }

  const orderedMatches = matches.sort((a, b) => a.start - b.start);
  let cursor = 0;
  let output = '';
  for (const match of orderedMatches) {
    output += text.slice(cursor, match.start);
    output += mentionMap.get(match.token) ?? match.token;
    cursor = match.end;
  }
  output += text.slice(cursor);
  return output;
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
