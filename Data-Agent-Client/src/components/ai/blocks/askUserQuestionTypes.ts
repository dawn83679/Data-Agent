/**
 * Single question structure matching backend UserQuestion model
 */
export interface SingleQuestion {
  question: string;
  options?: string[];
  freeTextHint?: string | null;
  allowMultiSelect?: boolean; // Controls whether multiple options can be selected (default: false)
}

/**
 * Ask-user-question tool: payload from backend AskUserQuestionTool (JSON).
 * Supports both single-question (legacy) and multi-question formats.
 */
export interface AskUserQuestionPayload {
  // New multi-question format
  questions?: SingleQuestion[];

  // Legacy single-question format (backward compatibility)
  question?: string;
  options?: string[];
  freeTextHint?: string | null;
}

export const ASK_USER_QUESTION_TOOL_NAME = 'askUserQuestion';

export function isAskUserQuestionTool(toolName: string): boolean {
  return toolName === ASK_USER_QUESTION_TOOL_NAME;
}

function parseSingleQuestion(obj: Record<string, unknown>): SingleQuestion | null {
  const question = obj.question;
  if (question == null || typeof question !== 'string') return null;

  let options = Array.isArray(obj.options)
    ? (obj.options as unknown[]).filter((o): o is string => typeof o === 'string')
    : undefined;
  
  // Limit options to maximum 3
  if (options && options.length > 3) {
    options = options.slice(0, 3);
  }
  
  const freeTextHint = obj.freeTextHint != null ? String(obj.freeTextHint) : undefined;
  const allowMultiSelect = typeof obj.allowMultiSelect === 'boolean' ? obj.allowMultiSelect : undefined;

  return { question, options, freeTextHint, allowMultiSelect };
}

function payloadFromObject(obj: Record<string, unknown>): AskUserQuestionPayload | null {
  // Try parsing multi-question format first
  if (Array.isArray(obj.questions)) {
    const questions = (obj.questions as unknown[])
      .map((q) => {
        if (!q || typeof q !== 'object') return null;
        return parseSingleQuestion(q as Record<string, unknown>);
      })
      .filter((q): q is SingleQuestion => q !== null);

    if (questions.length > 0) {
      return { questions };
    }
  }

  // Fallback to legacy single-question format
  const singleQuestion = parseSingleQuestion(obj);
  if (singleQuestion) {
    return {
      question: singleQuestion.question,
      options: singleQuestion.options,
      freeTextHint: singleQuestion.freeTextHint,
    };
  }

  return null;
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

    // Handle direct array format: [{question: "...", options: [...]}, ...]
    if (Array.isArray(parsed)) {
      const questions = parsed
        .map((q) => {
          if (!q || typeof q !== 'object') return null;
          return parseSingleQuestion(q as Record<string, unknown>);
        })
        .filter((q): q is SingleQuestion => q !== null);

      if (questions.length > 0) {
        return { questions };
      }
      return null;
    }

    // Handle object format: {questions: [{...}]} or {question: "...", options: [...]}
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

/**
 * Normalize payload to array of questions for easier component usage.
 * Handles both multi-question and legacy single-question formats.
 */
export function normalizeToQuestions(payload: AskUserQuestionPayload | null): SingleQuestion[] {
  if (!payload) return [];

  // Multi-question format
  if (payload.questions && payload.questions.length > 0) {
    return payload.questions;
  }

  // Legacy single-question format
  if (payload.question) {
    return [{
      question: payload.question,
      options: payload.options,
      freeTextHint: payload.freeTextHint,
      allowMultiSelect: (payload as any).allowMultiSelect,
    }];
  }

  return [];
}
