import { useEffect, useRef, useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { cn } from '../../../lib/utils';
import type { SubAgentBlockModel, SubAgentTextEntry, SubAgentToolEntry } from '../messageListLib/types';
import { TextBlock } from './TextBlock';
import { ToolRunBlock } from './ToolRunBlock';
import { ToolRunExecuting } from './ToolRunExecuting';
import { ToolExecutionState } from '../messageListLib/types';

export interface SubAgentBlockProps {
  block: SubAgentBlockModel;
}

function statusLabel(status?: string): string {
  switch ((status ?? '').toLowerCase()) {
    case 'completed':
      return 'Completed';
    case 'running':
      return 'Running';
    case 'failed':
      return 'Failed';
    case 'waiting_approval':
      return 'Waiting approval';
    case 'skipped':
      return 'Skipped';
    default:
      return status || 'Pending';
  }
}

function statusTone(status?: string): string {
  switch ((status ?? '').toLowerCase()) {
    case 'completed':
      return 'text-emerald-700 dark:text-emerald-300';
    case 'failed':
      return 'text-red-700 dark:text-red-300';
    case 'waiting_approval':
      return 'text-amber-700 dark:text-amber-300';
    case 'running':
      return 'text-sky-700 dark:text-sky-300';
    default:
      return 'theme-text-secondary';
  }
}

function roleLabel(role?: string): string {
  return (role ?? 'sub_agent')
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function renderTextEntry(entry: SubAgentTextEntry, index: number) {
  return (
    <div key={`sub-agent-text-${index}`}>
      <TextBlock data={entry.data} />
    </div>
  );
}

function renderToolEntry(entry: SubAgentToolEntry, index: number) {
  const isIncomplete =
    entry.pending ||
    entry.executionState === ToolExecutionState.STREAMING_ARGUMENTS ||
    entry.executionState === ToolExecutionState.EXECUTING;

  if (isIncomplete) {
    return (
      <ToolRunExecuting
        key={`sub-agent-tool-${entry.toolCallId ?? index}`}
        toolName={entry.toolName}
        parametersData=""
      />
    );
  }

  return (
    <ToolRunBlock
      key={`sub-agent-tool-${entry.toolCallId ?? index}`}
      toolName={entry.toolName}
      parametersData={entry.parametersData}
      responseData={entry.responseData}
      responseError={entry.responseError}
      pending={entry.pending}
      executionState={entry.executionState}
      toolCallId={entry.toolCallId}
    />
  );
}

function useContentFingerprint(entries: SubAgentBlockModel['entries']): number {
  const last = entries[entries.length - 1];
  const lastLen = last?.kind === 'text' ? last.data.length : 0;
  return entries.length * 100000 + lastLen;
}

export function SubAgentBlock({ block }: SubAgentBlockProps) {
  const isRunning = (block.status ?? '').toLowerCase() === 'running';
  const [expanded, setExpanded] = useState(isRunning);
  const scrollRef = useRef<HTMLDivElement>(null);
  const contentFingerprint = useContentFingerprint(block.entries);

  useEffect(() => {
    if (isRunning) {
      setExpanded(true);
    } else {
      setExpanded(false);
    }
  }, [isRunning]);

  useEffect(() => {
    if (isRunning && expanded && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [contentFingerprint, isRunning, expanded]);

  return (
    <div className="mb-1.5 last:mb-0">
      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="flex w-full items-start gap-1.5 py-0.5 text-left text-[11px] opacity-70 theme-text-secondary hover:opacity-80 transition-opacity"
      >
        <span className="opacity-80">
          {expanded ? <ChevronDown className="w-3 h-3 shrink-0" aria-hidden /> : <ChevronRight className="w-3 h-3 shrink-0" aria-hidden />}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-3">
            <span className="truncate font-normal theme-text-primary">
              {block.title || roleLabel(block.agentRole)}
            </span>
            <span className={cn('shrink-0 uppercase tracking-wide', statusTone(block.status))}>
              {statusLabel(block.status)}
            </span>
          </div>
        </div>
      </button>

      {expanded && (
        <div ref={scrollRef} className="pl-4 pr-0 py-1 border-l border-current/20 max-h-[320px] overflow-y-auto">
          {block.entries.length > 0 && (
            <div className="flex flex-col gap-1">
              {block.entries.map((entry, index) => (
                entry.kind === 'text' ? renderTextEntry(entry, index) : renderToolEntry(entry, index)
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
