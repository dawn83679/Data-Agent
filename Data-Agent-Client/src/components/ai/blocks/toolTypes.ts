import { isTodoTool } from './todoTypes';
import { isAskUserQuestionTool } from './askUserQuestionTypes';
import { isEnterPlanModeTool } from './enterPlanModeTypes';
import { isExitPlanModeTool } from './exitPlanModeTypes';
import { isCallingSubAgentTool } from './subAgentTypes';

const CHART_TOOL_NAMES = new Set(['renderChart']);
const THINKING_TOOL_NAMES = new Set(['thinking']);
const SKILL_TOOL_NAMES = new Set(['activateSkill']);
const FILE_EXPORT_TOOL_NAMES = new Set(['exportFile']);
const EXECUTE_SELECT_TOOL_NAMES = new Set(['executeSelectSql']);
const GET_OBJECT_DETAIL_TOOL_NAMES = new Set(['getObjectDetail']);

/**
 * Unified tool type detection and classification for AI assistant tools.
 *
 * Tool Categories:
 * 1. Interactive System Tools: TodoWrite, AskUserQuestion (need user interaction)
 * 2. Built-in Database Tools: DDL, SQL queries, table operations (use ToolRunDetail)
 * 3. Built-in Chart Tool: renderChart (use ChartToolBlock)
 */

export enum ToolType {
  /** TodoWrite tool - renders as TodoListBlock */
  TODO = 'TODO',
  /** AskUserQuestion tool - renders as AskUserQuestionBlock */
  ASK_USER = 'ASK_USER',
  /** Built-in chart rendering tool. */
  CHART = 'CHART',
  /** Thinking tool - renders as collapsible thought block. */
  THINKING = 'THINKING',
  /** EnterPlanMode tool - renders as compact transition indicator. */
  ENTER_PLAN = 'ENTER_PLAN',
  /** ExitPlanMode tool - renders as plan card with action buttons. */
  EXIT_PLAN = 'EXIT_PLAN',
  /** ActivateSkill tool - renders response as markdown. */
  SKILL = 'SKILL',
  /** ExportFile tool - renders downloadable file card with preview. */
  FILE_EXPORT = 'FILE_EXPORT',
  /** executeSelectSql - renders structured query results. */
  EXECUTE_SELECT = 'EXECUTE_SELECT',
  /** getObjectDetail - renders structured DDL details. */
  GET_OBJECT_DETAIL = 'GET_OBJECT_DETAIL',
  /** SubAgent tools (exploreSchema, generateSqlPlan) - renders as SubAgent card with progress. */
  CALLING_SUB_AGENT = 'CALLING_SUB_AGENT',
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
  if (CHART_TOOL_NAMES.has(toolName)) return ToolType.CHART;
  if (THINKING_TOOL_NAMES.has(toolName)) return ToolType.THINKING;
  if (isEnterPlanModeTool(toolName)) return ToolType.ENTER_PLAN;
  if (isExitPlanModeTool(toolName)) return ToolType.EXIT_PLAN;
  if (SKILL_TOOL_NAMES.has(toolName)) return ToolType.SKILL;
  if (FILE_EXPORT_TOOL_NAMES.has(toolName)) return ToolType.FILE_EXPORT;
  if (EXECUTE_SELECT_TOOL_NAMES.has(toolName)) return ToolType.EXECUTE_SELECT;
  if (GET_OBJECT_DETAIL_TOOL_NAMES.has(toolName)) return ToolType.GET_OBJECT_DETAIL;
  if (isCallingSubAgentTool(toolName)) return ToolType.CALLING_SUB_AGENT;
  return ToolType.GENERIC;
}
