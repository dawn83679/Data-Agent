import { MessageRole } from '../../../types/chat';
import { blocksToSegments } from './blocksToSegments';
import { isTodoCompleted, isTodoTool, parseTodoListResponse } from '../blocks';
import type { TodoItem, TodoListResponse } from '../blocks';
import { segmentsHaveTodo } from './segmentTodoUtils';
import type { Message, TodoBoxSpec } from './types';
import { SegmentKind } from './types';

function getTodoIdFromResponse(res: TodoListResponse | null): string {
  if (res?.todoId == null || res.todoId === '') return '';
  return res.todoId;
}

export type { TodoBoxSpec } from './types';

export interface TodoInMessagesState {
  /** Index of the last assistant message that contains a todo tool run. */
  lastAssistantMessageIndexWithTodo: number;
  /** Todo list from that last message, or null. */
  latestTodoItems: TodoItem[] | null;
  /** True when latestTodoItems is non-empty and every item is completed. */
  allTodoCompleted: boolean;
  /** Per message index: list of todo boxes to show (each todoId fixed in the message where it first appeared). */
  todoBoxesByMessageIndex: Record<number, TodoBoxSpec[]>;
}

/**
 * Derives todo-related state: one box per todoId, fixed in the message where that todoId first appeared;
 * each box shows the latest items for that todoId. Single pass over messages.
 */
export function useTodoInMessages(displayMessages: Message[]): TodoInMessagesState {
  const firstMessageIndexByTodoId: Record<string, number> = {};
  const latestItemsByTodoId: Record<string, TodoItem[]> = {};
  let lastAssistantMessageIndexWithTodo = -1;
  let latestTodoItems: TodoItem[] | null = null;

  displayMessages.forEach((msg, msgIndex) => {
    if (msg.role !== MessageRole.ASSISTANT) return;
    const segs =
      msg.blocks && msg.blocks.length > 0 ? blocksToSegments(msg.blocks) : [];
    if (!segmentsHaveTodo(segs)) return;
    lastAssistantMessageIndexWithTodo = msgIndex;
    segs.forEach((s) => {
      if (s.kind !== SegmentKind.TOOL_RUN || !isTodoTool(s.toolName)) return;
      const res = parseTodoListResponse(s.responseData);
      if (res == null) return;
      const todoId = getTodoIdFromResponse(res);
      const key = todoId === '' ? '__legacy' : todoId;
      if (firstMessageIndexByTodoId[key] === undefined) {
        firstMessageIndexByTodoId[key] = msgIndex;
      }
      latestItemsByTodoId[key] = res.items ?? [];
      latestTodoItems = res.items ?? null;
    });
  });

  const todoBoxesByMessageIndex: Record<number, TodoBoxSpec[]> = {};
  Object.entries(firstMessageIndexByTodoId).forEach(([key, firstIdx]) => {
    const items = latestItemsByTodoId[key];
    if (items == null || items.length === 0) return;
    if (!todoBoxesByMessageIndex[firstIdx]) todoBoxesByMessageIndex[firstIdx] = [];
    const todoId = key === '__legacy' ? '' : key;
    todoBoxesByMessageIndex[firstIdx].push({ todoId, items });
  });

  const itemsForCompletion = latestTodoItems as TodoItem[] | null;
  const allTodoCompleted =
    itemsForCompletion != null &&
    itemsForCompletion.length > 0 &&
    itemsForCompletion.every((item) => isTodoCompleted(item.status));

  return {
    lastAssistantMessageIndexWithTodo,
    latestTodoItems,
    allTodoCompleted,
    todoBoxesByMessageIndex,
  };
}
