import { useEffect, useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { parseCallingSubAgentArgs, resolveSubAgentResult, SUB_AGENT_TYPES } from './subAgentTypes';
import type { SubAgentProgressEvent, SubAgentType } from './subAgentTypes';
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

function getStableTaskKey(options: {
  taskId?: string;
  taskCount: number;
}): string | undefined {
  const { taskId, taskCount } = options;
  if (taskId) return `task-${taskId}`;
  if (taskCount <= 1) return 'single';
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

  const connectionIds = args?.connectionIds ?? [];
  const taskCount = Math.max(args?.taskCount ?? 0, connectionIds.length);
  const orderedTaskIds: string[] = [];
  const seenTaskIds = new Set<string>();
  const toolSegments = nestedToolRuns?.filter((segment): segment is ToolRunSegment => segment.kind === SegmentKind.TOOL_RUN) ?? [];

  for (const event of progressEvents ?? []) {
    if (!event.taskId || seenTaskIds.has(event.taskId)) continue;
    seenTaskIds.add(event.taskId);
    orderedTaskIds.push(event.taskId);
  }
  for (const segment of toolSegments) {
    if (!segment.subAgentTaskId || seenTaskIds.has(segment.subAgentTaskId)) continue;
    seenTaskIds.add(segment.subAgentTaskId);
    orderedTaskIds.push(segment.subAgentTaskId);
  }

  const taskIdToKey = new Map<string, string>();
  const taskIdToConnectionId = new Map<string, number | undefined>();
  orderedTaskIds.forEach((taskId, index) => {
    const eventConnectionId = (progressEvents ?? [])
      .find((event) => event.taskId === taskId && event.connectionId != null)
      ?.connectionId;
    const connectionId = eventConnectionId ?? (taskCount <= 1 ? connectionIds[index] : undefined);
    const taskKey = connectionId != null ? `conn-${connectionId}` : `task-${taskId}`;
    taskIdToKey.set(taskId, taskKey);
    taskIdToConnectionId.set(taskId, connectionId);
  });

  const orderedKeys: string[] = [];
  const keySet = new Set<string>();
  const ensureKeyOrder = (taskKey: string) => {
    if (!keySet.has(taskKey)) {
      keySet.add(taskKey);
      orderedKeys.push(taskKey);
    }
  };

  if (orderedTaskIds.length === 0 && taskCount > 1) {
    const fallbackCount = taskCount;
    for (let index = 0; index < fallbackCount; index += 1) {
      const connectionId = connectionIds[index];
      const taskKey = connectionId != null ? `conn-${connectionId}` : `slot-${index + 1}`;
      ensureKeyOrder(taskKey);
    }
  }

  orderedTaskIds.forEach((taskId) => {
    ensureKeyOrder(taskIdToKey.get(taskId) ?? `task-${taskId}`);
  });

  if (orderedKeys.length === 0) {
    orderedKeys.push('single');
  }

  const progressByKey = new Map<string, SubAgentProgressEvent[]>();
  const nestedByKey = new Map<string, Segment[]>();

  for (const event of progressEvents ?? []) {
    if (!event.taskId && taskCount > 1) continue;
    const taskKey = event.taskId ? (taskIdToKey.get(event.taskId) ?? `task-${event.taskId}`) : orderedKeys[0];
    const current = progressByKey.get(taskKey) ?? [];
    current.push(event);
    progressByKey.set(taskKey, current);
    ensureKeyOrder(taskKey);
  }

  for (const segment of toolSegments) {
    if (!segment.subAgentTaskId && taskCount > 1) continue;
    const taskKey = segment.subAgentTaskId
      ? (taskIdToKey.get(segment.subAgentTaskId) ?? `task-${segment.subAgentTaskId}`)
      : orderedKeys[0];
    const current = nestedByKey.get(taskKey) ?? [];
    current.push(segment);
    nestedByKey.set(taskKey, current);
    ensureKeyOrder(taskKey);
  }

  return orderedKeys.map((taskKey, index) => {
    const taskProgress = progressByKey.get(taskKey) ?? [];
    const taskNested = nestedByKey.get(taskKey);
    const taskId = orderedTaskIds.find((candidateTaskId) => (taskIdToKey.get(candidateTaskId) ?? `task-${candidateTaskId}`) === taskKey);
    const connectionId = (() => {
      if (taskKey.startsWith('conn-')) {
        const parsed = Number(taskKey.slice(5));
        return Number.isFinite(parsed) ? parsed : undefined;
      }
      return taskIdToConnectionId.get(taskId ?? orderedTaskIds[index] ?? '') ?? connectionIds[index];
    })();
    const connectionName = connectionId != null ? connectionNameById.get(connectionId) : undefined;
    const taskLabel = connectionName
      ? `${agentLabel} ${connectionName}`
      : orderedKeys.length > 1
        ? `${agentLabel} #${index + 1}${connectionId != null ? ` (connId: ${connectionId})` : ''}`
        : agentLabel;
    const taskInstruction = taskId
      ? args?.taskInstructions?.[orderedTaskIds.indexOf(taskId)]
      : orderedKeys.length === 1
        ? (args?.userQuestion ?? args?.taskInstructions?.[0])
        : args?.taskInstructions?.[index];
    const taskErrorEvent = [...taskProgress].reverse().find((event) => event.phase === 'error');
    const taskErrorMessage = taskErrorEvent?.message;
    const taskError = (orderedKeys.length === 1 && isError)
      || !!taskErrorEvent
      || (taskNested?.some((segment) => segment.kind === SegmentKind.TOOL_RUN && !!segment.responseError) ?? false);
    const completionEvent = [...taskProgress].reverse().find((event) => event.phase === 'complete');
    const fallbackResult = responseData && (taskId || orderedKeys.length === 1)
      ? resolveSubAgentResult(agentType, responseData, taskId)
      : {};
    const taskComplete = !!completionEvent
      || (!!fallbackResult.resultJson && !taskError && isComplete)
      || (orderedKeys.length === 1 && !taskError && isComplete);
    const taskResultSummary = completionEvent?.summaryText ?? fallbackResult.summaryText;
    const taskResultJson = completionEvent?.resultJson ?? fallbackResult.resultJson;
    const consoleTaskKey = getStableTaskKey({
      taskId,
      taskCount,
    });
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
    const elapsedText = elapsedSeconds > 0 ? `${elapsedSeconds.toFixed(1)}s` : undefined;

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
  const closeTab = useWorkspaceStore((state) => state.closeTab);
  const reorderTabs = useWorkspaceStore((state) => state.reorderTabs);
  const startTimeRef = useRef(Date.now());
  const completedAtRef = useRef<number | undefined>(undefined);
  const taskStartedAtRef = useRef(new Map<string, number>());
  const taskCompletedAtRef = useRef(new Map<string, number>());
  const fallbackToolCallIdRef = useRef(`subagent-${Date.now()}`);
  const [elapsed, setElapsed] = useState(0);

  const args = parseCallingSubAgentArgs(parametersData, toolName);
  const agentType = (args?.agentType ?? SUB_AGENT_TYPES.EXPLORER) as SubAgentType;
  const agentLabel = SUB_AGENT_LABELS[agentType] ?? agentType;
  const stableToolCallId = toolCallId ?? fallbackToolCallIdRef.current;
  const hasResult = responseData != null && responseData !== '';
  const isComplete = hasResult || responseError;
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

  useEffect(() => {
    const now = Date.now();
    for (const event of progressEvents ?? []) {
      const taskKey = getStableTaskKey({
        taskId: event.taskId,
        taskCount: args?.taskCount ?? 0,
      }) ?? (event.connectionId != null ? `conn-${event.connectionId}` : 'single');

      if (!taskStartedAtRef.current.has(taskKey)) {
        taskStartedAtRef.current.set(taskKey, now);
      }
      if ((event.phase === 'complete' || event.phase === 'error') && !taskCompletedAtRef.current.has(taskKey)) {
        taskCompletedAtRef.current.set(taskKey, now);
      }
    }
  }, [args?.taskCount, progressEvents]);

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
    const aggregateTabId = `subagent-console-${stableToolCallId}`;
    const expectedTaskConsoleIds = new Set(
      tasks
        .map((task) => task.consoleTabId)
        .filter((tabId): tabId is string => !!tabId)
    );

    for (const tab of tabs) {
      if (tab.id === aggregateTabId) {
        closeTab(tab.id);
        continue;
      }
      if (!tab.id.startsWith(`${aggregateTabId}-`)) continue;
      if (expectedTaskConsoleIds.has(tab.id)) continue;
      closeTab(tab.id);
    }
  }, [closeTab, stableToolCallId, tabs, tasks]);

  useEffect(() => {
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
  }, [reorderTabs, tabs, tasks]);

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
