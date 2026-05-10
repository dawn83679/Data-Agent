package edu.zsc.ai.domain.service.ai.autowrite;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.subagent.memorywriter.MemoryWriterAgentService;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.config.ai.SubAgentFactory;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMemoryWriterImpl implements ConversationMemoryWriter {

    private static final Set<String> PROMPT_ROLES = Set.of("USER", "AI");
    private static final int MAX_MESSAGE_CHARS = 1000;

    private final SubAgentFactory subAgentFactory;
    private final AiModelCatalog aiModelCatalog;
    private final MemoryService memoryService;

    @Override
    public void writeMemory(MemoryWriteContext context, Long conversationId, Long userId) {
        LocalDateTime startedAt = LocalDateTime.now();
        String modelName = aiModelCatalog.defaultModelName();
        MemoryWriterAgentService agentService = subAgentFactory.buildMemoryWriterAgent(modelName, conversationId);
        String instruction = buildInstruction(context);

        Map<String, Object> invocationContext = new LinkedHashMap<>();
        invocationContext.put(InvocationContextConstant.USER_ID, userId);
        invocationContext.put(InvocationContextConstant.CONVERSATION_ID, conversationId);
        invocationContext.put(InvocationContextConstant.AGENT_TYPE, AgentTypeEnum.MEMORY_WRITER.getCode());
        invocationContext.put(InvocationContextConstant.AGENT_MODE, AgentModeEnum.AGENT.getCode());
        invocationContext.put(InvocationContextConstant.MODEL_NAME, modelName);

        log.info("[MemAutoWrite] Starting memory writer agent: conversationId={}, userId={}, messageCount={}, instructionLength={}",
                conversationId, userId, context.newMessages().size(), instruction.length());
        String response = agentService.write(instruction, InvocationParameters.from(invocationContext));
        log.info("[MemAutoWrite] Memory writer agent completed: conversationId={}, responsePreview={}",
                conversationId, preview(response));

        AiMemory workingMemory = memoryService.getConversationWorkingMemory(userId, conversationId);
        if (workingMemory == null || workingMemory.getUpdatedAt() == null || workingMemory.getUpdatedAt().isBefore(startedAt)) {
            throw new IllegalStateException("记忆写入 Agent 没有持久化当前会话工作记忆");
        }
    }

    private String buildInstruction(MemoryWriteContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("更新当前会话的后台记忆状态。\n");
        builder.append("这是 CONVERSATION_WORKING_MEMORY 的严格 JSON 草稿生成任务。\n");
        builder.append("你必须先读取当前会话工作记忆，然后通过 updateMemory 写入且只写入一个更新后的 JSON 草稿。\n\n");
        builder.append("必填顶层 JSON 字段：currentTask、activeScope、resolvedMilestones、highPriorityCandidates、userConfirmedFacts、verifiedFindings、decisionPriorities、openQuestions。\n");
        builder.append("未确认的检索信息只能放入 highPriorityCandidates。\n");
        builder.append("用户确认的信息只能放入 userConfirmedFacts。\n");
        builder.append("已验证结果只能放入 verifiedFindings。\n");
        builder.append("verifiedFindings.exactValue 必须精确，不得使用 ~、约、大约、可能、左右。\n");
        builder.append("resolvedMilestones 只保存已经解决且仍会影响下一轮的事项。\n\n");
        builder.append("新的会话片段：\n");
        for (StoredChatMessage message : context.newMessages()) {
            if (!PROMPT_ROLES.contains(message.getRole())) {
                continue;
            }
            String text = extractText(message);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            String normalized = text.length() > MAX_MESSAGE_CHARS
                    ? text.substring(0, MAX_MESSAGE_CHARS) + "...(已截断)"
                    : text;
            builder.append("[").append(message.getRole()).append("] ").append(normalized).append("\n\n");
        }
        builder.append("记住：每次运行都必须写入会话工作记忆，USER 记忆是可选项。");
        return builder.toString();
    }

    private String extractText(StoredChatMessage message) {
        try {
            ChatMessage deserialized = ChatMessageDeserializer.messageFromJson(message.getData());
            if (deserialized instanceof dev.langchain4j.data.message.UserMessage userMessage) {
                return userMessage.singleText();
            }
            if (deserialized instanceof AiMessage aiMessage) {
                return aiMessage.text();
            }
            return null;
        } catch (Exception e) {
            return message.getData();
        }
    }

    private String preview(String value) {
        if (StringUtils.isBlank(value)) {
            return "(空)";
        }
        return StringUtils.abbreviate(value.replace('\n', ' '), 160);
    }
}
