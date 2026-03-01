import { TodoListBlock } from './TodoListBlock';
import { McpToolBlock } from './McpToolBlock';
import { ToolRunStreaming } from './ToolRunStreaming';
import { ToolRunExecuting } from './ToolRunExecuting';
import { GenericToolRun } from './GenericToolRun';
import { parseTodoListResponse } from './todoTypes';
import {
  parseAskUserQuestionParameters,
  parseAskUserQuestionResponse,
} from './askUserQuestionTypes';
import { parseWriteConfirmPayload } from './writeConfirmTypes';
import { getToolType, ToolType } from './toolTypes';
import { formatParameters } from './formatParameters';
import { ToolExecutionState } from '../messageListLib/types';
import { AskUserQuestionCard } from './AskUserQuestionCard';
import { WriteConfirmCard } from './WriteConfirmCard';

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
  /** MCP server name (e.g., "chart-server") for server-specific rendering */
  serverName?: string;
}

/**
 * Renders a single tool execution result.
 *
 * Tool types:
 * - TODO: TodoWrite → TodoListBlock
 * - ASK_USER: AskUserQuestion → AskUserQuestionCard (Inline)
 * - WRITE_CONFIRM: AskUserConfirm → WriteConfirmCard (Inline)
 * - MCP: External tools (charts, etc.) → McpToolBlock
 * - GENERIC: All other tools (database, etc.) → ToolRunDetail
 */
export function ToolRunBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
  pending = false,
  executionState,
  serverName,
}: ToolRunBlockProps) {
  const toolType = getToolType(toolName, serverName);
  const formattedParameters = formatParameters(parametersData);
  const isInteractive = toolType === ToolType.ASK_USER || toolType === ToolType.WRITE_CONFIRM;

  // 1. Handle Execution Lifecycle States
  if (executionState === ToolExecutionState.STREAMING_ARGUMENTS) {
    if (isInteractive) return <ToolRunExecuting toolName={toolName} parametersData={parametersData} />;
    return <ToolRunStreaming toolName={toolName} partialArguments={parametersData} />;
  }

  if (executionState === ToolExecutionState.EXECUTING || (pending && !executionState)) {
    // Both interactive and non-interactive tools should just pulse while executing/pending
    return <ToolRunExecuting toolName={toolName} parametersData={parametersData} />;
  }

  // 2. Error Fallback (Always generic)
  if (responseError) {
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

      const askUserSubmittedAnswer =
        askUserPayloadFromResponse == null && askUserPayloadFromParams != null && (responseData ?? '').trim() !== ''
          ? responseData.trim()
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
      const writeConfirmPayload = parseWriteConfirmPayload(parametersData) ?? parseWriteConfirmPayload(responseData);
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

    case ToolType.MCP:
      return (
        <McpToolBlock
          toolName={toolName}
          parametersData={parametersData}
          responseData={responseData}
          responseError={responseError}
          serverName={serverName}
        />
      );

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
