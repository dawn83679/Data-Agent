import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CheckCircle, ChevronDown, ChevronRight, XCircle } from 'lucide-react';
import { TodoListBlock } from './TodoListBlock';
import { AskUserQuestionBlock } from './AskUserQuestionBlock';
import { isTodoTool, parseTodoListResponse } from './todoTypes';
import {
  isAskUserQuestionTool,
  parseAskUserQuestionParameters,
  parseAskUserQuestionResponse,
} from './askUserQuestionTypes';
import { ToolRunDetail } from './ToolRunDetail';
import { useAIAssistantContext } from '../AIAssistantContext';
import { cn } from '../../../lib/utils';
import { TOOL_RUN_LABEL_FAILED, TOOL_RUN_LABEL_RAN } from '../../../constants/chat';

export interface ToolRunBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  /** True when tool execution failed (backend ToolExecution.hasFailed()). */
  responseError?: boolean;
  /** True while waiting for TOOL_RESULT (no icon, tool name blinks). */
  pending?: boolean;
  /** LangChain4j tool call id; when set with askUserQuestion, submitToolAnswer is used instead of submitMessage. */
  toolCallId?: string;
  /** When true, this block has later segments in the same message (e.g. user already answered and model continued). */
  hasSegmentsAfter?: boolean;
}

/** One tool run: pending = tool name only (blink); completed = icon + Ran/Failed + expandable details. Todo tools render TodoListBlock; askUserQuestion renders AskUserQuestionBlock. */
export function ToolRunBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
  pending = false,
  toolCallId,
  hasSegmentsAfter = false,
}: ToolRunBlockProps) {
  const { t } = useTranslation();
  const { submitMessage, submitToolAnswer, isLoading } = useAIAssistantContext();
  const todoItems = !responseError && isTodoTool(toolName) ? parseTodoListResponse(responseData)?.items ?? null : null;
  const isTodoResult = todoItems !== null;
  const isAskUserTool = !responseError && isAskUserQuestionTool(toolName);
  const askUserPayloadFromResponse = isAskUserTool ? parseAskUserQuestionResponse(responseData) : null;
  const askUserPayloadFromParams = isAskUserTool ? parseAskUserQuestionParameters(parametersData) : null;
  const askUserPayload = askUserPayloadFromResponse ?? askUserPayloadFromParams ?? null;
  const askUserSubmittedAnswer =
    isAskUserTool && askUserPayloadFromResponse == null && askUserPayloadFromParams != null && (responseData ?? '').trim() !== ''
      ? responseData.trim()
      : undefined;
  const isAskUserResult = askUserPayload !== null;
  const [collapsed, setCollapsed] = useState(() => !isAskUserQuestionTool(toolName));

  const { formattedParameters, isParametersJson } = (() => {
    if (!parametersData?.trim()) return { formattedParameters: parametersData, isParametersJson: false };
    try {
      const parsed = JSON.parse(parametersData);
      return { formattedParameters: JSON.stringify(parsed, null, 2), isParametersJson: true };
    } catch {
      return { formattedParameters: parametersData, isParametersJson: false };
    }
  })();

  if (pending) {
    return (
      <div className="mb-2 text-xs opacity-70 theme-text-secondary">
        <div className="w-full py-1.5 flex items-center gap-2 text-left rounded theme-text-primary">
          <span className="font-medium animate-pulse">{toolName}</span>
        </div>
      </div>
    );
  }

  if (isTodoResult) {
    return (
      <div className="mb-2">
        <TodoListBlock items={todoItems} />
      </div>
    );
  }

  if (isAskUserResult) {
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

  return (
    <div
      className={cn(
        'mb-2 text-xs rounded transition-colors',
        collapsed ? 'opacity-70 theme-text-secondary' : 'opacity-100 theme-text-primary'
      )}
    >
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className="w-full py-1.5 flex items-center gap-2 text-left rounded transition-colors theme-text-primary hover:bg-black/5 dark:hover:bg-white/5"
      >
        {responseError ? (
          <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" aria-label="Failed" />
        ) : (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" aria-hidden />
        )}
        <span className="font-medium">
          {responseError ? TOOL_RUN_LABEL_FAILED : TOOL_RUN_LABEL_RAN}
          {toolName}
        </span>
        <span className={cn('ml-auto shrink-0', collapsed ? 'opacity-60' : 'opacity-80')}>
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <ToolRunDetail
          formattedParameters={formattedParameters}
          isParametersJson={isParametersJson}
          responseData={responseData}
        />
      )}
    </div>
  );
}
