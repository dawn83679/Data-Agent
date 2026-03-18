import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Braces, CheckCircle, Database, Loader2, XCircle } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { cn } from '../../lib/utils';
import { SUB_AGENT_LABELS } from '../../constants/chat';
import type { SubAgentConsoleTabMetadata } from '../../types/tab';
import { markdownRemarkPlugins, useMarkdownComponents } from '../ai/blocks';
import { SegmentKind } from '../ai/messageListLib/types';
import { connectionService } from '../../services/connection.service';
import { QUERY_KEY_CONNECTIONS } from '../../constants/explorer';
import { ExplorerResultPreview, PlannerResultPreview, Section, ToolCalls } from './SubAgentConsoleSections';
import { getInvocationStatusText, inferConnectionId, parseExplorerResult, parsePlannerResult } from './subAgentConsoleParsers';

export interface SubAgentConsoleProps {
  tabId: string;
  metadata: SubAgentConsoleTabMetadata;
}

function getStatusIcon(status: 'pending' | 'running' | 'complete' | 'error', className?: string) {
  if (status === 'complete') return <CheckCircle className={cn('w-4 h-4 text-green-500', className)} />;
  if (status === 'error') return <XCircle className={cn('w-4 h-4 text-red-500', className)} />;
  return <Loader2 className={cn('w-4 h-4 animate-spin theme-text-secondary', className)} />;
}

export function SubAgentConsole({ metadata }: SubAgentConsoleProps) {
  const markdownComponents = useMarkdownComponents();
  const invocation = metadata.invocations[0];
  const agentType = invocation?.agentType ?? metadata.agentType;
  const isExplorer = agentType === 'explorer';
  const connectionId = inferConnectionId(metadata);
  const { data: connections = [] } = useQuery({
    queryKey: QUERY_KEY_CONNECTIONS,
    queryFn: () => connectionService.getConnections(),
    staleTime: 5 * 60 * 1000,
  });
  const connectionName = useMemo(
    () => (connectionId != null ? connections.find((connection) => connection.id === connectionId)?.name : undefined),
    [connectionId, connections]
  );
  const AgentIcon = isExplorer ? Database : Braces;
  const accentColor = isExplorer ? 'text-cyan-600 dark:text-cyan-400' : 'text-purple-600 dark:text-purple-400';
  const title = invocation?.taskLabel ?? SUB_AGENT_LABELS[agentType] ?? agentType;
  const displayTitle = isExplorer && connectionName
    ? `${SUB_AGENT_LABELS[agentType] ?? agentType} ${connectionName}`
    : title;
  const status = invocation?.status ?? metadata.status;
  const statusText = invocation ? getInvocationStatusText(invocation) : 'Starting Agent...';
  const inlineStatusText = status === 'complete' ? null : statusText;
  const toolRunCount = invocation?.nestedToolRuns?.filter((segment) => segment.kind === SegmentKind.TOOL_RUN).length ?? 0;
  const shouldShowToolCalls = status === 'running' || toolRunCount > 0;
  const resultJson = invocation?.resultJson ?? metadata.resultJson;
  const explorerResult = isExplorer ? parseExplorerResult(resultJson) : null;
  const plannerResult = isExplorer ? null : parsePlannerResult(resultJson);
  const errorMessage = invocation?.errorMessage ?? explorerResult?.errorMessage;
  const instruction = invocation?.params.userQuestion ?? metadata.params?.userQuestion;
  const resultSummary = invocation?.resultSummary
    ?? metadata.summary
    ?? (isExplorer ? explorerResult?.summaryText : plannerResult?.summaryText);

  return (
    <div className="flex-1 overflow-auto theme-bg-main p-6">
      <div className="max-w-4xl mx-auto flex flex-col gap-4">
        <Section title="Overview">
          <div className="flex items-start gap-3">
            <AgentIcon className={cn('w-5 h-5 mt-0.5', accentColor)} />
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 px-3">
                <span className="text-[15px] font-semibold theme-text-primary">{displayTitle}</span>
                {inlineStatusText && (
                  <span className="text-[12px] theme-text-secondary">
                    {inlineStatusText}
                  </span>
                )}
                {getStatusIcon(status)}
              </div>
              {instruction && (
                <div className="mt-3 rounded-md theme-bg-tertiary px-3 py-2">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.08em] theme-text-secondary">
                    Instruction
                  </p>
                  <p className="mt-1 whitespace-pre-wrap break-words text-[12px] theme-text-primary">
                    {instruction}
                  </p>
                </div>
              )}
            </div>
          </div>
        </Section>

        {shouldShowToolCalls && (
          <Section title="Tool Calls">
            <ToolCalls invocation={invocation} />
          </Section>
        )}

        {(resultSummary || resultJson || (status === 'error' && errorMessage)) && (
          <Section title="Result">
            {status === 'error' && errorMessage && !resultSummary && (
              <div className="mb-3 rounded-md border border-red-300 bg-red-50/50 px-3 py-2 text-[12px] text-red-700 dark:border-red-700 dark:bg-red-900/10 dark:text-red-300">
                {errorMessage}
              </div>
            )}
            {resultSummary && (
              <div className="text-[12px] theme-text-primary">
                <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
                  {resultSummary}
                </ReactMarkdown>
              </div>
            )}
            {isExplorer ? (
              <ExplorerResultPreview
                connectionName={connectionName}
                result={explorerResult ?? { objects: [] }}
                rawResponseNode={resultJson ? (
                  <div className="text-[12px] theme-text-primary">
                    <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
                      {explorerResult?.rawResponse ?? ''}
                    </ReactMarkdown>
                  </div>
                ) : undefined}
              />
            ) : (
              <PlannerResultPreview
                result={plannerResult ?? { planSteps: [], sqlBlocks: [] }}
                markdownComponents={markdownComponents}
                rawResponseNode={plannerResult?.rawResponse ? (
                  <div className="mt-3 rounded-md theme-bg-tertiary px-3 py-2">
                    <div className="text-[12px] theme-text-primary">
                      <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
                        {plannerResult.rawResponse}
                      </ReactMarkdown>
                    </div>
                  </div>
                ) : undefined}
              />
            )}
          </Section>
        )}
      </div>
    </div>
  );
}
