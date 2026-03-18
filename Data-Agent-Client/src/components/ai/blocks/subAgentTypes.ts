import { resolveToolCallPreview } from './toolCallPreview';

export const SUB_AGENT_TYPES = {
  EXPLORER: 'explorer',
  PLANNER: 'planner',
} as const;

export type SubAgentType = typeof SUB_AGENT_TYPES[keyof typeof SUB_AGENT_TYPES];

export interface SubAgentProgressEvent {
  agentType: SubAgentType;
  phase: 'start' | 'progress' | 'complete' | 'error';
  message?: string;
  /** Tool usage stats (present on 'complete' phase). */
  toolCount?: number;
  toolCounts?: Record<string, number>;
  /** Identifies which parallel SubAgent task this event belongs to. */
  taskId?: string;
  /** Stable connection identity from backend lifecycle events. */
  connectionId?: number;
  /** Effective timeout for this sub-agent/task. */
  timeoutSeconds?: number;
  /** Real task-scoped result summary from backend SUB_AGENT_COMPLETE. */
  summaryText?: string;
  /** Real task-scoped result payload from backend SUB_AGENT_COMPLETE. */
  resultJson?: string;
}

export function normalizeSubAgentType(agentType: unknown): SubAgentType | null {
  if (agentType === SUB_AGENT_TYPES.EXPLORER) return SUB_AGENT_TYPES.EXPLORER;
  if (agentType === SUB_AGENT_TYPES.PLANNER) return SUB_AGENT_TYPES.PLANNER;
  return null;
}

export interface ResolvedSubAgentResult {
  summaryText?: string;
  resultJson?: string;
}

function buildExplorerPayload(raw: Record<string, unknown>): string | undefined {
  const payload: Record<string, unknown> = {};
  if (typeof raw.status === 'string') {
    payload.status = raw.status;
  }
  if (Array.isArray(raw.objects)) {
    payload.objects = raw.objects;
  }
  if (typeof raw.summaryText === 'string') {
    payload.summaryText = raw.summaryText;
  }
  if (typeof raw.errorMessage === 'string') {
    payload.errorMessage = raw.errorMessage;
  }
  if (typeof raw.rawResponse === 'string') {
    payload.rawResponse = raw.rawResponse;
  }
  return Object.keys(payload).length > 0 ? JSON.stringify(payload) : undefined;
}

function buildPlannerPayload(raw: Record<string, unknown>): string | undefined {
  const payload: Record<string, unknown> = {};
  if (typeof raw.summaryText === 'string') {
    payload.summaryText = raw.summaryText;
  }
  if (Array.isArray(raw.planSteps)) {
    payload.planSteps = raw.planSteps;
  }
  if (Array.isArray(raw.sqlBlocks)) {
    payload.sqlBlocks = raw.sqlBlocks;
  }
  if (typeof raw.rawResponse === 'string') {
    payload.rawResponse = raw.rawResponse;
  }
  return Object.keys(payload).length > 0 ? JSON.stringify(payload) : undefined;
}

const VALID_PHASES = new Set(['start', 'progress', 'complete', 'error']);
const SUB_AGENT_TOOL_NAMES = new Set(['callingExplorerSubAgent', 'callingPlannerSubAgent']);

export function isCallingSubAgentTool(name: string): boolean {
  return SUB_AGENT_TOOL_NAMES.has(name);
}

/** Derive agentType from tool name (exploreSchema → explorer, generateSqlPlan → planner). */
export function agentTypeFromToolName(toolName: string): SubAgentType | null {
  if (toolName === 'callingExplorerSubAgent') return SUB_AGENT_TYPES.EXPLORER;
  if (toolName === 'callingPlannerSubAgent') return SUB_AGENT_TYPES.PLANNER;
  return null;
}

/**
 * Try to parse a STATUS block's data as a SubAgent progress event.
 * Returns null if data is not a valid SubAgent progress JSON.
 */
