/**
 * Todo status enum. Matches backend TodoStatusEnum.
 */
export enum TodoStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
}

const TODO_STATUS_VALUES = new Set<string>(Object.values(TodoStatus));

export function normalizeTodoStatus(status: string | undefined): TodoStatus | undefined {
  if (status == null || status === '') return undefined;
  const upper = status.toUpperCase();
  return TODO_STATUS_VALUES.has(upper) ? (upper as TodoStatus) : undefined;
}

/**
 * Todo item shape for display. Matches backend Todo.
 * Each item's status is returned by the server: NOT_STARTED | IN_PROGRESS | PAUSED | COMPLETED.
 */
export interface TodoItem {
  title: string;
  description?: string;
  /** From backend TodoTool response; one of TodoStatus. */
  status?: string;
  priority?: string;
  createdAt?: string;
  updatedAt?: string;
}

export function isTodoCompleted(status: string | undefined): boolean {
  return normalizeTodoStatus(status) === TodoStatus.COMPLETED;
}

export function isTodoInProgress(status: string | undefined): boolean {
  return normalizeTodoStatus(status) === TodoStatus.IN_PROGRESS;
}

export function isTodoPaused(status: string | undefined): boolean {
  return normalizeTodoStatus(status) === TodoStatus.PAUSED;
}

/** Parsed todo tool response: { "todoId": string, "items": TodoItem[] }. */
export interface TodoListResponse {
  todoId: string;
  items: TodoItem[];
}

/** Parse tool response for `todo_write`. Returns null on parse error. */
export function parseTodoListResponse(responseData: string): TodoListResponse | null {
  if (responseData == null) return null;
  const trimmed = responseData.trim();
  if (trimmed === '') return null;
  try {
    const parsed = JSON.parse(trimmed) as unknown;
    
    // Handle AgentToolResult wrapper: { success, message, result: "stringified JSON" }
    if (parsed && typeof parsed === 'object' && 'result' in parsed) {
      const wrapper = parsed as { result?: unknown };
      if (typeof wrapper.result === 'string') {
        try {
          const innerParsed = JSON.parse(wrapper.result) as unknown;
          if (innerParsed && typeof innerParsed === 'object' && Array.isArray((innerParsed as { items?: unknown }).items)) {
            const obj = innerParsed as { todoId?: string; items: TodoItem[] };
            const todoId = obj.todoId != null ? String(obj.todoId) : '';
            return { todoId, items: obj.items };
          }
        } catch {
          // Inner parse failed, continue to other formats
        }
      }
    }
    
    // Handle direct object format: { todoId, items }
    if (parsed && typeof parsed === 'object' && Array.isArray((parsed as { items?: unknown }).items)) {
      const obj = parsed as { todoId?: string; items: TodoItem[] };
      const todoId = obj.todoId != null ? String(obj.todoId) : '';
      return { todoId, items: obj.items };
    }
    
    return null;
  } catch {
    return null;
  }
}

/** Recognized todo tools. */
export const TODO_TOOL_NAMES = ['todoWrite'] as const;

export function isTodoTool(toolName: string): boolean {
  return TODO_TOOL_NAMES.includes(toolName as (typeof TODO_TOOL_NAMES)[number]);
}
