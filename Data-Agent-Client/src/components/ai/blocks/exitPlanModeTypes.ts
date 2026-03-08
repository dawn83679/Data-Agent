/** Mirrors backend ExitPlanResult */
export interface ExitPlanPayload {
  title: string;
  steps: PlanStep[];
}

export interface PlanStep {
  order: number;
  description: string;
  sql: string;
  objectName: string;
}

export const EXIT_PLAN_MODE_TOOL_NAME = 'exitPlanMode';

export function isExitPlanModeTool(toolName: string): boolean {
  return toolName === EXIT_PLAN_MODE_TOOL_NAME;
}

export function parseExitPlanPayload(
  responseData: string | null | undefined
): ExitPlanPayload | null {
  if (!responseData?.trim()) return null;
  try {
    const parsed = JSON.parse(responseData.trim()) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;
    if (typeof obj.title !== 'string') return null;
    return {
      title: obj.title,
      steps: Array.isArray(obj.steps) ? obj.steps : [],
    };
  } catch {
    return null;
  }
}

/**
 * Best-effort parser for partial/streaming exitPlanMode JSON.
 * Extracts whatever fields are available from incomplete JSON using regex.
 */
export function parsePartialExitPlanPayload(
  data: string | null | undefined
): ExitPlanPayload | null {
  if (!data?.trim()) return null;

  // Try full parse first
  const full = parseExitPlanPayload(data);
  if (full) return full;

  // Fall back to regex extraction from partial JSON
  const titleMatch = data.match(/"title"\s*:\s*"((?:[^"\\]|\\.)*)"/);
  const title = titleMatch?.[1]?.replace(/\\"/g, '"').replace(/\\n/g, '\n') ?? '';
  if (!title) return null;

  // Extract complete step objects: {"order":...,"description":...,"sql":...,"objectName":...}
  const steps: PlanStep[] = [];
  const stepRegex = /\{\s*"order"\s*:\s*(\d+)\s*,\s*"description"\s*:\s*"((?:[^"\\]|\\.)*)"\s*,\s*"sql"\s*:\s*"((?:[^"\\]|\\.)*)"\s*,\s*"objectName"\s*:\s*"((?:[^"\\]|\\.)*)"\s*\}/g;
  let stepMatch;
  while ((stepMatch = stepRegex.exec(data)) !== null) {
    steps.push({
      order: Number(stepMatch[1]),
      description: unescape(stepMatch[2]),
      sql: unescape(stepMatch[3]),
      objectName: unescape(stepMatch[4]),
    });
  }

  return { title, steps };
}

function unescape(s: string): string {
  return s.replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\"/g, '"').replace(/\\\\/g, '\\');
}

