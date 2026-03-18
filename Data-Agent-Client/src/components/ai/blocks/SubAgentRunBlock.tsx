import { useEffect, useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { parseCallingSubAgentArgs, resolveSubAgentResult, SUB_AGENT_TYPES } from './subAgentTypes';
import type { CallingSubAgentArgs, SubAgentProgressEvent, SubAgentType } from './subAgentTypes';
import { SUB_AGENT_LABELS } from '../../../constants/chat';
import type { Segment } from '../messageListLib/types';
import { SegmentKind } from '../messageListLib/types';
import { buildNestedToolCalls, getSubAgentStatusText, subAgentConsoleTabId } from './subAgentDataHelpers';
import { useSubAgentConsoleTab } from './subAgentConsoleHook';
import { SingleSubAgentCard } from './SingleSubAgentCard';
import type { SubAgentInvocation } from '../../../types/tab';
import { useAIAssistantContext } from '../AIAssistantContext';
import { useWorkspaceStore } from '../../../store/workspaceStore';
import { connectionService } from '../../../services/connection.service';
import { QUERY_KEY_CONNECTIONS } from '../../../constants/explorer';

export interface SubAgentRunBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError?: boolean;
  progressEvents?: SubAgentProgressEvent[];
  toolCallId?: string;
  nestedToolRuns?: Segment[];
}

type ToolRunSegment = Extract<Segment, { kind: typeof SegmentKind.TOOL_RUN }>;

interface RequestTaskSlot {
  taskKey: string;
  slotIndex: number;
  connectionId?: number;
  instruction?: string;
  timeoutSeconds?: number;
}

interface TaskViewModel {
  taskKey: string;
  label: string;
  agentType: SubAgentType;
  consoleTaskKey?: string;
  consoleTabId?: string;
  isConsoleReady: boolean;
  isComplete: boolean;
  isError: boolean;
  statusText: string;
  elapsedText?: string;
  nestedToolRuns?: Segment[];
  resultSummary?: string;
  resultJson?: string;
  errorMessage?: string;
  invocation: SubAgentInvocation;
}

const MIN_SUB_AGENT_TIMEOUT_SECONDS = 120;

function normalizePreviewTimeoutSeconds(timeoutSeconds?: number): number | undefined {
  if (!timeoutSeconds || timeoutSeconds <= 0) {
    return undefined;
  }
  return Math.max(timeoutSeconds, MIN_SUB_AGENT_TIMEOUT_SECONDS);
}

function parseStableExplorerArgs(parametersData: string, toolName?: string): CallingSubAgentArgs | null {
  if (toolName !== 'callingExplorerSubAgent' || !parametersData.trim()) {
    return null;
  }

  try {
    let parsed: unknown = JSON.parse(parametersData);
    if (typeof parsed === 'string') {
      parsed = JSON.parse(parsed) as unknown;
    }
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null;
    }

    const obj = parsed as Record<string, unknown>;
    if (!Array.isArray(obj.tasks) || obj.tasks.length === 0) {
      return null;
    }

    const hasCompleteTasks = obj.tasks.every((task) => (
      !!task
      && typeof task === 'object'
      && typeof (task as Record<string, unknown>).connectionId === 'number'
      && typeof (task as Record<string, unknown>).instruction === 'string'
      && ((task as Record<string, unknown>).instruction as string).trim().length > 0
    ));

    if (!hasCompleteTasks) {
      return null;
    }

  return parseCallingSubAgentArgs(parametersData, toolName);
  } catch {
    return null;
  }
}

