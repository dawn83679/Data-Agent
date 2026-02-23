import { isTodoTool } from './todoTypes';
import { isAskUserQuestionTool } from './askUserQuestionTypes';

/**
 * Unified tool type detection and classification for AI assistant tools.
 *
 * Tool Categories:
 * 1. Interactive System Tools: TodoWrite, AskUserQuestion (need user interaction)
 * 2. Built-in Database Tools: DDL, SQL queries, table operations (use ToolRunDetail)
 * 3. MCP External Tools: Charts, visualizations (use McpToolBlock)
 */

export enum ToolType {
  /** TodoWrite tool - renders as TodoListBlock */
  TODO = 'TODO',
  /** AskUserQuestion tool - renders as AskUserQuestionBlock */
  ASK_USER = 'ASK_USER',
  /** MCP external tools (charts, visualizations) - renders as McpToolBlock */
  MCP = 'MCP',
  /** All other tools (including built-in database tools) - renders as ToolRunDetail */
  GENERIC = 'GENERIC',
}

/**
 * Get tool type for rendering dispatch
 *
 * @param toolName - The name of the tool
 * @param serverName - MCP server name for precise detection (e.g., "chart-server")
 * @returns The tool type for rendering
 */
export function getToolType(toolName: string, serverName?: string): ToolType {
  if (isTodoTool(toolName)) return ToolType.TODO;
  if (isAskUserQuestionTool(toolName)) return ToolType.ASK_USER;
  // Precise detection: serverName exists = MCP tool
  if (serverName !== undefined && serverName !== '') return ToolType.MCP;
  return ToolType.GENERIC;
}
