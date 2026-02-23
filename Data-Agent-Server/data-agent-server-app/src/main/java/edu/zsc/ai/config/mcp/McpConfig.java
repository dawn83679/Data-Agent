package edu.zsc.ai.config.mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(name = "mcp.enabled", havingValue = "true", matchIfMissing = true)
public class McpConfig implements DisposableBean {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private final McpProperties properties;
    private List<McpClient> clients;
    /** Tool name → MCP server name mapping (e.g., "generate_bar_chart" → "chart-server") */
    private final Map<String, String> toolNameToServerMap = new ConcurrentHashMap<>();

    @Bean
    public List<McpClient> mcpClients() {
        log.info("MCP enabled: {}, servers config: {}", properties.isEnabled(), properties.getServers());
        if (properties.getServers() == null || properties.getServers().isEmpty()) {
            log.warn("No MCP servers configured");
            return List.of();
        }

        clients = properties.getServers().entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(e -> {
                    String serverName = e.getKey();
                    McpClient client = createClient(serverName, e.getValue());

                    // Build tool name → server name mapping
                    if (client instanceof DefaultMcpClient defaultClient) {
                        try {
                            List<ToolSpecification> tools = defaultClient.listTools();
                            tools.forEach(tool -> {
                                toolNameToServerMap.put(tool.name(), serverName);
                                log.debug("Mapped tool '{}' to server '{}'", tool.name(), serverName);
                            });
                            log.info("MCP client '{}' provides {} tools", serverName, tools.size());
                        } catch (Exception ex) {
                            log.warn("Failed to list tools for mapping from server '{}': {}",
                                    serverName, ex.getMessage());
                        }
                    }
                    return client;
                })
                .filter(Objects::nonNull)
                .toList();

        log.info("Initialized {} MCP client(s), mapped {} tools", clients.size(), toolNameToServerMap.size());
        return clients;
    }

    /**
     * Exposes tool name → MCP server name mapping as a Bean for ChatService.
     *
     * @return Unmodifiable map of tool names to server names
     */
    @Bean("mcpToolNameToServerMap")
    public Map<String, String> mcpToolNameToServerMap() {
        return Collections.unmodifiableMap(toolNameToServerMap);
    }

    @Bean("mcpToolProvider")
    public McpToolProvider mcpToolProvider(List<McpClient> mcpClients) {
        if (mcpClients == null || mcpClients.isEmpty()) {
            log.warn("No MCP clients available, returning empty McpToolProvider");
            return McpToolProvider.builder()
                    .mcpClients(List.of())
                    .build();
        }

        log.info("Creating MCP tool provider from {} clients", mcpClients.size());

        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .failIfOneServerFails(false)
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

            DefaultMcpClient.Builder clientBuilder = DefaultMcpClient.builder()
                    .transport(transport)
                    .key(name)
                    .listener(new McpClientLifecycleListener(name))
                    .toolExecutionTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));

            // Configure health check if enabled
            if (config.isAutoHealthCheck()) {
                clientBuilder.autoHealthCheck(true)
                        .autoHealthCheckInterval(Duration.ofSeconds(config.getHealthCheckIntervalSeconds()));
                log.info("Health check enabled for MCP client '{}' (interval: {}s)",
                        name, config.getHealthCheckIntervalSeconds());
            }

            McpClient client = clientBuilder.build();

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
