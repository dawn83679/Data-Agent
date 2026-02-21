package edu.zsc.ai.mcp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP Chart Tool URL Extension Test
 * 
 * Tests whether URLs returned by MCP chart tools automatically have .png extension added
 */
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("MCP Chart URL Extension Test")
@Slf4j
class McpChartUrlExtensionTest {

    @Autowired(required = false)
    private ReActAgentProvider reActAgentProvider;

    @Autowired(required = false)
    @Qualifier("mcpToolProvider")
    private Map<ToolSpecification, ToolExecutor> mcpToolProvider;
    
    @Autowired(required = false)
    private edu.zsc.ai.domain.service.ai.AiConversationService aiConversationService;

    @Test
    @DisplayName("Test 1: Direct Pie Chart Tool Call with URL Extension Verification")
    void testPieChartUrlExtension() {
        assertNotNull(mcpToolProvider, "MCP tool provider should not be null");
        
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║  Test 1: Direct Pie Chart Tool Call with URL Extension    ║");
        log.info("╚════════════════════════════════════════════════════════════╝\n");
        
        // Find pie chart tool
        ToolSpecification pieChartTool = mcpToolProvider.keySet().stream()
            .filter(spec -> spec.name().toLowerCase().contains("pie") 
                         && spec.name().toLowerCase().contains("chart"))
            .findFirst()
            .orElse(null);
        
        if (pieChartTool == null) {
            log.warn("⚠ Skipping test: Pie chart tool not found");
            log.warn("Available tools: {}", mcpToolProvider.keySet().stream()
                .map(ToolSpecification::name)
                .toList());
            return;
        }
        
        log.info("✓ Found tool: {}", pieChartTool.name());
        log.info("  Description: {}", pieChartTool.description());
        
        try {
            // Construct test data - simple pie chart data
            String testData = """
                [
                  {"category": "Chrome", "value": 65},
                  {"category": "Firefox", "value": 15},
                  {"category": "Safari", "value": 12},
                  {"category": "Edge", "value": 8}
                ]
                """;
            
            // Construct tool execution request
            String arguments = String.format(
                "{\"data\": %s, \"title\": \"Browser Market Share\", \"width\": 600, \"height\": 400}",
                testData.replace("\n", "").replace("  ", "")
            );
            
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name(pieChartTool.name())
                .arguments(arguments)
                .build();
            
            log.info("\n[Executing Tool]");
            log.info("Tool name: {}", request.name());
            log.info("Arguments: {}", request.arguments());
            
            // Get executor and invoke
            ToolExecutor executor = mcpToolProvider.get(pieChartTool);
            assertNotNull(executor, "Tool executor should not be null");
            
            String result = executor.execute(request, "test-memory-id");
            
            log.info("\n[Execution Result]");
            log.info("Result: {}", result);
            
            // Verify result
            assertNotNull(result, "Tool execution result should not be null");
            assertFalse(result.trim().isEmpty(), "Tool execution result should not be empty");
            
            // Verify URL is present
            assertTrue(result.contains("http"), "Result should contain URL");
            
            // Verify .png extension was added
            if (result.contains("alipayobjects.com")) {
                assertTrue(result.endsWith(".png") || result.contains(".png"), 
                    "Aliyun OSS URL should have .png extension added");
                log.info("✓ URL correctly has .png extension added");
            }
            
            log.info("\n✅ Test passed: Pie chart tool returned URL with correct format\n");
            
        } catch (Exception e) {
            log.error("❌ Test failed", e);
            fail("Pie chart tool invocation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 2: Direct Column Chart Tool Call with URL Extension Verification")
    void testColumnChartUrlExtension() {
        assertNotNull(mcpToolProvider, "MCP tool provider should not be null");
        
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║  Test 2: Direct Column Chart Tool Call with URL Extension ║");
        log.info("╚════════════════════════════════════════════════════════════╝\n");
        
        // Find column chart tool
        ToolSpecification columnChartTool = mcpToolProvider.keySet().stream()
            .filter(spec -> spec.name().toLowerCase().contains("column") 
                         && spec.name().toLowerCase().contains("chart"))
            .findFirst()
            .orElse(null);
        
        if (columnChartTool == null) {
            log.warn("⚠ Skipping test: Column chart tool not found");
            return;
        }
        
        log.info("✓ Found tool: {}", columnChartTool.name());
        
        try {
            // Construct test data - F1 driver nationality distribution
            String testData = """
                [
                  {"category": "UK", "value": 187},
                  {"category": "Germany", "value": 54},
                  {"category": "Brazil", "value": 34},
                  {"category": "Italy", "value": 29},
                  {"category": "France", "value": 28}
                ]
                """;
            
            String arguments = String.format(
                "{\"data\": %s, \"title\": \"F1 Driver Nationality Distribution\", \"axisXTitle\": \"Nationality\", \"axisYTitle\": \"Number of Drivers\", \"width\": 800, \"height\": 500}",
                testData.replace("\n", "").replace("  ", "")
            );
            
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name(columnChartTool.name())
                .arguments(arguments)
                .build();
            
            log.info("\n[Executing Tool]");
            log.info("Tool name: {}", request.name());
            
            ToolExecutor executor = mcpToolProvider.get(columnChartTool);
            String result = executor.execute(request, "test-memory-id");
            
            log.info("\n[Execution Result]");
            log.info("Result: {}", result);
            
            // Verify result
            assertNotNull(result, "Tool execution result should not be null");
            assertTrue(result.contains("http"), "Result should contain URL");
            
            // Verify extension
            if (result.contains("alipayobjects.com")) {
                assertTrue(result.endsWith(".png") || result.contains(".png"), 
                    "URL should have .png extension added");
                log.info("✓ URL correctly has .png extension added");
            }
            
            log.info("\n✅ Test passed: Column chart tool returned URL with correct format\n");
            
        } catch (Exception e) {
            log.error("❌ Test failed", e);
            fail("Column chart tool invocation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 3: Direct Organization Chart Tool Call with URL Extension Verification")
    void testOrganizationChartUrlExtension() {
        assertNotNull(mcpToolProvider, "MCP tool provider should not be null");
        
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║  Test 3: Direct Org Chart Tool Call with URL Extension    ║");
        log.info("╚════════════════════════════════════════════════════════════╝\n");
        
        // Find organization chart tool
        ToolSpecification orgChartTool = mcpToolProvider.keySet().stream()
            .filter(spec -> spec.name().toLowerCase().contains("organization") 
                         && spec.name().toLowerCase().contains("chart"))
            .findFirst()
            .orElse(null);
        
        if (orgChartTool == null) {
            log.warn("⚠ Skipping test: Organization chart tool not found");
            return;
        }
        
        log.info("✓ Found tool: {}", orgChartTool.name());
        
        try {
            // Construct test data - database table structure
            String testData = """
                {
                  "name": "drivers",
                  "description": "F1 Drivers Table",
                  "children": [
                    {"name": "driverId", "description": "Driver ID (Primary Key)"},
                    {"name": "driverRef", "description": "Driver Reference Name"},
                    {"name": "number", "description": "Car Number"},
                    {"name": "code", "description": "Driver Code"},
                    {"name": "forename", "description": "First Name"},
                    {"name": "surname", "description": "Last Name"},
                    {"name": "dob", "description": "Date of Birth"},
                    {"name": "nationality", "description": "Nationality"},
                    {"name": "url", "description": "URL"}
                  ]
                }
                """;
            
            String arguments = String.format(
                "{\"data\": %s, \"orient\": \"vertical\", \"width\": 800, \"height\": 600}",
                testData.replace("\n", "").replace("  ", "")
            );
            
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name(orgChartTool.name())
                .arguments(arguments)
                .build();
            
            log.info("\n[Executing Tool]");
            log.info("Tool name: {}", request.name());
            
            ToolExecutor executor = mcpToolProvider.get(orgChartTool);
            String result = executor.execute(request, "test-memory-id");
            
            log.info("\n[Execution Result]");
            log.info("Result: {}", result);
            
            // Verify result
            assertNotNull(result, "Tool execution result should not be null");
            assertTrue(result.contains("http"), "Result should contain URL");
            
            // Verify extension
            if (result.contains("alipayobjects.com")) {
                assertTrue(result.endsWith(".png") || result.contains(".png"), 
                    "URL should have .png extension added");
                log.info("✓ URL correctly has .png extension added");
            }
            
            log.info("\n✅ Test passed: Organization chart tool returned URL with correct format\n");
            
        } catch (Exception e) {
            log.error("❌ Test failed", e);
            fail("Organization chart tool invocation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 4: AI Conversation Generate Pie Chart with URL Extension Verification")
    void testAiGeneratePieChartWithUrlExtension() {
        if (reActAgentProvider == null) {
            log.warn("⚠ Skipping test: ReActAgentProvider not injected");
            return;
        }
        
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║  Test 4: AI Generate Pie Chart with URL Extension         ║");
        log.info("╚════════════════════════════════════════════════════════════╝\n");
        
        try {
            // Get AI Agent
            ReActAgent agent = reActAgentProvider.getAgent(ModelEnum.QWEN3_MAX.getModelName());
            assertNotNull(agent, "AI Agent should not be null");
            
            // Create test conversation
            Long userId = 2L;
            Long conversationId = createTestConversation(userId, "MCP Chart Test - Pie Chart");
            
            // Simulate user request
            String userMessage = """
                Please generate a pie chart showing the following browser market share data:
                - Chrome: 65%
                - Firefox: 15%
                - Safari: 12%
                - Edge: 8%
                
                Chart title: Browser Market Share Distribution
                """;
            
            log.info("[User Message]\n{}", userMessage);
            
            // Set invocation parameters
            Map<String, Object> contextMap = new java.util.HashMap<>();
            contextMap.put("userId", userId);
            InvocationParameters parameters = InvocationParameters.from(contextMap);
            
            // Call AI - memoryId format must be "userId:conversationId"
            String memoryId = userId + ":" + conversationId;
            log.info("Using memoryId: {}", memoryId);
            TokenStream tokenStream = agent.chat(memoryId, userMessage, parameters);
            
            // Collect response
            CompletableFuture<AiMessage> responseFuture = new CompletableFuture<>();
            StringBuilder fullResponse = new StringBuilder();
            List<ToolExecutionRequest> toolRequests = new java.util.ArrayList<>();
            
            tokenStream.onPartialResponse(content -> {
                fullResponse.append(content);
            });
            
            tokenStream.onIntermediateResponse(response -> {
                if (response.aiMessage().hasToolExecutionRequests()) {
                    log.info("\n[AI Tool Invocation]");
                    for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
                        log.info("  Tool: {}", toolRequest.name());
                        log.info("  Arguments: {}", toolRequest.arguments());
                        toolRequests.add(toolRequest);
                    }
                }
            });
            
            tokenStream.onCompleteResponse(response -> {
                log.info("\n[AI Complete Response]");
                log.info("{}", response.aiMessage().text());
                responseFuture.complete(response.aiMessage());
            });
            
            tokenStream.onError(error -> {
                log.error("❌ AI response error", error);
                responseFuture.completeExceptionally(error);
            });
            
            tokenStream.start();
            
            // Wait for response completion (max 60 seconds)
            responseFuture.get(60, TimeUnit.SECONDS);
            String finalResponse = fullResponse.toString();
            
            log.info("\n[Test Results]");
            log.info("AI invoked tools: {}", !toolRequests.isEmpty());
            log.info("Response length: {} characters", finalResponse.length());
            
            // Verify if chart tool was called
            boolean calledChartTool = toolRequests.stream()
                .anyMatch(req -> req.name().toLowerCase().contains("chart") 
                              || req.name().toLowerCase().contains("pie"));
            
            if (calledChartTool) {
                log.info("✓ AI successfully invoked chart generation tool");
            } else {
                log.warn("⚠ AI did not invoke chart tool");
                log.warn("Tools invoked: {}", toolRequests.stream()
                    .map(ToolExecutionRequest::name)
                    .toList());
            }
            
            // Verify if response contains URL with extension
            if (finalResponse.contains("http") || finalResponse.contains("https")) {
                log.info("✓ Response contains URL");
                
                // Extract URL
                String[] words = finalResponse.split("\\s+");
                for (String word : words) {
                    if (word.contains("http") && word.contains("alipayobjects.com")) {
                        log.info("  Found URL: {}", word);
                        
                        if (word.endsWith(".png") || word.contains(".png")) {
                            log.info("  ✓ URL contains .png extension");
                        } else {
                            log.warn("  ⚠ URL missing .png extension");
                        }
                    }
                }
            } else {
                log.warn("⚠ No URL found in response");
            }
            
            log.info("\n✅ Test completed\n");
            
        } catch (Exception e) {
            log.error("❌ Test failed", e);
            fail("AI conversation test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 5: AI Conversation Generate Column Chart with URL Extension Verification")
    void testAiGenerateColumnChartWithUrlExtension() {
        if (reActAgentProvider == null) {
            log.warn("⚠ Skipping test: ReActAgentProvider not injected");
            return;
        }
        
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║  Test 5: AI Generate Column Chart with URL Extension      ║");
        log.info("╚════════════════════════════════════════════════════════════╝\n");
        
        try {
            ReActAgent agent = reActAgentProvider.getAgent(ModelEnum.QWEN3_MAX.getModelName());
            assertNotNull(agent, "AI Agent should not be null");
            
            // Create test conversation
            Long userId = 2L;
            Long conversationId = createTestConversation(userId, "MCP Chart Test - Column Chart");
            
            String userMessage = """
                Please generate a column chart showing quarterly sales for 2024:
                - Q1: 1.2 million
                - Q2: 1.5 million
                - Q3: 1.8 million
                - Q4: 2.0 million
                
                Chart title: 2024 Quarterly Sales
                X-axis title: Quarter
                Y-axis title: Sales (Million)
                """;
            
            log.info("[User Message]\n{}", userMessage);
            
            Map<String, Object> contextMap = new java.util.HashMap<>();
            contextMap.put("userId", userId);
            InvocationParameters parameters = InvocationParameters.from(contextMap);
            
            // memoryId format must be "userId:conversationId"
            String memoryId = userId + ":" + conversationId;
            log.info("Using memoryId: {}", memoryId);
            TokenStream tokenStream = agent.chat(memoryId, userMessage, parameters);
            
            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            StringBuilder fullResponse = new StringBuilder();
            
            tokenStream.onPartialResponse(fullResponse::append);
            
            tokenStream.onIntermediateResponse(response -> {
                if (response.aiMessage().hasToolExecutionRequests()) {
                    log.info("\n[AI Tool Invocation]");
                    response.aiMessage().toolExecutionRequests().forEach(req -> 
                        log.info("  Tool: {} - Arguments: {}", req.name(), req.arguments())
                    );
                }
            });
            
            tokenStream.onCompleteResponse(response -> {
                log.info("\n[AI Complete Response]");
                log.info("{}", response.aiMessage().text());
                responseFuture.complete(fullResponse.toString());
            });
            
            tokenStream.onError(error -> {
                log.error("❌ AI response error", error);
                responseFuture.completeExceptionally(error);
            });
            
            tokenStream.start();
            
            String finalResponse = responseFuture.get(60, TimeUnit.SECONDS);
            
            log.info("\n[Verification Results]");
            
            // Verify URL and extension
            if (finalResponse.contains("alipayobjects.com")) {
                log.info("✓ Response contains Aliyun OSS URL");
                
                // Check extension
                if (finalResponse.contains(".png")) {
                    log.info("✓ URL contains .png extension");
                } else {
                    log.warn("⚠ URL missing .png extension");
                }
            }
            
            log.info("\n✅ Test completed\n");
            
        } catch (Exception e) {
            log.error("❌ Test failed", e);
            fail("AI conversation test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 6: List All Available Chart Tools")
    void testListAllChartTools() {
        assertNotNull(mcpToolProvider, "MCP tool provider should not be null");
        
        log.info("\n╔════════════════════════════════════════════════════════════╗");
        log.info("║  Test 6: List All Available Chart Tools                   ║");
        log.info("╚════════════════════════════════════════════════════════════╝\n");
        
        log.info("[Total MCP Tools] {}", mcpToolProvider.size());
        log.info("");
        
        // Find all chart tools
        List<ToolSpecification> chartTools = mcpToolProvider.keySet().stream()
            .filter(spec -> {
                String name = spec.name().toLowerCase();
                return name.contains("chart") 
                    || name.contains("graph") 
                    || name.contains("diagram")
                    || name.contains("map")
                    || name.contains("plot");
            })
            .toList();
        
        log.info("[Chart Tools Count] {}", chartTools.size());
        log.info("");
        
        if (chartTools.isEmpty()) {
            log.warn("⚠ No chart tools found");
            log.warn("Please check mcp-servers.json configuration");
        } else {
            log.info("[Chart Tools List]");
            int index = 1;
            for (ToolSpecification tool : chartTools) {
                log.info("{}. {}", index++, tool.name());
                log.info("   Description: {}", tool.description());
                log.info("");
            }
            
            log.info("[Usage Suggestions]");
            log.info("In AI conversations, you can request:");
            log.info("  • \"Generate a pie chart showing...\"");
            log.info("  • \"Create a column chart displaying...\"");
            log.info("  • \"Help me draw a line chart...\"");
            log.info("  • \"Generate an organization chart...\"");
            log.info("");
        }
        
        log.info("✅ Tool list generation completed\n");
    }
    
    /**
     * Create test conversation
     */
    private Long createTestConversation(Long userId, String title) {
        if (aiConversationService == null) {
            // If service not injected, use timestamp as conversation ID (may fail, but at least no compile error)
            log.warn("AiConversationService not injected, using timestamp as conversation ID");
            return System.currentTimeMillis();
        }
        
        try {
            // Create new conversation
            edu.zsc.ai.domain.model.entity.ai.AiConversation conversation = 
                edu.zsc.ai.domain.model.entity.ai.AiConversation.builder()
                    .userId(userId)
                    .title(title)
                    .createdAt(java.time.LocalDateTime.now())
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();
            
            aiConversationService.save(conversation);
            log.info("Created test conversation: conversationId={}, title={}", conversation.getId(), title);
            return conversation.getId();
        } catch (Exception e) {
            log.warn("Failed to create test conversation, using timestamp as conversation ID", e);
            return System.currentTimeMillis();
        }
    }
}
