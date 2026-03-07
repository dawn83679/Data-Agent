import { isTodoTool } from './todoTypes';
import { isAskUserQuestionTool } from './askUserQuestionTypes';
import { isWriteConfirmTool } from './writeConfirmTypes';
import { isEnterPlanModeTool } from './enterPlanModeTypes';
import { isExitPlanModeTool } from './exitPlanModeTypes';

const CHART_TOOL_NAMES = new Set(['renderChart']);
const THINKING_TOOL_NAMES = new Set(['sequentialThinking']);

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
  /** SequentialThinking tool - renders as collapsible thought block. */
  THINKING = 'THINKING',
  /** EnterPlanMode tool - renders as compact transition indicator. */
  ENTER_PLAN = 'ENTER_PLAN',
  /** ExitPlanMode tool - renders as plan card with action buttons. */
  EXIT_PLAN = 'EXIT_PLAN',
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
  if (THINKING_TOOL_NAMES.has(toolName)) return ToolType.THINKING;
  if (isEnterPlanModeTool(toolName)) return ToolType.ENTER_PLAN;
  if (isExitPlanModeTool(toolName)) return ToolType.EXIT_PLAN;
  return ToolType.GENERIC;
}
