package edu.zsc.ai.config.mcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import edu.zsc.ai.mcp.config.McpServerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP (Model Context Protocol) configuration using LangChain4j official MCP support.
 * 
 * This configuration reads mcp-servers.json and creates McpClient instances
 * for each enabled MCP server using stdio transport.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class McpConfig {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Create a list of MCP clients from configuration file.
     * Each client connects to one MCP server via stdio transport.
     */
    @Bean
    public List<McpClient> mcpClients() {
        List<McpClient> clients = new ArrayList<>();
        
        try {
            ClassPathResource resource = new ClassPathResource("mcp-servers.json");
            if (!resource.exists()) {
                log.warn("mcp-servers.json not found, no MCP servers will be initialized");
                return clients;
            }
            
            McpServerConfig config = objectMapper.readValue(
                resource.getInputStream(), 
                McpServerConfig.class
            );
            
            for (Map.Entry<String, McpServerConfig.McpServer> entry : config.getMcpServers().entrySet()) {
                String serverName = entry.getKey();
                McpServerConfig.McpServer serverConfig = entry.getValue();
                
                if (serverConfig.isDisabled()) {
                    log.info("Skipping disabled MCP server: {}", serverName);
                    continue;
                }
                
                try {
                    McpClient client = createMcpClient(serverName, serverConfig);
                    clients.add(client);
                    log.info("Successfully created MCP client for server: {}", serverName);
                } catch (Exception e) {
                    log.error("Failed to create MCP client for server: {}", serverName, e);
                }
            }
            
            log.info("Initialized {} MCP clients", clients.size());
            
        } catch (IOException e) {
            log.error("Failed to load mcp-servers.json", e);
        }
        
        return clients;
    }
    
    /**
     * Create a single MCP client for a server using stdio transport.
     */
    private McpClient createMcpClient(String serverName, McpServerConfig.McpServer config) {
        // Build command with arguments
        List<String> command = new ArrayList<>();
        command.add(config.getCommand());
        if (config.getArgs() != null && config.getArgs().length > 0) {
            command.addAll(List.of(config.getArgs()));
        }
        
        log.debug("Creating MCP client for '{}' with command: {}", serverName, command);
        
        // Create stdio transport
        StdioMcpTransport.Builder transportBuilder = StdioMcpTransport.builder()
            .command(command);
        
        // Add environment variables if specified
        if (config.getEnv() != null && !config.getEnv().isEmpty()) {
            transportBuilder.environment(config.getEnv());
        }
        
        McpTransport transport = transportBuilder.build();
        
        // Create MCP client with the transport
        return DefaultMcpClient.builder()
            .transport(transport)
            .key(serverName)  // Important: set key to identify this client
            .build();
    }
}
