import type { Message } from './types';

export const COMPACT_SUMMARY_PREFIX = '[CONVERSATION_SUMMARY]';

export function isCompactSummaryMessage(message: Message): boolean {
  return message.localKind === 'compact-summary'
    || message.messageStatus === 'COMPRESSION_SUMMARY';
}

export function stripCompactSummaryPrefix(content: string): string {
  const withoutPrefix = content.startsWith(COMPACT_SUMMARY_PREFIX)
    ? content.slice(COMPACT_SUMMARY_PREFIX.length).trimStart()
    : content.trim();
  const normalizedNewlines = withoutPrefix.includes('\\n') && !withoutPrefix.includes('\n')
    ? withoutPrefix.replace(/\\n/g, '\n')
    : withoutPrefix;
  const fencedMatch = normalizedNewlines.match(/^```(?:[\w-]+)?\s*\n([\s\S]*?)\n```$/);
  return (fencedMatch?.[1] ?? normalizedNewlines).trim();
}
