import { useEffect, useRef } from 'react';
import { TodoListBlock } from './TodoListBlock';
import { McpToolBlock } from './McpToolBlock';
import { ToolRunPending } from './ToolRunPending';
import { ToolRunStreaming } from './ToolRunStreaming';
import { ToolRunExecuting } from './ToolRunExecuting';
import { GenericToolRun } from './GenericToolRun';
import { parseTodoListResponse } from './todoTypes';
import {
  parseAskUserQuestionParameters,
  parseAskUserQuestionResponse,
  normalizeToQuestions,
  type AskUserQuestionPayload,
} from './askUserQuestionTypes';
import { getToolType, ToolType } from './toolTypes';
import { formatParameters } from './formatParameters';
import { useAIAssistantContext } from '../AIAssistantContext';
import { ToolExecutionState } from '../messageListLib/types';
import { useAskQuestionModalStore } from '../../../store/askQuestionModalStore';

/**
 * Helper component to handle opening the question modal only once.
 * Uses useEffect to prevent re-opening on every render.
 */
function AskUserQuestionHandler({
  conversationId,
  askUserPayload,
  submitMessage,
  openModal,
}: {
  conversationId: number;
  askUserPayload: AskUserQuestionPayload;
  submitMessage: (message: string) => Promise<void>;
  openModal: (conversationId: number, questions: any[], onSubmit: (answer: string) => void) => void;
}) {
  const openedRef = useRef(false);

  useEffect(() => {
    // Only open modal once per question instance
    if (!openedRef.current) {
      openedRef.current = true;

      const questions = normalizeToQuestions(askUserPayload);
      const handleSubmit = (answer: string) => {
        submitMessage(answer);
      };

      openModal(conversationId, questions, handleSubmit);
    }
  }, [conversationId, askUserPayload, submitMessage, openModal]);

  return null;
}

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
 * - ASK_USER: AskUserQuestion → AskUserQuestionBlock
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
  const { submitMessage, conversationId } = useAIAssistantContext();
  const openModal = useAskQuestionModalStore((state) => state.open);

  const toolType = getToolType(toolName, serverName);
  const { formattedParameters, isParametersJson } = formatParameters(parametersData);

  // Handle execution state (new approach)
  if (executionState === ToolExecutionState.STREAMING_ARGUMENTS) {
    return <ToolRunStreaming toolName={toolName} partialArguments={parametersData} />;
  }

  if (executionState === ToolExecutionState.EXECUTING) {
    return <ToolRunExecuting toolName={toolName} parametersData={parametersData} />;
  }

  // Backward compatibility: if no executionState but pending=true
  if (pending && !executionState) {
    return <ToolRunPending toolName={toolName} />;
  }

  // Dispatch by tool type
  if (!responseError) {
    switch (toolType) {
      case ToolType.TODO: {
        const todoItems = parseTodoListResponse(responseData)?.items ?? null;
        if (todoItems) {
          return (
            <div className="mb-2">
              <TodoListBlock items={todoItems} />
            </div>
          );
        }
        break;
      }

      case ToolType.ASK_USER: {
        const askUserPayloadFromResponse = parseAskUserQuestionResponse(responseData);
        const askUserPayloadFromParams = parseAskUserQuestionParameters(parametersData);
        const askUserPayload = askUserPayloadFromResponse ?? askUserPayloadFromParams ?? null;
        const askUserSubmittedAnswer =
          askUserPayloadFromResponse == null && askUserPayloadFromParams != null && (responseData ?? '').trim() !== ''
            ? responseData.trim()
            : undefined;

        // If already answered: don't render anything
        if (askUserSubmittedAnswer && askUserPayload) {
          return null;
        }

        // If question received and not answered: render AskUserQuestionHandler
        if (askUserPayload && !askUserSubmittedAnswer && conversationId !== null) {
          return (
            <AskUserQuestionHandler
              conversationId={conversationId}
              askUserPayload={askUserPayload}
              submitMessage={submitMessage}
              openModal={openModal}
            />
          );
        }

        // Any other case: don't render anything
        return null;
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
    }
  }

  return (
    <GenericToolRun
      toolName={toolName}
      formattedParameters={formattedParameters}
      isParametersJson={isParametersJson}
      responseData={responseData}
      responseError={responseError}
    />
  );
}
