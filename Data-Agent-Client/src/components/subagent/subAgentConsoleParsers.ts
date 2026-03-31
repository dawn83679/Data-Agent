import type { SubAgentConsoleTabMetadata, SubAgentInvocation } from '../../types/tab';
import { PLANNER_SQL_BLOCK_KIND, type ExplorerResultPayload, type PlannerResultPayload, type PlannerSqlBlockKind } from './subAgentConsoleTypes';
import { getToolDisplayName } from '../ai/blocks/sqlDiscoveryToolUtils';

function normalizeRelevanceScore(value: unknown): number | undefined {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return undefined;
  }
  return Math.max(0, Math.min(100, Math.round(value)));
}

export function getInvocationStatusText(invocation: SubAgentInvocation): string {
  const tools = invocation.nestedToolCalls ?? [];
  const completed = tools.filter((tool) => tool.status === 'complete').length;
  const runningTool = [...tools].reverse().find((tool) => tool.status === 'running');
  const lastCompletedTool = [...tools].reverse().find((tool) => tool.status === 'complete');
  const failedTool = [...tools].reverse().find((tool) => tool.responseError);

  if (invocation.status === 'error') {
    return failedTool ? `Failed at ${getToolDisplayName(failedTool.toolName)}` : 'Agent failed';
  }
  if (invocation.status === 'complete') {
    return 'Complete';
  }
  if (runningTool) {
    return `Calling ${getToolDisplayName(runningTool.toolName)}... (${completed}/${tools.length})`;
  }
  if (tools.length > 0 && completed === tools.length) {
    return 'Starting summary...';
  }
  if (lastCompletedTool) {
    return `Called ${getToolDisplayName(lastCompletedTool.toolName)}... (${completed}/${tools.length})`;
  }
  return 'Starting Agent...';
}

export function inferConnectionId(metadata: SubAgentConsoleTabMetadata): number | undefined {
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

export function parseExplorerResult(resultJson?: string): ExplorerResultPayload {
  if (!resultJson) {
    return { objects: [] };
  }
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
          relevanceScore: normalizeRelevanceScore(object.relevanceScore),
        })),
      rawResponse: typeof parsed.rawResponse === 'string' ? parsed.rawResponse : undefined,
    };
  } catch {
    return { objects: [] };
  }
}

function parsePlannerSqlBlockKind(value: unknown): PlannerSqlBlockKind | undefined {
  if (
    value === PLANNER_SQL_BLOCK_KIND.FINAL
    || value === PLANNER_SQL_BLOCK_KIND.CHECK
    || value === PLANNER_SQL_BLOCK_KIND.ALTERNATIVE
  ) {
    return value;
  }
  return undefined;
}

export function parsePlannerResult(resultJson?: string): PlannerResultPayload {
  if (!resultJson) {
    return { planSteps: [], sqlBlocks: [] };
  }
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