function buildRequestTaskSlots(args: CallingSubAgentArgs | null): RequestTaskSlot[] {
  const connectionIds = args?.connectionIds ?? [];
  const taskInstructions = args?.taskInstructions ?? [];
  const taskTimeoutSeconds = args?.taskTimeoutSeconds ?? [];
  const taskCount = Math.max(
    args?.taskCount ?? 0,
    connectionIds.length,
    taskInstructions.length,
    taskTimeoutSeconds.length,
    1,
  );

  if (taskCount <= 1) {
    return [{
      taskKey: 'single',
      slotIndex: 0,
      connectionId: connectionIds[0],
      instruction: args?.userQuestion ?? taskInstructions[0],
      timeoutSeconds: normalizePreviewTimeoutSeconds(taskTimeoutSeconds[0] ?? args?.timeoutSeconds),
    }];
  }

  return Array.from({ length: taskCount }, (_, index) => ({
    taskKey: `slot-${index + 1}`,
    slotIndex: index,
    connectionId: connectionIds[index],
    instruction: taskInstructions[index],
    timeoutSeconds: normalizePreviewTimeoutSeconds(taskTimeoutSeconds[index] ?? args?.timeoutSeconds),
  }));
}

function resolveSlotKey(options: {
  taskId?: string;
  connectionId?: number;
  slots: RequestTaskSlot[];
  taskIdToKey: Map<string, string>;
}): string | undefined {
  const { taskId, connectionId, slots, taskIdToKey } = options;
  if (taskId && taskIdToKey.has(taskId)) {
    return taskIdToKey.get(taskId);
  }

  if (connectionId != null) {
    const matchingSlots = slots.filter((slot) => slot.connectionId === connectionId);
    if (matchingSlots.length === 1) {
      return matchingSlots[0].taskKey;
    }
    if (matchingSlots.length > 1) {
      const usedKeys = new Set(taskIdToKey.values());
      return matchingSlots.find((slot) => !usedKeys.has(slot.taskKey))?.taskKey ?? matchingSlots[0].taskKey;
    }
  }

  if (slots.length === 1) {
    return slots[0].taskKey;
  }

  if (taskId) {
    const usedKeys = new Set(taskIdToKey.values());
    return slots.find((slot) => !usedKeys.has(slot.taskKey))?.taskKey;
  }

  return undefined;
}

