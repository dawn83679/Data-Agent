package edu.zsc.ai.domain.service.agent.multi;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.domain.service.agent.multi.model.MultiAgentTask;
import edu.zsc.ai.domain.service.agent.multi.model.SubAgentDelegationResult;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiAgentDelegationService {

    private static final long SUB_AGENT_TIMEOUT_SECONDS = 3600L;

    private final MultiAgentRunStore multiAgentRunStore;
    private final MultiAgentWorkerFactory multiAgentWorkerFactory;
    private final SseEmitterRegistry sseEmitterRegistry;

    public SubAgentDelegationResult delegate(
            AgentRoleEnum role,
            String title,
            String instructions,
            InvocationParameters parameters) {
        RequestContextInfo parent = RequestContext.get();
        if (parent == null || parent.getConversationId() == null || parent.getRunId() == null) {
            return SubAgentDelegationResult.builder()
                    .agentRole(role.getCode())
                    .title(defaultTitle(role, title))
                    .status("failed")
                    .summary("Delegation failed because request context was missing.")
                    .details("Request context is required for multi-agent delegation.")
                    .build();
        }

        MultiAgentTask task = multiAgentRunStore.createTask(
                parent.getRunId(),
                parent.getTaskId(),
                role,
                defaultTitle(role, title),
                StringUtils.defaultIfBlank(instructions, "(no instructions supplied)"));

        emit(parent.getConversationId(), ChatResponseBlock.taskStart(
                task.getRunId(),
                task.getTaskId(),
                task.getAgentRole().getCode(),
                task.getTitle(),
                "Delegated by orchestrator."));

        try {
            List<ChatResponseBlock> blocks = runSubAgent(parent, task, parameters);
            TaskOutcome outcome = summarizeOutcome(task, blocks);

            if (outcome.requiresApproval()) {
                emit(parent.getConversationId(), ChatResponseBlock.taskApproval(
                        task.getRunId(),
                        task.getTaskId(),
                        task.getAgentRole().getCode(),
                        task.getTitle(),
                        outcome.summary(),
                        outcome.approvalDetails()));
            }

            emit(parent.getConversationId(), ChatResponseBlock.taskResult(
                    task.getRunId(),
                    task.getTaskId(),
                    task.getAgentRole().getCode(),
                    task.getTitle(),
                    outcome.status(),
                    outcome.summary(),
                    outcome.details()));

            multiAgentRunStore.markTask(task.getRunId(), task.getTaskId(), outcome.status().toUpperCase(), outcome.summary());

            return SubAgentDelegationResult.builder()
                    .runId(task.getRunId())
                    .taskId(task.getTaskId())
                    .agentRole(task.getAgentRole().getCode())
                    .title(task.getTitle())
                    .status(outcome.status())
                    .summary(outcome.summary())
                    .details(outcome.details())
                    .requiresApproval(outcome.requiresApproval())
                    .build();
        } catch (Exception e) {
            log.error("Sub-agent delegation failed: role={}, title={}", role, task.getTitle(), e);
            String message = "Sub-agent failed: " + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            emit(parent.getConversationId(), ChatResponseBlock.taskResult(
                    task.getRunId(),
                    task.getTaskId(),
                    task.getAgentRole().getCode(),
                    task.getTitle(),
                    "failed",
                    message,
                    null));
            multiAgentRunStore.markTask(task.getRunId(), task.getTaskId(), "FAILED", message);
            return SubAgentDelegationResult.builder()
                    .runId(task.getRunId())
                    .taskId(task.getTaskId())
                    .agentRole(task.getAgentRole().getCode())
                    .title(task.getTitle())
                    .status("failed")
                    .summary(message)
                    .details(message)
                    .requiresApproval(false)
                    .build();
        }
    }

    private List<ChatResponseBlock> runSubAgent(
            RequestContextInfo parent,
            MultiAgentTask task,
            InvocationParameters parameters) throws InterruptedException {
        String modelName = StringUtils.defaultIfBlank(parent.getModelName(), "qwen3-max");
        String language = StringUtils.defaultIfBlank(parent.getLanguage(), "en");
        TokenStream tokenStream = multiAgentWorkerFactory.getWorker(modelName, language, task.getAgentRole())
                .run(buildSubAgentMessage(parent, task), buildChildParameters(parent, task));

        List<ChatResponseBlock> emittedBlocks = new ArrayList<>();
        StringBuilder textBuffer = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicBoolean emittedTaskText = new AtomicBoolean(false);
        List<String> streamedToolCallIds = new ArrayList<>();

        tokenStream.onPartialResponse(content -> {
            if (StringUtils.isNotBlank(content)) {
                textBuffer.append(content);
                emittedTaskText.set(true);
                ChatResponseBlock block = ChatResponseBlock.taskText(
                        task.getRunId(),
                        task.getTaskId(),
                        task.getAgentRole().getCode(),
                        content,
                        true);
                emittedBlocks.add(block);
                emit(parent.getConversationId(), block);
            }
        });

        tokenStream.onPartialToolCallWithContext((partialToolCall, context) -> {
            if (partialToolCall.id() != null) {
                streamedToolCallIds.add(partialToolCall.id());
            }
            ChatResponseBlock block = ChatResponseBlock.toolCall(
                    partialToolCall.id(),
                    partialToolCall.name(),
                    partialToolCall.partialArguments(),
                    true,
                    task.getRunId(),
                    task.getTaskId(),
                    task.getAgentRole().getCode());
            emittedBlocks.add(block);
            emit(parent.getConversationId(), block);
        });

        tokenStream.onIntermediateResponse(response -> {
            if (!response.aiMessage().hasToolExecutionRequests()) {
                return;
            }
            for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
                if (toolRequest.id() != null && streamedToolCallIds.contains(toolRequest.id())) {
                    continue;
                }
                ChatResponseBlock block = ChatResponseBlock.toolCall(
                        toolRequest.id(),
                        toolRequest.name(),
                        toolRequest.arguments(),
                        false,
                        task.getRunId(),
                        task.getTaskId(),
                        task.getAgentRole().getCode());
                emittedBlocks.add(block);
                emit(parent.getConversationId(), block);
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ChatResponseBlock block = ChatResponseBlock.toolResult(
                    toolExecution.request().id(),
                    toolExecution.request().name(),
                    toolExecution.result(),
                    toolExecution.hasFailed(),
                    task.getRunId(),
                    task.getTaskId(),
                    task.getAgentRole().getCode());
            emittedBlocks.add(block);
            emit(parent.getConversationId(), block);
        });

        tokenStream.onCompleteResponse(response -> {
            if (StringUtils.isBlank(textBuffer) && response.aiMessage() != null && StringUtils.isNotBlank(response.aiMessage().text())) {
                textBuffer.append(response.aiMessage().text());
            }
            latch.countDown();
        });

        tokenStream.onError(error -> {
            errorRef.set(error);
            latch.countDown();
        });

        tokenStream.start();

        if (!latch.await(SUB_AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Sub-agent timed out.");
        }
        if (errorRef.get() != null) {
            throw new IllegalStateException(errorRef.get().getMessage(), errorRef.get());
        }

        if (StringUtils.isNotBlank(textBuffer)) {
            ChatResponseBlock block = ChatResponseBlock.taskText(
                    task.getRunId(),
                    task.getTaskId(),
                    task.getAgentRole().getCode(),
                    textBuffer.toString(),
                    false);
            emittedBlocks.add(block);
            if (!emittedTaskText.get()) {
                emit(parent.getConversationId(), block);
            }
        }
        return emittedBlocks;
    }

    private InvocationParameters buildChildParameters(RequestContextInfo parent, MultiAgentTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotNull(map, "userId", parent.getUserId());
        putIfNotNull(map, "conversationId", parent.getConversationId());
        putIfNotNull(map, "connectionId", parent.getConnectionId());
        putIfNotNull(map, "databaseName", parent.getCatalog());
        putIfNotNull(map, "schemaName", parent.getSchema());
        putIfNotNull(map, "modelName", parent.getModelName());
        putIfNotNull(map, "language", parent.getLanguage());
        putIfNotNull(map, "agentMode", parent.getAgentMode());
        putIfNotNull(map, "runId", task.getRunId());
        putIfNotNull(map, "taskId", task.getTaskId());
        putIfNotNull(map, "agentRole", task.getAgentRole().getCode());
        putIfNotNull(map, "parentAgentRole", parent.getAgentRole());
        return InvocationParameters.from(map);
    }

    private String buildSubAgentMessage(RequestContextInfo parent, MultiAgentTask task) {
        return """
                Task title:
                %s

                Delegated instructions:
                %s

                Workspace context:
                - connectionId: %s
                - database: %s
                - schema: %s

                Return a concrete completion result for the orchestrator.
                """
                .formatted(
                        task.getTitle(),
                        task.getGoal(),
                        nullable(parent.getConnectionId()),
                        nullable(parent.getCatalog()),
                        nullable(parent.getSchema()));
    }

    private TaskOutcome summarizeOutcome(MultiAgentTask task, List<ChatResponseBlock> blocks) {
        String details = extractText(blocks);
        boolean hasError = hasToolError(blocks);
        boolean requestedApproval = requestedApproval(blocks);
        boolean executedWrite = usedTool(blocks, ToolNameEnum.EXECUTE_NON_SELECT_SQL.getToolName());

        if (task.getAgentRole() == AgentRoleEnum.SQL_EXECUTOR && requestedApproval && !executedWrite) {
            return new TaskOutcome(
                    "waiting_approval",
                    summarize(StringUtils.defaultIfBlank(details, "Execution is waiting for write approval.")),
                    StringUtils.defaultIfBlank(details, approvalDetails(blocks)),
                    true,
                    approvalDetails(blocks));
        }

        if (hasError) {
            return new TaskOutcome(
                    "failed",
                    summarize(StringUtils.defaultIfBlank(details, "Sub-agent execution failed.")),
                    StringUtils.defaultIfBlank(details, buildToolTimeline(blocks)),
                    false,
                    null);
        }

        return new TaskOutcome(
                "completed",
                summarize(StringUtils.defaultIfBlank(details, task.getTitle() + " completed.")),
                StringUtils.defaultIfBlank(details, buildToolTimeline(blocks)),
                false,
                null);
    }

    private boolean hasToolError(List<ChatResponseBlock> blocks) {
        for (ChatResponseBlock block : blocks) {
            if (!MessageBlockEnum.TOOL_RESULT.name().equals(block.getType()) || StringUtils.isBlank(block.getData())) {
                continue;
            }
            try {
                Map<String, Object> payload = JsonUtil.json2Object(block.getData(), Map.class);
                if (Boolean.TRUE.equals(payload.get("error"))) {
                    return true;
                }
                Object result = payload.get("result");
                if (result instanceof String resultText) {
                    Map<String, Object> parsedResult = JsonUtil.json2Object(resultText, Map.class);
                    if (Boolean.FALSE.equals(parsedResult.get("success"))) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean requestedApproval(List<ChatResponseBlock> blocks) {
        return usedTool(blocks, ToolNameEnum.ASK_USER_CONFIRM.getToolName());
    }

    private boolean usedTool(List<ChatResponseBlock> blocks, String toolName) {
        for (ChatResponseBlock block : blocks) {
            if ((!MessageBlockEnum.TOOL_CALL.name().equals(block.getType())
                    && !MessageBlockEnum.TOOL_RESULT.name().equals(block.getType()))
                    || StringUtils.isBlank(block.getData())) {
                continue;
            }
            try {
                Map<String, Object> payload = JsonUtil.json2Object(block.getData(), Map.class);
                if (toolName.equals(payload.get("toolName"))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private String approvalDetails(List<ChatResponseBlock> blocks) {
        for (ChatResponseBlock block : blocks) {
            if (!MessageBlockEnum.TOOL_CALL.name().equals(block.getType()) || StringUtils.isBlank(block.getData())) {
                continue;
            }
            try {
                Map<String, Object> payload = JsonUtil.json2Object(block.getData(), Map.class);
                if (!ToolNameEnum.ASK_USER_CONFIRM.getToolName().equals(payload.get("toolName"))) {
                    continue;
                }
                Object args = payload.get("arguments");
                if (!(args instanceof String argJson) || StringUtils.isBlank(argJson)) {
                    continue;
                }
                return argJson;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractText(List<ChatResponseBlock> blocks) {
        String latestFinalTaskText = null;
        StringBuilder streamingTaskText = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        for (ChatResponseBlock block : blocks) {
            if (MessageBlockEnum.TASK_TEXT.name().equals(block.getType()) && StringUtils.isNotBlank(block.getData())) {
                try {
                    Map<String, Object> payload = JsonUtil.json2Object(block.getData(), Map.class);
                    String content = Objects.toString(payload.get("content"), "");
                    boolean streaming = Boolean.TRUE.equals(payload.get("streaming"));
                    if (StringUtils.isBlank(content)) {
                        continue;
                    }
                    if (streaming) {
                        streamingTaskText.append(content);
                    } else {
                        latestFinalTaskText = content;
                    }
                    continue;
                } catch (Exception ignored) {
                }
            }
            if ((MessageBlockEnum.TEXT.name().equals(block.getType())
                    || MessageBlockEnum.TASK_RESULT.name().equals(block.getType())
                    || MessageBlockEnum.TASK_APPROVAL.name().equals(block.getType()))
                    && StringUtils.isNotBlank(block.getData())) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                if (MessageBlockEnum.TEXT.name().equals(block.getType())) {
                    sb.append(block.getData());
                    continue;
                }
                try {
                    Map<String, Object> payload = JsonUtil.json2Object(block.getData(), Map.class);
                    String summary = Objects.toString(payload.get("summary"), "");
                    String details = Objects.toString(payload.get("details"), "");
                    if (StringUtils.isNotBlank(details)) {
                        sb.append(details);
                    } else {
                        sb.append(summary);
                    }
                } catch (Exception ignored) {
                    sb.append(block.getData());
                }
            }
        }
        String taskText = StringUtils.isNotBlank(latestFinalTaskText)
                ? latestFinalTaskText
                : streamingTaskText.toString();
        if (StringUtils.isNotBlank(taskText)) {
            if (sb.length() > 0) {
                sb.insert(0, taskText.trim() + '\n');
            } else {
                sb.append(taskText.trim());
            }
        }
        return sb.toString().trim();
    }

    private String buildToolTimeline(List<ChatResponseBlock> blocks) {
        List<String> lines = new ArrayList<>();
        for (ChatResponseBlock block : blocks) {
            if (StringUtils.isBlank(block.getData())) {
                continue;
            }
            try {
                Map<String, Object> payload = JsonUtil.json2Object(block.getData(), Map.class);
                String toolName = Objects.toString(payload.get("toolName"), "");
                if (StringUtils.isBlank(toolName)) {
                    continue;
                }
                if (MessageBlockEnum.TOOL_CALL.name().equals(block.getType())) {
                    lines.add("Called tool: " + toolName);
                } else if (MessageBlockEnum.TOOL_RESULT.name().equals(block.getType())) {
                    lines.add("Tool result: " + toolName);
                }
            } catch (Exception ignored) {
            }
        }
        return String.join("\n", lines);
    }

    private String defaultTitle(AgentRoleEnum role, String title) {
        if (StringUtils.isNotBlank(title)) {
            return title.trim();
        }
        return switch (role) {
            case SCHEMA_ANALYST -> "Schema Analysis";
            case SQL_PLANNER -> "SQL Planning";
            case SQL_EXECUTOR -> "SQL Execution";
            case RESULT_ANALYST -> "Result Analysis";
            case ORCHESTRATOR -> "Orchestration";
        };
    }

    private String summarize(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        return raw.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private String nullable(Object value) {
        return value == null ? "(none)" : String.valueOf(value);
    }

    private void emit(Long conversationId, ChatResponseBlock block) {
        sseEmitterRegistry.emit(conversationId, block);
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private record TaskOutcome(
            String status,
            String summary,
            String details,
            boolean requiresApproval,
            String approvalDetails
    ) {
    }
}
