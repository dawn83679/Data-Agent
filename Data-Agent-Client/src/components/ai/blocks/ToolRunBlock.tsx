import { useEffect, useRef } from 'react';
import { ListTodo } from 'lucide-react';
import { TodoListBlock } from './TodoListBlock';
import { ToolRunStreaming } from './ToolRunStreaming';
import { ToolRunExecuting } from './ToolRunExecuting';
import { GenericToolRun } from './GenericToolRun';
import { ChartToolBlock } from './ChartToolBlock';
import { SkillToolBlock } from './SkillToolBlock';
import { parseTodoListResponse } from './todoTypes';
import {
  parseAskUserQuestionParameters,
  parseAskUserQuestionResponse,
} from './askUserQuestionTypes';
import { parseWriteConfirmPayload, parseWriteConfirmToken } from './writeConfirmTypes';
import { parseExitPlanPayload, parsePartialExitPlanPayload, type ExitPlanPayload } from './exitPlanModeTypes';
import { getToolType, ToolType } from './toolTypes';
import { formatParameters } from './formatParameters';
import { ToolExecutionState } from '../messageListLib/types';
import type { Segment } from '../messageListLib/types';
import { SubAgentRunBlock } from './SubAgentRunBlock';
import type { SubAgentProgressEvent } from './subAgentTypes';
import { AskUserQuestionCard } from './AskUserQuestionCard';
import { WriteConfirmCard } from './WriteConfirmCard';
import { ExitPlanModeCard } from './ExitPlanModeCard';
import { ThoughtBlock } from './ThoughtBlock';
import { useWorkspaceStore } from '../../../store/workspaceStore';
import { useAIAssistantContext } from '../AIAssistantContext';

export interface ToolRunBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  /** True when tool execution failed (backend ToolExecution.hasFailed()). */
  responseError?: boolean;
  /** True while waiting for TOOL_RESULT (no icon, tool name blinks). */
  pending?: boolean;
  /** Execution state: streaming arguments, executing, or complete. */
  executionState?: ToolExecutionState;
  /** Tool call id from TOOL_CALL block; used for pairing and retry dedupe. */
  toolCallId?: string;
  /** Whether this tool run belongs to current streaming turn and can auto retry. */
  allowAutoRetry?: boolean;
  /** SubAgent progress events collected from STATUS blocks (exploreSchema/generateSqlPlan only). */
  progressEvents?: SubAgentProgressEvent[];
  /** Nested tool runs from SubAgent (getEnvironmentOverview, searchObjects, etc.). */
  nestedToolRuns?: Segment[];
}

/**
 * Renders a single tool execution result.
 *
 * Tool types:
 * - TODO: TodoWrite → TodoListBlock
 * - ASK_USER: AskUserQuestion → AskUserQuestionCard (Inline)
 * - WRITE_CONFIRM: AskUserConfirm → WriteConfirmCard (Inline)
 * - GENERIC: All other tools (database, etc.) → ToolRunDetail
 */