function buildTaskViewModels(options: {
  toolCallId: string;
  agentType: SubAgentType;
  agentLabel: string;
  connectionNameById: Map<number, string>;
  args: ReturnType<typeof parseCallingSubAgentArgs>;
  startedAt: number;
  completedAt?: number;
  isComplete: boolean;
  isError: boolean;
  progressEvents?: SubAgentProgressEvent[];
  nestedToolRuns?: Segment[];
  responseData?: string;
  nowMs: number;
  taskStartedAt: Map<string, number>;
  taskCompletedAt: Map<string, number>;
}): TaskViewModel[] {
  const {
    toolCallId,
    agentType,
    agentLabel,
    connectionNameById,
    args,
    startedAt,
    completedAt,
    isComplete,
    isError,
    progressEvents,
    nestedToolRuns,
    responseData,
    nowMs,
    taskStartedAt,
    taskCompletedAt,
  } = options;

  const requestSlots = buildRequestTaskSlots(args);
  const toolSegments = nestedToolRuns?.filter((segment): segment is ToolRunSegment => segment.kind === SegmentKind.TOOL_RUN) ?? [];
  const progressByKey = new Map<string, SubAgentProgressEvent[]>();
  const nestedByKey = new Map<string, Segment[]>();
  const taskIdToKey = new Map<string, string>();

  for (const event of progressEvents ?? []) {
    const taskKey = resolveSlotKey({
      taskId: event.taskId,
      connectionId: event.connectionId,
      slots: requestSlots,
      taskIdToKey,
    });
    if (!taskKey) {
      continue;
    }
    if (event.taskId) {
      taskIdToKey.set(event.taskId, taskKey);
    }
    const current = progressByKey.get(taskKey) ?? [];
    current.push(event);
    progressByKey.set(taskKey, current);
  }

  for (const segment of toolSegments) {
    const taskKey = resolveSlotKey({
      taskId: segment.subAgentTaskId,
      slots: requestSlots,
      taskIdToKey,
    });
    if (!taskKey) {
      continue;
    }
    if (segment.subAgentTaskId) {
      taskIdToKey.set(segment.subAgentTaskId, taskKey);
    }
    const current = nestedByKey.get(taskKey) ?? [];
    current.push(segment);
    nestedByKey.set(taskKey, current);
  }

  return requestSlots.map((slot, index) => {
    const taskKey = slot.taskKey;
    const taskProgress = progressByKey.get(taskKey) ?? [];
    const taskNested = nestedByKey.get(taskKey);
    const taskId = [...taskIdToKey.entries()].find(([, mappedTaskKey]) => mappedTaskKey === taskKey)?.[0];
    const connectionId = slot.connectionId
      ?? [...taskProgress].reverse().find((event) => event.connectionId != null)?.connectionId;
    const connectionName = connectionId != null ? connectionNameById.get(connectionId) : undefined;
    const taskLabel = connectionName
      ? `${agentLabel} ${connectionName}`
      : requestSlots.length > 1
        ? `${agentLabel} #${index + 1}${connectionId != null ? ` (connId: ${connectionId})` : ''}`
        : agentLabel;
    const taskInstruction = slot.instruction ?? (requestSlots.length === 1 ? args?.userQuestion : undefined);
    const taskErrorEvent = [...taskProgress].reverse().find((event) => event.phase === 'error');
    const taskErrorMessage = taskErrorEvent?.message;
    const taskError = (requestSlots.length === 1 && isError)
      || !!taskErrorEvent
      || (taskNested?.some((segment) => segment.kind === SegmentKind.TOOL_RUN && !!segment.responseError) ?? false);
    const completionEvent = [...taskProgress].reverse().find((event) => event.phase === 'complete');
    const fallbackResult = responseData && (taskId || requestSlots.length === 1)
      ? resolveSubAgentResult(agentType, responseData, taskId)
      : {};
    const taskComplete = !!completionEvent
      || (!!fallbackResult.resultJson && !taskError && isComplete)
      || (requestSlots.length === 1 && !taskError && isComplete);
    const taskResultSummary = completionEvent?.summaryText ?? fallbackResult.summaryText;
    const taskResultJson = completionEvent?.resultJson ?? fallbackResult.resultJson;
    const taskTimeoutSeconds = (() => {
      const eventTimeoutSeconds = [...taskProgress]
        .reverse()
        .find((event) => typeof event.timeoutSeconds === 'number' && event.timeoutSeconds > 0)
        ?.timeoutSeconds;
      if (typeof eventTimeoutSeconds === 'number' && eventTimeoutSeconds > 0) {
        return eventTimeoutSeconds;
      }
      return slot.timeoutSeconds;
    })();
    const consoleTaskKey = taskKey;
    const isConsoleReady = !!consoleTaskKey;
    const timingKey = consoleTaskKey ?? taskKey;
    const startedAtForTask = taskStartedAt.get(timingKey) ?? startedAt;
    const completedAtForTask = taskComplete || taskError
      ? (taskCompletedAt.get(timingKey) ?? completedAt)
      : undefined;
    const statusText = getSubAgentStatusText({
      isComplete: taskComplete,
      isError: taskError,
      nestedToolRuns: taskNested,
      resultSummary: taskResultSummary,
    });
    const elapsedSeconds = ((completedAtForTask ?? nowMs) - startedAtForTask) / 1000;
    const elapsedText = elapsedSeconds > 0
      ? formatElapsedText(elapsedSeconds, taskTimeoutSeconds)
      : undefined;

    const invocation: SubAgentInvocation = {
      id: `${toolCallId}-${consoleTaskKey ?? taskKey}`,
      taskLabel,
      agentType,
      status: taskError ? 'error' : taskComplete ? 'complete' : 'running',
      errorMessage: taskErrorMessage,
      params: {
        ...(connectionId != null ? { connectionIds: [connectionId] } : {}),
        ...(taskInstruction ? { userQuestion: taskInstruction } : {}),
        taskCount: 1,
        ...(taskTimeoutSeconds ? { timeoutSeconds: taskTimeoutSeconds } : {}),
      },
      progressEvents: taskProgress,
      nestedToolCalls: buildNestedToolCalls(taskNested),
      nestedToolRuns: taskNested,
      resultSummary: taskResultSummary,
      resultJson: taskResultJson,
      startedAt: startedAtForTask,
      completedAt: completedAtForTask,
    };

    return {
      taskKey,
      label: taskLabel,
      agentType,
      consoleTaskKey,
      consoleTabId: consoleTaskKey ? subAgentConsoleTabId(toolCallId, consoleTaskKey) : undefined,
      isConsoleReady,
      isComplete: taskComplete,
      isError: taskError,
      statusText,
      elapsedText,
      nestedToolRuns: taskNested,
      resultSummary: taskResultSummary,
      resultJson: taskResultJson,
      invocation,
    };
  });
}