export function tryParseSubAgentProgress(data: string | undefined): SubAgentProgressEvent | null {
  if (!data) return null;
  try {
    const parsed = JSON.parse(data) as Record<string, unknown>;
    const normalizedAgentType = normalizeSubAgentType(parsed.agentType);
    if (
      normalizedAgentType &&
      typeof parsed.phase === 'string' &&
      VALID_PHASES.has(parsed.phase)
    ) {
      return {
        agentType: normalizedAgentType,
        phase: parsed.phase as SubAgentProgressEvent['phase'],
        message: typeof parsed.message === 'string' ? parsed.message : undefined,
        toolCount: typeof parsed.toolCount === 'number' ? parsed.toolCount : undefined,
        toolCounts: parsed.toolCounts && typeof parsed.toolCounts === 'object'
          ? parsed.toolCounts as Record<string, number>
          : undefined,
        taskId: typeof parsed.taskId === 'string' ? parsed.taskId : undefined,
        connectionId: typeof parsed.connectionId === 'number' ? parsed.connectionId : undefined,
        timeoutSeconds: typeof parsed.timeoutSeconds === 'number' ? parsed.timeoutSeconds : undefined,
        summaryText: typeof parsed.summaryText === 'string' ? parsed.summaryText : undefined,
        resultJson: typeof parsed.resultJson === 'string' ? parsed.resultJson : undefined,
      };
    }
  } catch (error) {
    console.warn("[SubAgent] progress parse failed", { data, error });
  }
  return null;
}

export interface CallingSubAgentArgs {
  agentType: SubAgentType;
  userQuestion?: string;
  connectionIds?: number[];
  taskCount?: number;
  taskInstructions?: string[];
  timeoutSeconds?: number;
  taskTimeoutSeconds?: Array<number | undefined>;
}

/**
 * Parse the arguments of callingExplorerSubAgent / callingPlannerSubAgent TOOL_CALL.
 * agentType is derived from toolName.
 *
 * Explorer format: { tasks: [{connectionId:1, instruction:..., timeoutSeconds?: N}, ...], timeoutSeconds?: N }
 * Planner format: { instruction: "...", schemaSummaryJson: "...", timeoutSeconds?: N }
 */
export function parseCallingSubAgentArgs(
  args: string,
  toolName?: string
): CallingSubAgentArgs | null {
  const derivedAgentType = toolName ? agentTypeFromToolName(toolName) : null;
  const preview = resolveToolCallPreview(toolName ?? '', args, 'streaming');
  if (!args && !derivedAgentType) return null;

  try {
    let parsed: unknown = args && args.trim() ? JSON.parse(args) : {};
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return derivedAgentType ? { agentType: derivedAgentType } : null;
    }
    const obj = parsed as Record<string, unknown>;

    const agentType = normalizeSubAgentType(obj.agentType) ?? derivedAgentType;
    if (!agentType) {
      return null;
    }

    // Extract connectionIds from tasks (Explorer format).
    let connectionIds: number[] | undefined;
    let taskCount: number | undefined;
    let taskInstructions: string[] | undefined;
    let taskTimeoutSeconds: Array<number | undefined> | undefined;
    if (Array.isArray(obj.tasks)) {
      const tasks = obj.tasks;
      taskCount = tasks.length;
      connectionIds = tasks
        .map((t) => (t && typeof t === 'object' && 'connectionId' in t) ? (t as Record<string, unknown>).connectionId : null)
        .filter((id): id is number => typeof id === 'number');
      taskInstructions = tasks
        .map((t) => (t && typeof t === 'object' && 'instruction' in t) ? (t as Record<string, unknown>).instruction : null)
        .filter((instruction): instruction is string => typeof instruction === 'string' && instruction.trim().length > 0);
      taskTimeoutSeconds = tasks
        .map((t) => {
          if (!t || typeof t !== 'object' || !('timeoutSeconds' in t)) {
            return undefined;
          }
          const timeoutSeconds = (t as Record<string, unknown>).timeoutSeconds;
          return typeof timeoutSeconds === 'number' && timeoutSeconds > 0 ? timeoutSeconds : undefined;
        });
    }

    const directInstruction = typeof obj.instruction === 'string' ? obj.instruction : undefined;
    const timeoutSeconds = typeof obj.timeoutSeconds === 'number' && obj.timeoutSeconds > 0
      ? obj.timeoutSeconds
      : undefined;
    if ((!taskInstructions || taskInstructions.length === 0) && preview?.taskInstructions?.length) {
      taskInstructions = preview.taskInstructions;
    }
    const userQuestion = directInstruction
      ?? preview?.instruction
      ?? (taskInstructions?.length === 1 ? taskInstructions[0] : undefined);

    if ((!connectionIds || connectionIds.length === 0) && preview?.connectionIds?.length) {
      connectionIds = preview.connectionIds;
    }
    if (!taskCount) {
      taskCount = preview?.taskInstructions?.length ?? preview?.connectionIds?.length;
    }

    return {
      agentType,
      userQuestion,
      connectionIds: connectionIds?.length ? connectionIds : undefined,
      taskCount,
      taskInstructions: taskInstructions?.length ? taskInstructions : (directInstruction ? [directInstruction] : undefined),
      timeoutSeconds,
      taskTimeoutSeconds: taskTimeoutSeconds?.some((value) => value != null) ? taskTimeoutSeconds : undefined,
    };
  } catch {
    // Streaming: args arrive as partial JSON — silently fall back
    if (derivedAgentType) {
      return {
        agentType: derivedAgentType,
        userQuestion: preview?.instruction ?? preview?.userQuestion,
        connectionIds: preview?.connectionIds,
        taskCount: preview?.taskInstructions?.length ?? preview?.connectionIds?.length,
        taskInstructions: preview?.taskInstructions,
        timeoutSeconds: undefined,
        taskTimeoutSeconds: undefined,
      };
    }
  }
  return null;
}