export function ToolRunBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
  pending = false,
  executionState,
  toolCallId,
  allowAutoRetry = false,
  progressEvents,
  nestedToolRuns,
}: ToolRunBlockProps) {
  const toolType = getToolType(toolName);
  const formattedParameters = formatParameters(parametersData);
  const isInteractive = toolType === ToolType.ASK_USER || toolType === ToolType.WRITE_CONFIRM;

  // 0. exploreSchema / generateSqlPlan — render as SubAgent card at every lifecycle stage
  if (toolType === ToolType.CALLING_SUB_AGENT) {
    return (
      <SubAgentRunBlock
        toolName={toolName}
        parametersData={parametersData}
        responseData={responseData}
        responseError={responseError}
        progressEvents={progressEvents}
        toolCallId={toolCallId}
        nestedToolRuns={nestedToolRuns}
      />
    );
  }

  // 0a. Thinking tool — render as thought block at every lifecycle stage
  if (toolType === ToolType.THINKING) {
    const isStreaming = executionState === ToolExecutionState.STREAMING_ARGUMENTS
      || executionState === ToolExecutionState.EXECUTING
      || (pending && !executionState);
    const analysis = extractThinkingAnalysis(parametersData);
    if (!analysis) return null;
    return <ThoughtBlock data={analysis} defaultExpanded={isStreaming} />;
  }

  // 0b. EnterPlanMode — compact transition indicator + auto-switch frontend mode
  if (toolType === ToolType.ENTER_PLAN) {
    const isStreaming = executionState === ToolExecutionState.STREAMING_ARGUMENTS
      || executionState === ToolExecutionState.EXECUTING
      || (pending && !executionState);

    return <EnterPlanModeIndicator isStreaming={isStreaming} />;
  }

  // 0c. ExitPlanMode — stream into plan tab, render action card in chat when complete
  if (toolType === ToolType.EXIT_PLAN) {
    const isStreaming = executionState === ToolExecutionState.STREAMING_ARGUMENTS
      || executionState === ToolExecutionState.EXECUTING
      || (pending && !executionState);

    if (isStreaming) {
      return <PlanStreamHandler parametersData={parametersData} />;
    }
    const payload = parseExitPlanPayload(parametersData);
    if (!payload) return null;
    return <PlanCompleteHandler payload={payload} />;
  }

  // 1. Handle Execution Lifecycle States
  if (executionState === ToolExecutionState.STREAMING_ARGUMENTS) {
    if (isInteractive) {
      return (
        <ToolRunExecuting
          toolName={toolName}
          parametersData={parametersData}
        />
      );
    }
    return (
      <ToolRunStreaming
        toolName={toolName}
        partialArguments={parametersData}
      />
    );
  }

  if (executionState === ToolExecutionState.EXECUTING || (pending && !executionState)) {
    // Both interactive and non-interactive tools should just pulse while executing/pending
    return (
      <ToolRunExecuting
        toolName={toolName}
        parametersData={parametersData}
      />
    );
  }

  // 2. Error fallback for non-chart tools.
  // Chart errors should still go through ChartToolBlock to trigger auto-feedback.
  if (responseError && toolType !== ToolType.CHART) {
    return (
      <GenericToolRun
        toolName={toolName}
        formattedParameters={formattedParameters}
        responseData={responseData}
        responseError={responseError}
      />
    );
  }

  // 3. Dispatch Completed Tool Rendering by Category
  switch (toolType) {
    // CATEGORY A: Interactive Tools (Inline Cards)
    case ToolType.ASK_USER: {
      const askUserPayloadFromResponse = parseAskUserQuestionResponse(responseData);
      const askUserPayloadFromParams = parseAskUserQuestionParameters(parametersData);
      const askUserPayload = askUserPayloadFromResponse ?? askUserPayloadFromParams ?? null;

      // Detect if responseData is a user's submitted answer (not a minimal tool summary).
      // Minimal summaries like "2 question(s) presented to user." are NOT user answers.
      const responseText = (responseData ?? '').trim();
      const isMinimalToolSummary = /^\d+ question\(s\) presented to user\.$/.test(responseText);
      const askUserSubmittedAnswer =
        askUserPayloadFromResponse == null && askUserPayloadFromParams != null && responseText !== '' && !isMinimalToolSummary
          ? responseText
          : undefined;

      if (!askUserPayload) return null;

      // If already answered but not loading, or if we need to show the form
      // We always render the card (interactive if questions to answer, static if answered)
      return (
        <AskUserQuestionCard
          askUserPayload={askUserPayload}
          submittedAnswer={askUserSubmittedAnswer}
        />
      );
    }

    case ToolType.WRITE_CONFIRM: {
      // Display data (sql, explanation) is in tool call arguments;
      // confirmationToken is in tool result (generated server-side).
      const fromParams = parseWriteConfirmPayload(parametersData);
      const fromResponse = parseWriteConfirmPayload(responseData);
      const tokenOnly = parseWriteConfirmToken(responseData);

      // Merge: prefer params for display data, fill in token from response
      let writeConfirmPayload = fromResponse ?? fromParams ?? null;
      if (fromParams && tokenOnly) {
        writeConfirmPayload = { ...fromParams, confirmationToken: tokenOnly.confirmationToken, expiresInSeconds: tokenOnly.expiresInSeconds };
      }
      if (!writeConfirmPayload) return null;

      // Determine if it was already answered checking responseData text if parameters held the token
      let submittedAnswer: string | undefined = undefined;
      if (responseData && (responseData.includes('confirmed') || responseData.includes('cancelled'))) {
        submittedAnswer = responseData;
      }

      return (
        <WriteConfirmCard
          payload={writeConfirmPayload}
          submittedAnswer={submittedAnswer}
        />
      );
    }

    // CATEGORY B: Specialized Content Presentation
    case ToolType.TODO: {
      const todoItems = parseTodoListResponse(responseData)?.items ?? null;
      if (!todoItems) return null;
      return (
        <div className="mb-2">
          <TodoListBlock items={todoItems} />
        </div>
      );
    }

    case ToolType.CHART:
      return (
        <ChartToolBlock
          toolName={toolName}
          parametersData={parametersData}
          responseData={responseData}
          responseError={responseError}
          toolCallId={toolCallId}
          allowAutoRetry={allowAutoRetry}
        />
      );

    case ToolType.SKILL: {
      const parsedSkillName = extractSkillName(parametersData);
      return (
        <SkillToolBlock
          skillName={parsedSkillName}
          responseData={responseData}
          responseError={responseError}
        />
      );
    }

    // CATEGORY C: Generic Data Fetching / DB Tools
    case ToolType.GENERIC:
    default:
      return (
        <GenericToolRun
          toolName={toolName}
          formattedParameters={formattedParameters}
          responseData={responseData}
          responseError={responseError}
        />
      );
  }
}

