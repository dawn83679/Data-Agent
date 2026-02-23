import { useTranslation } from 'react-i18next';
import { TodoListBlock } from './TodoListBlock';
import { AskUserQuestionBlock } from './AskUserQuestionBlock';
import { McpToolBlock } from './McpToolBlock';
import { ToolRunPending } from './ToolRunPending';
import { ToolRunStreaming } from './ToolRunStreaming';
import { ToolRunExecuting } from './ToolRunExecuting';
import { GenericToolRun } from './GenericToolRun';
import { parseTodoListResponse } from './todoTypes';
import {
  parseAskUserQuestionParameters,
  parseAskUserQuestionResponse,
} from './askUserQuestionTypes';
import { getToolType, ToolType } from './toolTypes';
import { formatParameters } from './formatParameters';
import { useAIAssistantContext } from '../AIAssistantContext';
import { ToolExecutionState } from '../messageListLib/types';

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
  /** LangChain4j tool call id; when set with askUserQuestion, submitToolAnswer is used instead of submitMessage. */
  toolCallId?: string;
  /** When true, this block has later segments in the same message (e.g. user already answered and model continued). */
  hasSegmentsAfter?: boolean;
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
  toolCallId,
  hasSegmentsAfter = false,
  serverName,
}: ToolRunBlockProps) {
  const { t } = useTranslation();
  const { submitMessage, submitToolAnswer, isLoading } = useAIAssistantContext();

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

        if (askUserPayload) {
          const useToolAnswer = !!toolCallId;
          const blockDisabled = isLoading || (useToolAnswer && hasSegmentsAfter);
          return (
            <div className="mb-2">
              <AskUserQuestionBlock
                payload={askUserPayload}
                disabled={blockDisabled}
                submittedAnswer={askUserSubmittedAnswer}
                onSubmit={(answer) =>
                  useToolAnswer
                    ? submitToolAnswer(toolCallId, answer)
                    : submitMessage(t('ai.askUserQuestion.answerPrefix') + answer)
                }
              />
            </div>
          );
        }
        break;
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
