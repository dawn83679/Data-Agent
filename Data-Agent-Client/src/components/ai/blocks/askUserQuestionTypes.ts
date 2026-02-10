/**
 * Ask-user-question tool: payload from backend AskUserQuestionTool (JSON).
 * Matches: { question: string, options?: string[], freeTextHint?: string }.
 */
export interface AskUserQuestionPayload {
  question: string;
  options?: string[];
  freeTextHint?: string | null;
}

export const ASK_USER_QUESTION_TOOL_NAME = 'askUserQuestion';

export function isAskUserQuestionTool(toolName: string): boolean {
  return toolName === ASK_USER_QUESTION_TOOL_NAME;
}

function payloadFromObject(obj: Record<string, unknown>): AskUserQuestionPayload | null {
  const question = obj.question;
  if (question == null || typeof question !== 'string') return null;
  const options = Array.isArray(obj.options)
    ? (obj.options as unknown[]).filter((o): o is string => typeof o === 'string').slice(0, 3)
    : undefined;
  const freeTextHint = obj.freeTextHint != null ? String(obj.freeTextHint) : undefined;
  return { question, options, freeTextHint };
}

/**
 * Parse tool response (question JSON from tool). Returns null on parse error or missing question.
 */
export function parseAskUserQuestionResponse(responseData: string | null | undefined): AskUserQuestionPayload | null {
  if (responseData == null) return null;
  const trimmed = responseData.trim();
  if (trimmed === '') return null;
  try {
    const parsed = JSON.parse(trimmed) as unknown;
    if (!parsed || typeof parsed !== 'object') return null;
    return payloadFromObject(parsed as Record<string, unknown>);
  } catch {
    return null;
  }
}

/**
 * Parse TOOL_CALL arguments (question JSON) for askUserQuestion. Used when TOOL_RESULT is the user's answer (plain text).
 */
export function parseAskUserQuestionParameters(parametersData: string | null | undefined): AskUserQuestionPayload | null {
  if (parametersData == null) return null;
  const trimmed = parametersData.trim();
  if (trimmed === '') return null;
  try {
    let parsed: unknown = JSON.parse(trimmed);
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (!parsed || typeof parsed !== 'object') return null;
    return payloadFromObject(parsed as Record<string, unknown>);
  } catch {
    return null;
  }
}
