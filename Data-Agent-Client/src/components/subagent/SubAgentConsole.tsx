import type { ReactNode } from 'react';
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Braces, CheckCircle, ChevronRight, Database, Loader2, XCircle } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { cn } from '../../lib/utils';
import { SUB_AGENT_LABELS } from '../../constants/chat';
import type { SubAgentConsoleTabMetadata, SubAgentInvocation } from '../../types/tab';
import { ToolRunBlock, markdownRemarkPlugins, useMarkdownComponents } from '../ai/blocks';
import { SegmentKind, ToolExecutionState } from '../ai/messageListLib/types';
import { isTodoTool, parseTodoListResponse } from '../ai/blocks/todoTypes';
import { SqlCodeBlock } from '../common/SqlCodeBlock';
import { connectionService } from '../../services/connection.service';
import { QUERY_KEY_CONNECTIONS } from '../../constants/explorer';

export interface SubAgentConsoleProps {
  tabId: string;
  metadata: SubAgentConsoleTabMetadata;
}

function getStatusIcon(status: 'pending' | 'running' | 'complete' | 'error', className?: string) {
  if (status === 'complete') return <CheckCircle className={cn('w-4 h-4 text-green-500', className)} />;
  if (status === 'error') return <XCircle className={cn('w-4 h-4 text-red-500', className)} />;
  return <Loader2 className={cn('w-4 h-4 animate-spin theme-text-secondary', className)} />;
}

function getInvocationStatusText(invocation: SubAgentInvocation): string {
  const tools = invocation.nestedToolCalls ?? [];
  const completed = tools.filter((tool) => tool.status === 'complete').length;
  const runningTool = [...tools].reverse().find((tool) => tool.status === 'running');
  const lastCompletedTool = [...tools].reverse().find((tool) => tool.status === 'complete');
  const failedTool = [...tools].reverse().find((tool) => tool.responseError);

  if (invocation.status === 'error') {
    return failedTool ? `Failed at ${failedTool.toolName}` : 'Agent failed';
  }
  if (invocation.status === 'complete') {
    return 'Complete';
  }
  if (runningTool) {
    return `Calling ${runningTool.toolName}... (${completed}/${tools.length})`;
  }
  if (tools.length > 0 && completed === tools.length) {
    return 'Starting summary...';
  }
  if (lastCompletedTool) {
    return `Called ${lastCompletedTool.toolName}... (${completed}/${tools.length})`;
  }
  return 'Starting Agent...';
}

function ToolCalls({ invocation }: { invocation?: SubAgentInvocation }) {
  const rawToolRuns = invocation?.nestedToolRuns?.filter((segment) => segment.kind === SegmentKind.TOOL_RUN) ?? [];
  const latestTodoIndexById = new Map<string, number>();

  rawToolRuns.forEach((toolRun, index) => {
    if (!isTodoTool(toolRun.toolName)) return;
    const todoId = parseTodoListResponse(toolRun.responseData)?.todoId ?? '';
    if (!todoId) return;
    latestTodoIndexById.set(todoId, index);
  });

  const toolRuns = rawToolRuns.filter((toolRun, index) => {
    if (!isTodoTool(toolRun.toolName)) return true;
    const todoId = parseTodoListResponse(toolRun.responseData)?.todoId ?? '';
    if (!todoId) return true;
    return latestTodoIndexById.get(todoId) === index;
  });
  if (toolRuns.length === 0) {
    return <p className="text-[11px] theme-text-secondary">No tool calls yet.</p>;
  }

  return (
    <div className="space-y-2">
      {toolRuns.map((toolRun, index) => (
        <ToolRunBlock
          key={`${toolRun.toolCallId ?? toolRun.toolName}-${index}`}
          toolName={toolRun.toolName}
          parametersData={toolRun.parametersData}
          responseData={toolRun.responseData}
          responseError={toolRun.responseError}
          pending={toolRun.pending}
          executionState={
            toolRun.executionState === ToolExecutionState.STREAMING_ARGUMENTS
              ? ToolExecutionState.EXECUTING
              : toolRun.executionState
          }
          toolCallId={toolRun.toolCallId}
        />
      ))}
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border theme-border theme-bg-panel px-4 py-3">
      <h3 className="text-[11px] font-semibold uppercase tracking-[0.08em] theme-text-secondary mb-3">
        {title}
      </h3>
      {children}
    </section>
  );
}

interface ExplorerObjectPreview {
  catalog?: string;
  schema?: string;
  objectName: string;
  objectType?: string;
  objectDdl?: string;
  relevance?: string;
}

interface ExplorerResultPayload {
  status?: 'SUCCESS' | 'ERROR';
  summaryText?: string;
  objects: ExplorerObjectPreview[];
  errorMessage?: string;
  rawResponse?: string;
}

function relevanceClass(relevance?: string): string {
  if (relevance === 'HIGH') return 'text-emerald-600 dark:text-emerald-400';
  if (relevance === 'MEDIUM') return 'text-amber-600 dark:text-amber-400';
  if (relevance === 'LOW') return 'text-rose-600 dark:text-rose-400';
  return 'theme-text-primary';
}

