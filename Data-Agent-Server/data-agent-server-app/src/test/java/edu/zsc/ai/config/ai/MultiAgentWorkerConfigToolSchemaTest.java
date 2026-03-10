package edu.zsc.ai.config.ai;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import edu.zsc.ai.agent.MultiAgentWorker;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.memory.MemoryTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.DiscoveryTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.think.ThinkingTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationStore;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.domain.service.ai.MemoryCandidateService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class MultiAgentWorkerConfigToolSchemaTest {

    @Test
    void schemaAnalystWorkerShouldExposeDiscoveryToolsToModel() {
        CapturingStreamingChatModel model = new CapturingStreamingChatModel();
        MultiAgentWorker worker = buildConfig(model).schemaAnalystWorkers()
                .get(MultiAgentWorkerConfig.workerKey("qwen3-max"));

        run(worker);

        Set<String> toolNames = capturedToolNames(model);
        assertTrue(toolNames.contains("getEnvironmentOverview"), toolNames.toString());
        assertTrue(toolNames.contains("searchObjects"), toolNames.toString());
        assertTrue(toolNames.contains("getObjectDetail"), toolNames.toString());
        assertTrue(toolNames.contains("thinking"), toolNames.toString());
        assertFalse(toolNames.contains("executeSelectSql"));
        assertFalse(toolNames.contains("askUserConfirm"));
    }

    @Test
    void sqlExecutorWorkerShouldExposeOnlyExecutionToolsToModel() {
        CapturingStreamingChatModel model = new CapturingStreamingChatModel();
        MultiAgentWorker worker = buildConfig(model).sqlExecutorWorkers()
                .get(MultiAgentWorkerConfig.workerKey("qwen3-max"));

        run(worker);

        Set<String> toolNames = capturedToolNames(model);
        assertTrue(toolNames.contains("executeSelectSql"), toolNames.toString());
        assertTrue(toolNames.contains("executeNonSelectSql"), toolNames.toString());
        assertTrue(toolNames.contains("askUserConfirm"), toolNames.toString());
        assertFalse(toolNames.contains("getEnvironmentOverview"));
        assertFalse(toolNames.contains("searchObjects"));
        assertFalse(toolNames.contains("getObjectDetail"));
        assertFalse(toolNames.contains("thinking"));
    }

    private MultiAgentWorkerConfig buildConfig(CapturingStreamingChatModel model) {
        return new MultiAgentWorkerConfig(
                Map.of("qwen3-max", model),
                new MultiAgentPromptConfig(),
                new DiscoveryTool(mock(DiscoveryService.class)),
                new ThinkingTool(),
                new MemoryTool(
                        mock(MemoryCandidateService.class),
                        mock(MemoryService.class),
                        new MemoryProperties()),
                new TodoTool(),
                new ActivateSkillTool(),
                new ExecuteSqlTool(mock(SqlExecutionService.class), mock(WriteConfirmationStore.class)),
                new AskUserConfirmTool(mock(WriteConfirmationStore.class)));
    }

    private void run(MultiAgentWorker worker) {
        assertNotNull(worker);
        worker.run("probe tool schema", InvocationParameters.from(Map.of()))
                .onCompleteResponse(response -> {
                })
                .onError(error -> fail(error.getMessage()))
                .start();
    }

    private Set<String> capturedToolNames(CapturingStreamingChatModel model) {
        ChatRequest request = model.lastRequest.get();
        assertNotNull(request);
        return java.util.stream.Stream.concat(
                        request.toolSpecifications() == null ? java.util.stream.Stream.empty() : request.toolSpecifications().stream(),
                        request.parameters() == null || request.parameters().toolSpecifications() == null
                                ? java.util.stream.Stream.empty()
                                : request.parameters().toolSpecifications().stream())
                .filter(Objects::nonNull)
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());
    }

    private static class CapturingStreamingChatModel implements StreamingChatModel {

        private final AtomicReference<ChatRequest> lastRequest = new AtomicReference<>();

        @Override
        public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
            lastRequest.set(chatRequest);
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("ok"))
                    .build());
        }
    }
}
