import { useState } from 'react';
import { cn } from '../../../lib/utils';
import { getRenderConfig } from './McpToolConfig';
import { McpToolHeader } from './McpToolHeader';
import { McpToolContent } from './McpToolContent';

export interface McpToolBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError?: boolean;
  /** MCP server name (e.g., "chart-server") for server-specific rendering */
  serverName?: string;
}

/**
 * Detect if response contains an image URL
 */
function extractImageUrl(text: string): string | null {
  if (!text) return null;
  const urlMatch = text.match(/https?:\/\/[^\s<>"]+\.(png|jpg|jpeg|gif|svg|webp)/i);
  if (urlMatch) return urlMatch[0];

  // Also check for Aliyun OSS URLs without extension
  const ossMatch = text.match(/https?:\/\/[^\s<>"]*alipayobjects\.com[^\s<>"]*/i);
  if (ossMatch) return ossMatch[0];

  return null;
}

/**
 * MCP Tool Block - unified configuration-based rendering
 *
 * Rendering modes (auto-selected by serverName):
 * - chart-server: Minimal mode (chart-only display)
 * - Other servers: Detailed mode (full information)
 */
export function McpToolBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
  serverName,
}: McpToolBlockProps) {
  // Get configuration based on server name and error state
  const config = getRenderConfig(serverName, responseError);
  const [collapsed, setCollapsed] = useState(config.defaultCollapsed);
  const imageUrl = extractImageUrl(responseData);

  return (
    <div
      className={cn(
        config.containerClass,
        collapsed ? 'opacity-70 theme-text-secondary' : 'opacity-100 theme-text-primary'
      )}
    >
      {/* Unified header - renders simple or full style based on config */}
      <McpToolHeader
        config={config}
        collapsed={collapsed}
        onToggle={() => setCollapsed(c => !c)}
        toolName={toolName}
        serverName={serverName}
        responseError={responseError}
      />

      {/* Unified content - conditionally shows chart, parameters, response */}
      {!collapsed && (
        <McpToolContent
          config={config}
          imageUrl={imageUrl}
          toolName={toolName}
          parametersData={parametersData}
          responseData={responseData}
        />
      )}
    </div>
  );
}