function relevanceBlockClass(relevance?: string): string {
  if (relevance === 'HIGH') return 'border-emerald-500/40 bg-emerald-500/12 hover:bg-emerald-500/18';
  if (relevance === 'MEDIUM') return 'border-amber-500/40 bg-amber-500/12 hover:bg-amber-500/18';
  if (relevance === 'LOW') return 'border-rose-500/40 bg-rose-500/12 hover:bg-rose-500/18';
  return 'theme-border theme-bg-tertiary';
}

function shouldExpandObjectDdl(relevance?: string): boolean {
  return relevance === 'HIGH';
}

function parseExplorerResult(resultJson?: string): ExplorerResultPayload {
  if (!resultJson) return { objects: [] };
  try {
    const parsed = JSON.parse(resultJson) as Record<string, unknown>;
    const objects = Array.isArray(parsed.objects) ? parsed.objects : [];
    return {
      status: parsed.status === 'SUCCESS' || parsed.status === 'ERROR' ? parsed.status : undefined,
      summaryText: typeof parsed.summaryText === 'string' ? parsed.summaryText : undefined,
      errorMessage: typeof parsed.errorMessage === 'string' ? parsed.errorMessage : undefined,
      objects: objects
        .filter((object): object is Record<string, unknown> => !!object && typeof object === 'object')
        .map((object) => ({
          catalog: typeof object.catalog === 'string' ? object.catalog : undefined,
          schema: typeof object.schema === 'string' ? object.schema : undefined,
          objectName: typeof object.objectName === 'string' ? object.objectName : 'unknown_object',
          objectType: typeof object.objectType === 'string' ? object.objectType : undefined,
          objectDdl: typeof object.objectDdl === 'string' ? object.objectDdl : undefined,
          relevance: typeof object.relevance === 'string' ? object.relevance : undefined,
        })),
      rawResponse: typeof parsed.rawResponse === 'string' ? parsed.rawResponse : undefined,
    };
  } catch {
    return { objects: [] };
  }
}

interface PlannerStepPreview {
  title?: string;
  content?: string;
}

type PlannerSqlBlockKind = 'FINAL' | 'CHECK' | 'ALTERNATIVE';

interface PlannerSqlBlockPreview {
  title?: string;
  sql: string;
  kind?: PlannerSqlBlockKind;
}

interface PlannerResultPayload {
  summaryText?: string;
  planSteps: PlannerStepPreview[];
  sqlBlocks: PlannerSqlBlockPreview[];
  rawResponse?: string;
}

function parsePlannerSqlBlockKind(value: unknown): PlannerSqlBlockKind | undefined {
  if (value === 'FINAL' || value === 'CHECK' || value === 'ALTERNATIVE') {
    return value;
  }
  return undefined;
}

function parsePlannerResult(resultJson?: string): PlannerResultPayload {
  if (!resultJson) return { planSteps: [], sqlBlocks: [] };
  try {
    const parsed = JSON.parse(resultJson) as Record<string, unknown>;
    const planSteps = Array.isArray(parsed.planSteps) ? parsed.planSteps : [];
    const sqlBlocks = Array.isArray(parsed.sqlBlocks) ? parsed.sqlBlocks : [];
    return {
      summaryText: typeof parsed.summaryText === 'string' ? parsed.summaryText : undefined,
      planSteps: planSteps
        .filter((step): step is Record<string, unknown> => !!step && typeof step === 'object')
        .map((step) => ({
          title: typeof step.title === 'string' ? step.title : undefined,
          content: typeof step.content === 'string' ? step.content : undefined,
        })),
      sqlBlocks: sqlBlocks
        .filter((block): block is Record<string, unknown> => !!block && typeof block === 'object')
        .map((block) => ({
          title: typeof block.title === 'string' ? block.title : undefined,
          sql: typeof block.sql === 'string' ? block.sql : '',
          kind: parsePlannerSqlBlockKind(block.kind),
        }))
        .filter((block) => block.sql.trim().length > 0),
      rawResponse: typeof parsed.rawResponse === 'string' ? parsed.rawResponse : undefined,
    };
  } catch {
    return { planSteps: [], sqlBlocks: [] };
  }
}

function shouldExpandSqlBlock(kind?: PlannerSqlBlockKind): boolean {
  return kind === 'FINAL';
}

function inferConnectionId(metadata: SubAgentConsoleTabMetadata): number | undefined {
  const invocation = metadata.invocations[0];
  const eventConnectionId = invocation?.progressEvents.find((event) => event.connectionId != null)?.connectionId;
  if (eventConnectionId != null) {
    return eventConnectionId;
  }
  if (invocation?.params.connectionIds?.length === 1) {
    return invocation.params.connectionIds[0];
  }
  if (metadata.params?.connectionIds?.length === 1) {
    return metadata.params.connectionIds[0];
  }
  return undefined;
}

