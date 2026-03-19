package edu.zsc.ai.domain.service.agent.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.api.model.request.ChatUserMention;
import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.constant.UserPromptTagConstant;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.service.agent.prompt.strategy.SystemContextPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.SystemReminderPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserMemoryPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserMentionPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserQuestionPromptStrategy;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;

class UserPromptTemplateServiceTest {

    @Test
    void render_buildsMarkdownTemplateWithMinimalBlocks_andStripReturnsOriginalQuestion() {
        UserPromptHandlerChain chain = new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new SystemReminderPromptStrategy(),
                new UserMemoryPromptStrategy(),
                new UserMentionPromptStrategy(),
                new UserQuestionPromptStrategy()));
        UserPromptManager manager = new UserPromptManager(
                chain,
                PromptConfig.loadClassPathResource(UserPromptManager.TEMPLATE_PATH));

        UserPromptAssemblyContext context = UserPromptAssemblyContext.builder()
                .userMessage("Please design a safer SQL migration plan")
                .language("en")
                .agentMode("normal")
                .modelName("qwen3-max")
                .currentDate(LocalDate.of(2026, 3, 18))
                .timezone("Asia/Shanghai")
                .memoryPromptContext(MemoryPromptContext.builder()
                        .recallResult(MemoryRecallResult.builder()
                                .items(List.of(
                                        MemoryRecallItem.builder()
                                                .id(1L)
                                                .memoryType("PREFERENCE")
                                                .content("User prefers concise explanations with SQL examples.")
                                                .score(0.95)
                                                .build(),
                                        MemoryRecallItem.builder()
                                                .id(2L)
                                                .scope("WORKSPACE")
                                                .workspaceLevel("SCHEMA")
                                                .memoryType("BUSINESS_RULE")
                                                .subType("DOMAIN_RULE")
                                                .reviewState("NEEDS_REVIEW")
                                                .content("Always confirm write SQL against production-like databases.")
                                                .score(0.91)
                                                .build()))
                                .build())
                        .build())
                .userMentions(List.of(
                        ChatUserMention.builder()
                                .token("@orders")
                                .objectType("TABLE")
                                .connectionId(12L)
                                .connectionName("main")
                                .catalogName("analytics")
                                .schemaName("public")
                                .objectName("orders")
                                .build(),
                        ChatUserMention.builder()
                                .token("@active_users")
                                .objectType("VIEW")
                                .connectionId(12L)
                                .connectionName("main")
                                .catalogName("analytics")
                                .schemaName("public")
                                .objectName("active_users")
                                .build()))
                .build();

        PromptRenderResult<UserPromptSection> result = manager.render(context);
        String prompt = result.renderedPrompt();

        assertTrue(prompt.contains(UserPromptTagConstant.SYSTEM_CONTEXT_OPEN));
        assertTrue(prompt.contains(UserPromptTagConstant.SYSTEM_REMIDER_OPEN));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MEMORY_OPEN));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MENTION_OPEN));
        assertTrue(prompt.contains("today: 2026-03-18"));
        assertTrue(prompt.contains("timezone: Asia/Shanghai"));
        assertTrue(prompt.contains("language: en"));
        assertTrue(prompt.contains("agent_mode: normal"));
        assertTrue(prompt.contains("model_name: qwen3-max"));
        assertTrue(prompt.contains("User prefers concise explanations with SQL examples."));
        assertTrue(prompt.contains("[WORKSPACE/SCHEMA · BUSINESS_RULE/DOMAIN_RULE · NEEDS_REVIEW]"));
        assertTrue(prompt.contains("Always confirm write SQL against production-like databases."));
        assertTrue(prompt.contains("TABLE:main.analytics.public.orders"));
        assertTrue(prompt.contains("VIEW:main.analytics.public.active_users"));
        assertTrue(prompt.contains("Please design a safer SQL migration plan"));
        assertTrue(result.estimatedTokens() > 0);
        assertEquals("Please design a safer SQL migration plan",
                edu.zsc.ai.agent.memory.MemoryUtil.stripInjectedWrapper(prompt));
    }

    @Test
    void render_usesStableNoneFallbacksForEmptyMemoryAndMentionBlocks() {
        UserPromptHandlerChain chain = new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new SystemReminderPromptStrategy(),
                new UserMemoryPromptStrategy(),
                new UserMentionPromptStrategy(),
                new UserQuestionPromptStrategy()));
        UserPromptManager manager = new UserPromptManager(
                chain,
                PromptConfig.loadClassPathResource(UserPromptManager.TEMPLATE_PATH));

        UserPromptAssemblyContext context = UserPromptAssemblyContext.builder()
                .userMessage("hello")
                .language("zh")
                .agentMode("normal")
                .modelName("qwen3-max")
                .currentDate(LocalDate.of(2026, 3, 19))
                .timezone("Asia/Shanghai")
                .build();

        String prompt = manager.render(context).renderedPrompt();

        assertTrue(prompt.contains(UserPromptTagConstant.USER_MEMORY_OPEN
                + "\n" + PromptConstant.NONE + "\n"
                + UserPromptTagConstant.USER_MEMORY_CLOSE));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MENTION_OPEN
                + "\n" + PromptConstant.NONE + "\n"
                + UserPromptTagConstant.USER_MENTION_CLOSE));
    }
}
