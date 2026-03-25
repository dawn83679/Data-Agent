import type { ReactNode } from 'react';
import { ChevronRight } from 'lucide-react';
import ReactMarkdown, { type Components } from 'react-markdown';
import { cn } from '../../lib/utils';
import type { SubAgentInvocation } from '../../types/tab';
import { ToolRunBlock, isTodoTool, markdownRemarkPlugins, parseTodoListResponse } from '../ai/blocks';
import { SegmentKind, ToolExecutionState } from '../ai/messageListLib/types';
import { SqlCodeBlock } from '../common/SqlCodeBlock';
import type { ExplorerResultPayload, PlannerResultPayload, PlannerSqlBlockKind } from './subAgentConsoleTypes';

function relevanceTier(score?: number): 'high' | 'medium' | 'low' | 'unknown' {
  if (typeof score !== 'number') return 'unknown';
  if (score >= 80) return 'high';
  if (score >= 50) return 'medium';
  return 'low';
}

function relevanceClass(score?: number): string {
  const tier = relevanceTier(score);
  if (tier === 'high') return 'text-emerald-600 dark:text-emerald-400';
  if (tier === 'medium') return 'text-amber-600 dark:text-amber-400';
  if (tier === 'low') return 'text-rose-600 dark:text-rose-400';
  return 'theme-text-primary';
}

function relevanceBlockClass(score?: number): string {
  const tier = relevanceTier(score);
  if (tier === 'high') return 'border-emerald-500/40 bg-emerald-500/12 hover:bg-emerald-500/18';
  if (tier === 'medium') return 'border-amber-500/40 bg-amber-500/12 hover:bg-amber-500/18';
  if (tier === 'low') return 'border-rose-500/40 bg-rose-500/12 hover:bg-rose-500/18';
  return 'theme-border theme-bg-tertiary';
}

function shouldExpandObjectDdl(score?: number): boolean {
  return relevanceTier(score) === 'high';
}

function shouldExpandSqlBlock(kind?: PlannerSqlBlockKind): boolean {
  return kind === 'FINAL';
}

function ExplorerObjectLabel({
  connectionName,
  catalog,
  schema,
  objectName,
  relevanceScore,
  className,
}: {
  connectionName?: string;
  catalog?: string;
  schema?: string;
  objectName: string;
  relevanceScore?: number;
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
      {typeof relevanceScore === 'number' && (
        <span className="theme-text-secondary text-[10px] leading-4 opacity-80">
          score {relevanceScore}
        </span>
      )}
    </span>
  );
}

export function ToolCalls({ invocation }: { invocation?: SubAgentInvocation }) {
  const rawToolRuns = invocation?.nestedToolRuns?.filter((segment) => segment.kind === SegmentKind.TOOL_RUN) ?? [];
  const toolRunFailedByAgentError = invocation?.status === 'error';
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
        (() => {
          const isUnfinished =
            toolRun.pending
            || toolRun.executionState === ToolExecutionState.STREAMING_ARGUMENTS
            || toolRun.executionState === ToolExecutionState.EXECUTING;
          const shouldPromoteToError = Boolean(toolRunFailedByAgentError && isUnfinished);
          const coercedExecutionState = shouldPromoteToError
            ? ToolExecutionState.COMPLETE
            : toolRun.executionState;

          return (
        <ToolRunBlock
          key={`${toolRun.toolCallId ?? toolRun.toolName}-${index}`}
          toolName={toolRun.toolName}
          parametersData={toolRun.parametersData}
          responseData={toolRun.responseData}
          responseError={Boolean(toolRun.responseError || shouldPromoteToError)}
          pending={shouldPromoteToError ? false : toolRun.pending}
          executionState={
            coercedExecutionState === ToolExecutionState.STREAMING_ARGUMENTS
              ? ToolExecutionState.EXECUTING
              : coercedExecutionState
          }
          toolCallId={toolRun.toolCallId}
        />
          );
        })()
      ))}
    </div>
  );
}

export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border theme-border theme-bg-panel px-4 py-3">
      <h3 className="mb-3 text-[11px] font-semibold uppercase tracking-[0.08em] theme-text-secondary">
        {title}
      </h3>
      {children}
    </section>
  );
}

export function ExplorerResultPreview({
  connectionName,
  result,
  rawResponseNode,
}: {
  connectionName?: string;
  result: ExplorerResultPayload;
  rawResponseNode?: ReactNode;
}) {
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
            <details className="group" open={shouldExpandObjectDdl(object.relevanceScore)}>
              <summary
                className={cn(
                  'list-none cursor-pointer rounded-md border px-3 py-2 transition-colors',
                  'flex items-start gap-2',
                  relevanceBlockClass(object.relevanceScore),
                )}
              >
                <ChevronRight className="mt-0.5 h-4 w-4 shrink-0 theme-text-secondary transition-transform group-open:rotate-90" />
                <ExplorerObjectLabel
                  connectionName={connectionName}
                  catalog={object.catalog}
                  schema={object.schema}
                  objectName={object.objectName}
                  relevanceScore={object.relevanceScore}
                  className={cn('text-[12px] font-mono', relevanceClass(object.relevanceScore))}
                />
              </summary>
              <div className="mt-2">
                <SqlCodeBlock variant="block" language="sql" sql={object.objectDdl} wrapLongLines={true} />
              </div>
            </details>
          ) : (
            <div className={cn('rounded-md border px-3 py-2', relevanceBlockClass(object.relevanceScore))}>
              <ExplorerObjectLabel
                connectionName={connectionName}
                catalog={object.catalog}
                schema={object.schema}
                objectName={object.objectName}
                relevanceScore={object.relevanceScore}
                className={cn('text-[12px] font-mono', relevanceClass(object.relevanceScore))}
              />
            </div>
          )}
        </div>
      ))}
      {result.rawResponse && rawResponseNode}
    </div>
  );
}

export function PlannerResultPreview({
  result,
  rawResponseNode,
  markdownComponents,
}: {
  result: PlannerResultPayload;
  rawResponseNode?: ReactNode;
  markdownComponents?: Components | null;
}) {
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
                <SqlCodeBlock variant="block" language="sql" sql={block.sql} wrapLongLines={true} />
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
                  <ReactMarkdown components={markdownComponents ?? undefined} remarkPlugins={markdownRemarkPlugins}>
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
