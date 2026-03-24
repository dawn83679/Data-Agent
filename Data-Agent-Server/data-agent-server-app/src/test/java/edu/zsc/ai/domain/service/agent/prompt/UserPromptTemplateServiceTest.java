package edu.zsc.ai.domain.service.agent.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.api.model.request.ChatUserMention;
import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.constant.UserPromptTagConstant;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.service.agent.prompt.strategy.SystemContextPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserMemoryPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserMentionPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserPreferencesPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserQuestionPromptStrategy;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;

class UserPromptTemplateServiceTest {

    @Test
    void render_buildsMarkdownTemplateWithMinimalBlocks_andStripReturnsOriginalQuestion() {
        UserPromptHandlerChain chain = new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new UserMemoryPromptStrategy(),
                new UserPreferencesPromptStrategy(),
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
                                                .subType("RESPONSE_FORMAT")
                                                .content("Use concise explanations with SQL examples.")
                                                .score(0.95)
                                                .build(),
                                        MemoryRecallItem.builder()
                                                .id(2L)
                                                .scope("USER")
                                                .memoryType("BUSINESS_RULE")
                                                .subType("DOMAIN_RULE")
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
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MEMORY_OPEN));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_PREFERENCES_OPEN));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MENTION_OPEN));
        assertTrue(prompt.contains("today: 2026-03-18"));
        assertTrue(prompt.contains("timezone: Asia/Shanghai"));
        assertTrue(!prompt.contains("language: en"));
        assertTrue(!prompt.contains("agent_mode: normal"));
        assertTrue(!prompt.contains("model_name: qwen3-max"));
        assertTrue(prompt.contains("Preferred response format: Use concise explanations with SQL examples."));
        assertTrue(prompt.contains("[USER · BUSINESS_RULE/DOMAIN_RULE]"));
        assertTrue(prompt.contains("Always confirm write SQL against production-like databases."));
        assertTrue(prompt.contains("\"token\":\"@orders\""));
        assertTrue(prompt.contains("\"objectType\":\"TABLE\""));
        assertTrue(prompt.contains("\"connectionId\":12"));
        assertTrue(prompt.contains("\"catalogName\":\"analytics\""));
        assertTrue(prompt.contains("\"schemaName\":\"public\""));
        assertTrue(prompt.contains("\"objectName\":\"orders\""));
        assertTrue(prompt.contains("\"token\":\"@active_users\""));
        assertTrue(prompt.contains("\"objectType\":\"VIEW\""));
        assertTrue(prompt.contains("Please design a safer SQL migration plan"));
        assertTrue(prompt.lastIndexOf(UserPromptTagConstant.USER_MEMORY_OPEN) < prompt.lastIndexOf(UserPromptTagConstant.USER_PREFERENCES_OPEN));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MEMORY_OPEN
                + "\n- [USER · BUSINESS_RULE/DOMAIN_RULE] Always confirm write SQL against production-like databases.\n"
                + UserPromptTagConstant.USER_MEMORY_CLOSE));
        assertTrue(result.estimatedTokens() > 0);
        assertEquals("Please design a safer SQL migration plan",
                edu.zsc.ai.agent.memory.MemoryUtil.stripInjectedWrapper(prompt));
    }

    @Test
    void render_placesNaturalLanguagePreferencesInTopLevelSection() {
        UserPromptHandlerChain chain = new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new UserMemoryPromptStrategy(),
                new UserPreferencesPromptStrategy(),
                new UserMentionPromptStrategy(),
                new UserQuestionPromptStrategy()));
        UserPromptManager manager = new UserPromptManager(
                chain,
                PromptConfig.loadClassPathResource(UserPromptManager.TEMPLATE_PATH));

        UserPromptAssemblyContext context = UserPromptAssemblyContext.builder()
                .userMessage("Please check monthly sales")
                .language("zh")
                .agentMode("normal")
                .modelName("qwen3.5-plus")
                .currentDate(LocalDate.of(2026, 3, 19))
                .timezone("Asia/Shanghai")
                .memoryPromptContext(MemoryPromptContext.builder()
                        .recallResult(MemoryRecallResult.builder()
                                .items(List.of(
                                        MemoryRecallItem.builder()
                                                .id(1L)
                                                .memoryType("PREFERENCE")
                                                .subType("LANGUAGE_PREFERENCE")
                                                .content("用户偏好使用中文进行交互")
                                                .score(0.98)
                                                .build()))
                                .build())
                        .build())
                .build();

        String prompt = manager.render(context).renderedPrompt();

        assertTrue(prompt.contains(UserPromptTagConstant.USER_PREFERENCES_OPEN));
        assertTrue(prompt.contains("Preferred response language: 用户偏好使用中文进行交互"));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_PREFERENCES_OPEN
                + "\nPreferred response language: 用户偏好使用中文进行交互\n"
                + UserPromptTagConstant.USER_PREFERENCES_CLOSE));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MEMORY_OPEN
                + "\n" + PromptConstant.NONE + "\n"
                + UserPromptTagConstant.USER_MEMORY_CLOSE));
        assertTrue(!prompt.contains("<preference>"));
        assertTrue(!prompt.contains("LANGUAGE_PREFERENCE and RESPONSE_FORMAT from "));
    }

    @Test
    void render_usesStableNoneFallbacksForEmptyMemoryAndMentionBlocks() {
        UserPromptHandlerChain chain = new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new UserMemoryPromptStrategy(),
                new UserPreferencesPromptStrategy(),
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
        assertTrue(prompt.contains(UserPromptTagConstant.USER_PREFERENCES_OPEN
                + "\n" + PromptConstant.NONE + "\n"
                + UserPromptTagConstant.USER_PREFERENCES_CLOSE));
        assertTrue(prompt.contains(UserPromptTagConstant.USER_MENTION_OPEN
                + "\n" + PromptConstant.NONE + "\n"
                + UserPromptTagConstant.USER_MENTION_CLOSE));
    }

    @Test
    void render_throwsWhenSectionHandlerIsMissing() {
        UserPromptHandlerChain chain = new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new UserMemoryPromptStrategy(),
                new UserPreferencesPromptStrategy(),
                new UserMentionPromptStrategy()));
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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> manager.render(context));
        assertTrue(exception.getMessage().contains("No handler matched input"));
    }
}
