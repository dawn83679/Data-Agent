package edu.zsc.ai.mcp.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * MCP Server configuration model, maps to mcp-servers.json structure.
 */
@Data
public class McpServerConfig {
    
    @JsonProperty("mcpServers")
    private Map<String, McpServer> mcpServers;
    
    @Data
    public static class McpServer {
        private String command;
        private String[] args;
        private Map<String, String> env;
        private boolean disabled;
    }
}
