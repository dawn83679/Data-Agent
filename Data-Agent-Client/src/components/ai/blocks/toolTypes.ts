import { isTodoTool } from './todoTypes';
import { isAskUserQuestionTool } from './askUserQuestionTypes';
import { isWriteConfirmTool } from './writeConfirmTypes';

const CHART_TOOL_NAMES = new Set(['renderChart']);

/**
 * Unified tool type detection and classification for AI assistant tools.
 *
 * Tool Categories:
 * 1. Interactive System Tools: TodoWrite, AskUserQuestion, AskUserConfirm (need user interaction)
 * 2. Built-in Database Tools: DDL, SQL queries, table operations (use ToolRunDetail)
 * 3. Built-in Chart Tool: renderChart (use ChartToolBlock)
 */

export enum ToolType {
  /** TodoWrite tool - renders as TodoListBlock */
  TODO = 'TODO',
  /** AskUserQuestion tool - renders as AskUserQuestionBlock */
  ASK_USER = 'ASK_USER',
  /** AskUserConfirm tool - renders as write confirmation panel */
  WRITE_CONFIRM = 'WRITE_CONFIRM',
  /** Built-in chart rendering tool. */
  CHART = 'CHART',
  /** All other tools (including built-in database tools) - renders as ToolRunDetail */
  GENERIC = 'GENERIC',
}

/**
 * Get tool type for rendering dispatch
 *
 * @param toolName - The name of the tool
 * @returns The tool type for rendering
 */
export function getToolType(toolName: string): ToolType {
  if (isTodoTool(toolName)) return ToolType.TODO;
  if (isAskUserQuestionTool(toolName)) return ToolType.ASK_USER;
  if (isWriteConfirmTool(toolName)) return ToolType.WRITE_CONFIRM;
  if (CHART_TOOL_NAMES.has(toolName)) return ToolType.CHART;
  return ToolType.GENERIC;
}