/**
 * Extract the real summaryText from the SubAgent TOOL_RESULT when present.
 */
export function getSubAgentResultSummary(agentType: string, responseData: string): string {
  return resolveSubAgentResult(agentType, responseData).summaryText ?? '';
}

export function resolveSubAgentResult(
  agentType: string,
  responseData: string,
  taskId?: string
): ResolvedSubAgentResult {
  if (!responseData) return {};
  try {
    let parsed: unknown = JSON.parse(responseData);
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {};
    const obj = parsed as Record<string, unknown>;

    // Unwrap { result: "...", _trace: ... } wrapper
    let inner = obj;
    if (typeof obj.result === 'string') {
      try {
        const innerParsed = JSON.parse(obj.result) as unknown;
        if (innerParsed && typeof innerParsed === 'object' && !Array.isArray(innerParsed)) {
          inner = innerParsed as Record<string, unknown>;
        }
      } catch { /* use obj directly */ }
    }

    const normalized = agentType.toUpperCase().trim();
    if (normalized === 'EXPLORER' || agentType === 'explorer') {
      if (Array.isArray(inner.taskResults)) {
        const taskResults = inner.taskResults.filter((item): item is Record<string, unknown> => !!item && typeof item === 'object');
        const matchedTask = taskId
          ? taskResults.find((item) => item.taskId === taskId)
          : taskResults.length === 1 ? taskResults[0] : undefined;
        if (matchedTask) {
          return {
            summaryText: typeof matchedTask.summaryText === 'string' ? matchedTask.summaryText : undefined,
            resultJson: buildExplorerPayload(matchedTask),
          };
        }
        if (!taskId && taskResults.length > 0) {
          return {
            summaryText: `Completed ${taskResults.length} exploration task${taskResults.length === 1 ? '' : 's'}.`,
            resultJson: responseData,
          };
        }
        return {};
      }

      if (Array.isArray(inner.objects) || typeof inner.rawResponse === 'string') {
        return {
          summaryText: typeof inner.summaryText === 'string' ? inner.summaryText : undefined,
          resultJson: buildExplorerPayload(inner),
        };
      }

      const tables = Array.isArray(inner.tables) ? inner.tables : [];
      const colCount = tables.reduce((sum: number, t: unknown) => {
        if (t && typeof t === 'object' && 'columns' in t && Array.isArray((t as Record<string, unknown>).columns)) {
          return sum + ((t as Record<string, unknown>).columns as unknown[]).length;
        }
        return sum;
      }, 0);
      if (tables.length === 0 && colCount === 0) return {};
      return {
        summaryText: `Found ${tables.length} table${tables.length !== 1 ? 's' : ''}, ${colCount} column${colCount !== 1 ? 's' : ''}`,
        resultJson: JSON.stringify(inner),
      };
    }

    if (normalized === 'PLANNER' || agentType === SUB_AGENT_TYPES.PLANNER) {
      if (
        typeof inner.summaryText === 'string'
        || Array.isArray(inner.planSteps)
        || Array.isArray(inner.sqlBlocks)
        || typeof inner.rawResponse === 'string'
      ) {
        return {
          summaryText: typeof inner.summaryText === 'string' ? inner.summaryText : undefined,
          resultJson: buildPlannerPayload(inner),
        };
      }
    }
  } catch {
    // Parse failed
  }
  return {};
}