function formatElapsedText(elapsedSeconds: number, timeoutSeconds?: number): string {
  const elapsedText = `${elapsedSeconds.toFixed(1)}s`;
  if (!timeoutSeconds || timeoutSeconds <= 0) {
    return elapsedText;
  }
  return `${elapsedText} (timeout ${formatTimeoutText(timeoutSeconds)})`;
}

function formatTimeoutText(timeoutSeconds: number): string {
  return `${timeoutSeconds}s`;
}

function TaskSubAgentCard({
  toolCallId,
  conversationId,
  task,
}: {
  toolCallId: string;
  conversationId: number | null;
  task: TaskViewModel;
}) {
  const { handleOpenConsole } = useSubAgentConsoleTab({
    enabled: task.isConsoleReady,
    toolCallId,
    taskKey: task.consoleTaskKey,
    conversationId,
    agentType: task.agentType,
    taskLabel: task.label,
    status: task.isError ? 'error' : task.isComplete ? 'complete' : 'running',
    startedAt: task.invocation.startedAt,
    completedAt: task.invocation.completedAt,
    params: task.invocation.params,
    summary: task.resultSummary,
    resultJson: task.resultJson,
    invocations: [task.invocation],
  });

  return (
    <SingleSubAgentCard
      agentType={task.agentType}
      label={task.label}
      statusText={task.statusText}
      isComplete={task.isComplete}
      isError={task.isError}
      elapsedText={task.elapsedText}
      onOpenConsole={task.isConsoleReady ? handleOpenConsole : undefined}
    />
  );
}