/** Derive a stable tab ID from a plan title. */
export function planTabId(title: string): string {
  return `plan-${title.replace(/\s+/g, '-').toLowerCase().slice(0, 40)}`;
}

/**
 * During streaming: parse partial JSON, open/update the plan tab,
 * and show a compact streaming indicator in the chat sidebar.
 */
function PlanStreamHandler({ parametersData }: { parametersData: string }) {
  const openedRef = useRef(false);

  const partial = parsePartialExitPlanPayload(parametersData);

  useEffect(() => {
    if (!partial) return;
    const tabId = planTabId(partial.title);
    const ws = useWorkspaceStore.getState();

    if (!openedRef.current) {
      // First render — open the tab
      openedRef.current = true;
      ws.openTab({
        id: tabId,
        name: partial.title,
        type: 'plan',
        metadata: { planPayload: partial },
      });
    } else {
      // Subsequent renders — update the payload
      ws.updatePlanPayload(tabId, partial);
    }
  }, [parametersData]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!partial) return null;

  return (
    <div className="mb-2 px-3 py-2 rounded-lg border border-amber-300 dark:border-amber-700 bg-amber-50/50 dark:bg-amber-900/10 flex items-center gap-2">
      <ListTodo className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0 animate-pulse" />
      <span className="text-[12px] theme-text-primary flex-1 truncate">
        Generating plan: <span className="font-medium">{partial.title}</span>
        <span className="ml-1.5 theme-text-secondary">
          ({partial.steps.length} step{partial.steps.length !== 1 ? 's' : ''})
        </span>
      </span>
    </div>
  );
}

/**
 * On complete: finalize the plan tab with full payload,
 * render the ExitPlanModeCard (risks + action buttons) in chat + "View Plan" link.
 */
