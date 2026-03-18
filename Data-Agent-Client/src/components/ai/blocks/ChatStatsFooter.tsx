import { Wrench, Zap } from 'lucide-react';
import type { DoneMetadata } from '../../../types/chat';

export interface ChatStatsFooterProps {
  metadata: DoneMetadata;
}

function formatTokens(n: number): string {
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`;
  return String(n);
}

export function ChatStatsFooter({ metadata }: ChatStatsFooterProps) {
  const { toolCount, toolCounts, totalTokens, outputTokens } = metadata;

  const hasTools = toolCount != null && toolCount > 0;
  const hasTokens = totalTokens != null && totalTokens > 0;
  if (!hasTools && !hasTokens) return null;

  const toolDetails = toolCounts
    ? Object.entries(toolCounts)
        .sort(([, a], [, b]) => b - a)
        .map(([name, count]) => `${name} x${count}`)
        .join(', ')
    : '';

  return (
    <div className="mt-2 pt-1.5 border-t theme-border flex flex-wrap items-center gap-x-3 gap-y-0.5 text-[10px] theme-text-secondary opacity-60">
      {hasTools && (
        <span className="flex items-center gap-1" title={toolDetails}>
          <Wrench className="w-3 h-3" />
          <span>{toolCount} tool{toolCount !== 1 ? 's' : ''} used</span>
          {toolDetails && (
            <span className="hidden sm:inline">({toolDetails})</span>
          )}
        </span>
      )}
      {hasTokens && (
        <span className="flex items-center gap-1">
          <Zap className="w-3 h-3" />
          <span>
            {formatTokens(totalTokens!)} tokens
            {outputTokens != null && outputTokens > 0 && (
              <> (output: {formatTokens(outputTokens)})</>
            )}
          </span>
        </span>
      )}
    </div>
  );
}
