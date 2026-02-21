package edu.zsc.ai.config.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for MCP Tool Provider.
 * 
 * Provides tools from MCP servers to LangChain4j AI services.
 * Tools can be filtered by name or other criteria.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class McpToolProviderConfig {
    
    /**
     * Create a map of tool specifications and executors from all MCP clients.
     * 
     * This approach provides tools directly to AI services without using ToolProvider.
     * The tools are discovered from connected MCP servers and made available for execution.
     * 
     * @param mcpClients List of MCP clients to aggregate tools from
     * @return Map of tool specifications to their executors
     */
    @Bean("mcpToolProvider")
    public Map<ToolSpecification, ToolExecutor> mcpToolProvider(
            List<McpClient> mcpClients) {
        
        Map<ToolSpecification, ToolExecutor> toolsMap = new HashMap<>();
        
        if (mcpClients.isEmpty()) {
            log.warn("No MCP clients available, returning empty tools map");
            return toolsMap;
        }
        
        log.info("Creating MCP tools from {} clients", mcpClients.size());
        
        int clientIndex = 0;
        for (McpClient client : mcpClients) {
            try {
                if (client instanceof DefaultMcpClient defaultClient) {
                    // Get tools from the client
                    List<ToolSpecification> toolSpecs = new ArrayList<>(defaultClient.listTools());
                    
                    log.info("Found {} tools from MCP client #{}", 
                        toolSpecs.size(), 
                        clientIndex);
                    
                    // Create executors for each tool
                    for (ToolSpecification toolSpec : toolSpecs) {
                        ToolExecutor executor = (toolExecutionRequest, memoryId) -> {
                            ToolExecutionResult result = defaultClient.executeTool(toolExecutionRequest);
                            // Convert result to string
                            return result.resultText();
                        };
                        
                        toolsMap.put(toolSpec, executor);
                    }
                }
                clientIndex++;
            } catch (Exception e) {
                log.error("Failed to load tools from MCP client #{}", clientIndex, e);
                // Continue with other clients even if one fails
                clientIndex++;
            }
        }
        
        log.info("MCP tools created successfully: {} tools total", toolsMap.size());
        return toolsMap;
    }
}