function PlanCompleteHandler({ payload }: { payload: ExitPlanPayload }) {
  const finalizedRef = useRef(false);
  const { latestPlanTabId } = useAIAssistantContext();
  const tabId = planTabId(payload.title);

  useEffect(() => {
    if (finalizedRef.current) return;
    finalizedRef.current = true;

    // Only auto-open the tab if:
    // - latestPlanTabId is undefined (no context = streaming / live session)
    // - OR this is the latest plan in the conversation
    const shouldAutoOpen = latestPlanTabId === undefined || latestPlanTabId === tabId;

    const ws = useWorkspaceStore.getState();
    const existingTab = ws.tabs.find((t) => t.id === tabId);
    if (existingTab) {
      ws.updatePlanPayload(tabId, payload);
    } else if (shouldAutoOpen) {
      ws.openTab({
        id: tabId,
        name: payload.title,
        type: 'plan',
        metadata: { planPayload: payload },
      });
    }
  }, [tabId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleViewPlan = () => {
    const ws = useWorkspaceStore.getState();
    const existingTab = ws.tabs.find((t) => t.id === tabId);
    if (existingTab) {
      ws.switchTab(tabId);
    } else {
      // Create tab on demand for non-latest plans
      ws.openTab({
        id: tabId,
        name: payload.title,
        type: 'plan',
        metadata: { planPayload: payload },
      });
    }
  };

  return (
    <div className="flex flex-col gap-2 mb-2">
      {/* "View Plan" link */}
      <div className="px-3 py-2 rounded-lg border border-amber-300 dark:border-amber-700 bg-amber-50/50 dark:bg-amber-900/10 flex items-center gap-2">
        <ListTodo className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0" />
        <span className="text-[12px] theme-text-primary flex-1 truncate">
          Plan: <span className="font-medium">{payload.title}</span>
          <span className="ml-1.5 theme-text-secondary">
            ({payload.steps.length} step{payload.steps.length !== 1 ? 's' : ''})
          </span>
        </span>
        <button
          type="button"
          onClick={handleViewPlan}
          className="text-[11px] text-amber-600 dark:text-amber-400 hover:underline shrink-0"
        >
          View Plan
        </button>
      </div>
      {/* Action buttons card */}
      <ExitPlanModeCard planTabId={tabId} />
    </div>
  );
}

/**
 * EnterPlanMode indicator: shows transition status and auto-switches the frontend
 * agent mode selector to Plan when the tool execution completes.
 */
function EnterPlanModeIndicator({ isStreaming }: { isStreaming: boolean }) {
  const switchedRef = useRef(false);
  const { agentState } = useAIAssistantContext();

  useEffect(() => {
    // Switch frontend mode to Plan once the tool finishes executing
    if (!isStreaming && !switchedRef.current) {
      switchedRef.current = true;
      agentState.setAgent('Plan');
    }
  }, [isStreaming, agentState]);

  return (
    <div className="mb-2 px-3 py-2 rounded-lg border border-amber-300 dark:border-amber-700
                    bg-amber-50/50 dark:bg-amber-900/10 flex items-center gap-2">
      <ListTodo className={`w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0 ${isStreaming ? 'animate-pulse' : ''}`} />
      <span className="text-[12px] theme-text-primary">
        {isStreaming ? 'Switching to Plan mode...' : 'Switched to Plan mode'}
      </span>
    </div>
  );
}

/** Extract the "skillName" field from activateSkill tool call arguments. */
function extractSkillName(parametersData: string): string {
  if (!parametersData) return 'unknown';
  try {
    let parsed: unknown = JSON.parse(parametersData);
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const obj = parsed as Record<string, unknown>;
      if (typeof obj.skillName === 'string') return obj.skillName;
      if (obj.request && typeof obj.request === 'object') {
        const req = obj.request as Record<string, unknown>;
        if (typeof req.skillName === 'string') return req.skillName;
      }
    }
  } catch {
    const match = parametersData.match(/"skillName"\s*:\s*"([^"]+)"/);
    if (match?.[1]) return match[1];
  }
  return 'unknown';
}

/** Extract the "analysis" field from thinking tool call arguments. */
function extractThinkingAnalysis(parametersData: string): string | null {
  if (!parametersData) return null;
  try {
    let parsed: unknown = JSON.parse(parametersData);
    // Handle double-encoded JSON string
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;
    // Try nested request.analysis first, then top-level analysis
    if (obj.request && typeof obj.request === 'object') {
      const req = obj.request as Record<string, unknown>;
      if (typeof req.analysis === 'string' && req.analysis) return req.analysis;
    }
    if (typeof obj.analysis === 'string' && obj.analysis) return obj.analysis;
    // Fallback: if only goal is available during streaming, show that
    if (typeof obj.goal === 'string' && obj.goal) return obj.goal;
    if (obj.request && typeof obj.request === 'object') {
      const req = obj.request as Record<string, unknown>;
      if (typeof req.goal === 'string' && req.goal) return req.goal;
    }
    return null;
  } catch {
    // During streaming, parametersData may be partial/incomplete JSON.
    // Try to extract analysis text with regex as fallback.
    const match = parametersData.match(/"analysis"\s*:\s*"((?:[^"\\]|\\.)*)"/);
    if (match?.[1]) return match[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
    const goalMatch = parametersData.match(/"goal"\s*:\s*"((?:[^"\\]|\\.)*)"/);
    if (goalMatch?.[1]) return goalMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
    return null;
  }
}
