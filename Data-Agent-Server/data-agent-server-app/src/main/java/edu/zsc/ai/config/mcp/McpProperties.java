package edu.zsc.ai.config.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * MCP configuration properties
 */
@Data
@ConfigurationProperties(prefix = "mcp")
@Validated
public class McpProperties {
    
    // Global MCP feature toggle
    private boolean enabled = true;

    // MCP server configurations
    @Valid
    private Map<String, ServerConfig> servers;
    
    @Data
    public static class ServerConfig {

        // NPM package name (simplified mode, e.g., "@modelcontextprotocol/server-everything")
        private String packageName;

        // Full command path (advanced mode, e.g., "npx" or "/usr/local/bin/custom")
        private String command;

        // Command arguments
        private List<String> args;

        // Environment variables
        private Map<String, String> env = new HashMap<>();

        // Enable/disable this server
        private boolean enabled = true;

        // Tool execution timeout in seconds
        private int timeoutSeconds = 60;

        // Enable debug logging
        private boolean debug = false;

        // Enable automatic health checks (default: true)
        private boolean autoHealthCheck = true;

        // Health check interval in seconds (default: 30)
        private int healthCheckIntervalSeconds = 30;

        public void validate() {
            if (packageName == null && command == null) {
                throw new IllegalArgumentException(
                    "Either 'packageName' or 'command' must be specified");
            }
        }
    }
}