export function SubAgentRunBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
  progressEvents,
  toolCallId,
  nestedToolRuns,
}: SubAgentRunBlockProps) {
  const { conversationId } = useAIAssistantContext();
  const tabs = useWorkspaceStore((state) => state.tabs);
  const reorderTabs = useWorkspaceStore((state) => state.reorderTabs);
  const startTimeRef = useRef(Date.now());
  const completedAtRef = useRef<number | undefined>(undefined);
  const taskStartedAtRef = useRef(new Map<string, number>());
  const taskCompletedAtRef = useRef(new Map<string, number>());
  const fallbackToolCallIdRef = useRef(`subagent-${Date.now()}`);
  const [elapsed, setElapsed] = useState(0);

  const previewArgs = parseCallingSubAgentArgs(parametersData, toolName);
  const stableExplorerArgs = useMemo(
    () => parseStableExplorerArgs(parametersData, toolName),
    [parametersData, toolName]
  );
  const args = toolName === 'callingExplorerSubAgent'
    ? stableExplorerArgs
    : previewArgs;
  const agentType = (previewArgs?.agentType ?? SUB_AGENT_TYPES.EXPLORER) as SubAgentType;
  const agentLabel = SUB_AGENT_LABELS[agentType] ?? agentType;
  const stableToolCallId = toolCallId ?? fallbackToolCallIdRef.current;
  const hasResult = responseData != null && responseData !== '';
  const isComplete = hasResult || responseError;
  const shouldDelayExplorerRendering = toolName === 'callingExplorerSubAgent'
    && !stableExplorerArgs
    && !isComplete;
  const { data: connections = [] } = useQuery({
    queryKey: QUERY_KEY_CONNECTIONS,
    queryFn: () => connectionService.getConnections(),
    staleTime: 5 * 60 * 1000,
  });

  if (isComplete && completedAtRef.current == null) {
    completedAtRef.current = Date.now();
  }

  useEffect(() => {
    if (isComplete) return;
      const interval = setInterval(() => {
      setElapsed((Date.now() - startTimeRef.current) / 1000);
    }, 100);
    return () => clearInterval(interval);
  }, [isComplete]);

  const connectionNameById = useMemo(
    () => new Map(connections.map((connection) => [connection.id, connection.name])),
    [connections]
  );

  const tasks = useMemo(
    () => buildTaskViewModels({
      toolCallId: stableToolCallId,
      agentType,
      agentLabel,
      connectionNameById,
      args,
      startedAt: startTimeRef.current,
      completedAt: completedAtRef.current,
      isComplete,
      isError: responseError,
      progressEvents,
      nestedToolRuns,
      responseData: isComplete ? responseData : undefined,
      nowMs: completedAtRef.current ?? (startTimeRef.current + elapsed * 1000),
      taskStartedAt: taskStartedAtRef.current,
      taskCompletedAt: taskCompletedAtRef.current,
    }),
    [agentLabel, agentType, args, connectionNameById, elapsed, isComplete, nestedToolRuns, progressEvents, responseData, responseError, stableToolCallId]
  );

  useEffect(() => {
    if (shouldDelayExplorerRendering) {
      return;
    }
    const now = Date.now();
    for (const task of tasks) {
      if (!taskStartedAtRef.current.has(task.taskKey)) {
        taskStartedAtRef.current.set(task.taskKey, startTimeRef.current);
      }
      if ((task.isComplete || task.isError) && !taskCompletedAtRef.current.has(task.taskKey)) {
        taskCompletedAtRef.current.set(task.taskKey, now);
      }
    }
  }, [shouldDelayExplorerRendering, tasks]);

  useEffect(() => {
    if (shouldDelayExplorerRendering) {
      return;
    }
    const desiredOrder = tasks
      .map((task) => task.consoleTabId)
      .filter((tabId): tabId is string => !!tabId && tabs.some((tab) => tab.id === tabId));

    if (desiredOrder.length < 2) {
      return;
    }

    const currentOrder = tabs
      .filter((tab) => desiredOrder.includes(tab.id))
      .map((tab) => tab.id);

    if (currentOrder.length !== desiredOrder.length) {
      return;
    }

    for (let index = 0; index < desiredOrder.length; index += 1) {
      if (currentOrder[index] === desiredOrder[index]) {
        continue;
      }
      reorderTabs(desiredOrder[index], currentOrder[index]);
      return;
    }
  }, [reorderTabs, shouldDelayExplorerRendering, tabs, tasks]);

  if (shouldDelayExplorerRendering) {
    return (
      <div className="flex flex-col">
        <SingleSubAgentCard
          agentType={agentType}
          label={agentLabel}
          statusText="Starting Agent..."
          isComplete={false}
          isError={false}
          elapsedText={elapsed > 0 ? formatElapsedText(elapsed) : undefined}
        />
      </div>
    );
  }

  return (
    <div className="flex flex-col">
      {tasks.map((task) => (
        <TaskSubAgentCard
          key={task.taskKey}
          toolCallId={stableToolCallId}
          conversationId={conversationId}
          task={task}
        />
      ))}
    </div>
  );
}
