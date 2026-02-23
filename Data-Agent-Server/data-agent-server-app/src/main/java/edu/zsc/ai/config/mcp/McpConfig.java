package edu.zsc.ai.config.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP configuration with lifecycle management
 * Provides both MCP clients and tool provider in a single configuration class
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.enabled", havingValue = "true", matchIfMissing = true)
public class McpConfig implements DisposableBean {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private final McpProperties properties;
    private List<McpClient> clients;

    @Bean
    public List<McpClient> mcpClients() {
        if (properties.getServers().isEmpty()) {
            return List.of();
        }

        clients = properties.getServers().entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(e -> createClient(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .toList();

        log.info("Initialized {} MCP client(s)", clients.size());
        return clients;
    }

    @Bean("mcpToolProvider")
    public McpToolProvider mcpToolProvider(List<McpClient> mcpClients) {
        if (mcpClients == null || mcpClients.isEmpty()) {
            log.warn("No MCP clients available, returning empty McpToolProvider");
            return McpToolProvider.builder().build();
        }

        log.info("Creating MCP tool provider from {} clients", mcpClients.size());
        
        // Log tool count for each client (non-blocking, for monitoring)
        mcpClients.forEach(client -> {
            if (client instanceof DefaultMcpClient defaultClient) {
                try {
                    int toolCount = defaultClient.listTools().size();
                    log.info("MCP client '{}' provides {} tools", defaultClient.key(), toolCount);
                } catch (Exception e) {
                    log.warn("Failed to list tools for MCP client '{}': {}", 
                            defaultClient.key(), e.getMessage());
                }
            }
        });
        
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();
    }

    @Override
    public void destroy() {
        if (clients != null && !clients.isEmpty()) {
            log.info("Closing {} MCP client(s)", clients.size());
            clients.forEach(client -> {
                try {
                    if (client instanceof DefaultMcpClient defaultClient) {
                        defaultClient.close();
                        log.debug("Closed MCP client: {}", defaultClient.key());
                    }
                } catch (Exception e) {
                    log.error("Failed to close MCP client", e);
                }
            });
        }
    }

    private McpClient createClient(String name, McpProperties.ServerConfig config) {
        try {
            config.validate();
            List<String> command = buildCommand(config);

            McpTransport transport = StdioMcpTransport.builder()
                    .command(command)
                    .environment(config.getEnv())
                    .build();
            
            McpClient client = DefaultMcpClient.builder()
                    .transport(transport)
                    .key(name)
                    .build();

            log.info("Successfully created MCP client '{}'", name);
            return client;

        } catch (Exception e) {
            log.error("Failed to create MCP client '{}': {}", name, e.getMessage(), e);
            return null;
        }
    }

    private List<String> buildCommand(McpProperties.ServerConfig config) {
        if (config.getPackageName() != null) {
            // Simplified mode: npx + packageName + optional args
            List<String> command = new ArrayList<>();
            command.add(getNpxCommand());
            command.add(config.getPackageName());
            if (config.getArgs() != null && !config.getArgs().isEmpty()) {
                command.addAll(config.getArgs());
            }
            return command;
        }

        if (config.getCommand() != null) {
            return buildFullCommand(config);
        }

        throw new IllegalArgumentException(
                "Either 'packageName' or 'command' must be specified");
    }

    private List<String> buildFullCommand(McpProperties.ServerConfig config) {
        String adaptedCommand = adaptCommand(config.getCommand());
        if (config.getArgs() != null && !config.getArgs().isEmpty()) {
            List<String> command = new ArrayList<>();
            command.add(adaptedCommand);
            command.addAll(config.getArgs());
            return command;
        }
        return List.of(adaptedCommand);
    }

    private String getNpxCommand() {
        return IS_WINDOWS ? "npx.cmd" : "npx";
    }

    private String adaptCommand(String cmd) {
        if (IS_WINDOWS && !cmd.endsWith(".cmd") && !cmd.endsWith(".exe")) {
            if (cmd.equals("npx") || cmd.equals("npm")) {
                return cmd + ".cmd";
            }
        }
        return cmd;
    }
}
