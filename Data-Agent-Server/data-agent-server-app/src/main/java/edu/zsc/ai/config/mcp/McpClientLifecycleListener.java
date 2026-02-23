package edu.zsc.ai.config.mcp;

import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class McpClientLifecycleListener implements McpClientListener {

    private final String clientName;

    public McpClientLifecycleListener(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public void beforeExecuteTool(McpCallContext context) {
        log.debug("MCP client '{}' executing tool: {}", clientName, context.message());
    }

    @Override
    public void afterExecuteTool(McpCallContext context, ToolExecutionResult result, Map<String, Object> rawResult) {
        if (result.isError()) {
            log.warn("MCP client '{}' tool execution failed: {}", clientName, result.resultText());
        } else {
            log.debug("MCP client '{}' tool execution succeeded", clientName);
        }
    }

    @Override
    public void onExecuteToolError(McpCallContext context, Throwable error) {
        log.error("MCP client '{}' tool execution error: {}", clientName, error.getMessage(), error);
    }
}
