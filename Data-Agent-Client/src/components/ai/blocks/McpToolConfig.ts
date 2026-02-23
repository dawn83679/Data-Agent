/**
 * MCP Tool Render Configuration
 *
 * Provides configuration-based rendering control for McpToolBlock component.
 * Eliminates code duplication by centralizing rendering decisions.
 */

export interface RenderConfig {
  /** Rendering mode: minimal (chart-only) or detailed (full info) */
  mode: 'minimal' | 'detailed';

  /** Default collapsed state */
  defaultCollapsed: boolean;

  /** Show parameters section */
  showParameters: boolean;

  /** Show response section */
  showResponse: boolean;

  /** Header style variant */
  headerStyle: 'simple' | 'full';

  /** Container CSS classes */
  containerClass: string;
}

/**
 * Get render configuration based on server name and error state
 *
 * @param serverName - MCP server name (e.g., "chart-server")
 * @param hasError - Whether tool execution failed
 * @returns Render configuration object
 */
export function getRenderConfig(serverName?: string, hasError?: boolean): RenderConfig {
  // chart-server: Minimal mode (chart-only display)
  // Falls back to detailed mode on error
  if (serverName === 'chart-server' && !hasError) {
    return {
      mode: 'minimal',
      defaultCollapsed: false,  // Default expanded to show chart
      showParameters: false,
      showResponse: false,
      headerStyle: 'simple',
      containerClass: 'mb-2 text-xs rounded transition-colors',
    };
  }

  // Other MCP servers: Detailed mode (full information)
  return {
    mode: 'detailed',
    defaultCollapsed: true,   // Default collapsed to save space
    showParameters: true,
    showResponse: true,
    headerStyle: 'full',
    containerClass: 'mb-3 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden bg-white dark:bg-gray-800 shadow-sm',
  };
}
