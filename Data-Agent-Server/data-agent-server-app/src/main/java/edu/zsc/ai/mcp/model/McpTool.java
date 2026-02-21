package edu.zsc.ai.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a tool provided by an MCP server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    
    /** Tool name from MCP server */
    private String name;
    
    /** Tool description */
    private String description;
    
    /** JSON schema for tool input parameters */
    private Map<String, Object> inputSchema;
    
    /** Which MCP server provides this tool */
    private String serverName;
}