function ExplorerObjectLabel({
  connectionName,
  catalog,
  schema,
  objectName,
  className,
}: {
  connectionName?: string;
  catalog?: string;
  schema?: string;
  objectName: string;
  className?: string;
}) {
  const namespace = [catalog, schema].filter(Boolean).join('.');
  const scopeLabel = [connectionName, namespace].filter(Boolean).join(' · ');

  return (
    <span className={cn('flex min-w-0 flex-col', className)}>
      {scopeLabel && (
        <span className="theme-text-secondary text-[10px] leading-4 opacity-70">
          {scopeLabel}
        </span>
      )}
      <span className="min-w-0 whitespace-normal break-keep leading-5">
        {objectName}
      </span>
    </span>
  );
}

function ExplorerResultPreview({
  connectionName,
  resultJson,
  rawResponseNode,
}: {
  connectionName?: string;
  resultJson?: string;
  rawResponseNode?: ReactNode;
}) {
  const result = parseExplorerResult(resultJson);
  if (result.objects.length === 0 && !result.rawResponse && !result.errorMessage) return null;

  return (
    <div className="mt-4 space-y-5">
      {result.errorMessage && (
        <div className="rounded-md border border-red-300 bg-red-50/50 px-3 py-2 text-[12px] text-red-700 dark:border-red-700 dark:bg-red-900/10 dark:text-red-300">
          {result.errorMessage}
        </div>
      )}
      {result.objects.map((object, index) => (
        <div key={`${object.catalog ?? ''}-${object.schema ?? ''}-${object.objectName}-${index}`}>
          {object.objectDdl ? (
            <details
              className="group"
              open={shouldExpandObjectDdl(object.relevance)}
            >
              <summary
                className={cn(
                  'list-none cursor-pointer rounded-md border px-3 py-2 transition-colors',
                  'flex items-start gap-2',
                  relevanceBlockClass(object.relevance),
                )}
              >
                <ChevronRight className="mt-0.5 h-4 w-4 shrink-0 theme-text-secondary transition-transform group-open:rotate-90" />
                <ExplorerObjectLabel
                  connectionName={connectionName}
                  catalog={object.catalog}
                  schema={object.schema}
                  objectName={object.objectName}
                  className={cn('text-[12px] font-mono', relevanceClass(object.relevance))}
                />
              </summary>
              <div className="mt-2">
                <SqlCodeBlock
                  variant="block"
                  language="sql"
                  sql={object.objectDdl}
                  wrapLongLines={true}
                />
              </div>
            </details>
          ) : (
            <div className={cn('rounded-md border px-3 py-2', relevanceBlockClass(object.relevance))}>
              <ExplorerObjectLabel
                connectionName={connectionName}
                catalog={object.catalog}
                schema={object.schema}
                objectName={object.objectName}
                className={cn('text-[12px] font-mono', relevanceClass(object.relevance))}
              />
            </div>
          )}
        </div>
      ))}
      {result.rawResponse && rawResponseNode}
    </div>
  );
}

function PlannerResultPreview({
  resultJson,
  rawResponseNode,
  markdownComponents,
}: {
  resultJson?: string;
  rawResponseNode?: ReactNode;
  markdownComponents: ReturnType<typeof useMarkdownComponents>;
}) {
  const result = parsePlannerResult(resultJson);
  if (result.sqlBlocks.length === 0 && result.planSteps.length === 0 && !result.rawResponse) return null;

  return (
    <div className="mt-4 space-y-5">
      {result.sqlBlocks.length > 0 && (
        <div className="space-y-3">
          {result.sqlBlocks.map((block, index) => (
            <details
              key={`${block.kind ?? ''}-${block.title ?? ''}-${index}`}
              className="group"
              open={shouldExpandSqlBlock(block.kind)}
            >
              <summary
                className={cn(
                  'list-none cursor-pointer rounded-md border px-3 py-2 transition-colors',
                  'flex items-center gap-2 theme-border theme-bg-tertiary hover:opacity-90',
                )}
              >
                <ChevronRight className="h-4 w-4 shrink-0 theme-text-secondary transition-transform group-open:rotate-90" />
                <span className="text-[12px] font-medium theme-text-primary">
                  {block.title ?? 'SQL'}
                </span>
                {block.kind && (
                  <span className="text-[10px] uppercase tracking-[0.08em] theme-text-secondary">
                    {block.kind}
                  </span>
                )}
              </summary>
              <div className="mt-2">
                <SqlCodeBlock
                  variant="block"
                  language="sql"
                  sql={block.sql}
                  wrapLongLines={true}
                />
              </div>
            </details>
          ))}
        </div>
      )}

      {result.planSteps.length > 0 && (
        <div className="space-y-2">
          {result.planSteps.map((step, index) => (
            <div key={`${step.title ?? ''}-${index}`} className="rounded-md border theme-border theme-bg-tertiary px-3 py-2">
              <p className="text-[12px] font-medium theme-text-primary">
                {step.title ?? `Step ${index + 1}`}
              </p>
              {step.content && (
                <div className="mt-1 text-[12px] theme-text-primary">
                  <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
                    {step.content}
                  </ReactMarkdown>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {result.rawResponse && rawResponseNode}
    </div>
  );
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
                resultJson={resultJson}
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
                resultJson={resultJson}
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
