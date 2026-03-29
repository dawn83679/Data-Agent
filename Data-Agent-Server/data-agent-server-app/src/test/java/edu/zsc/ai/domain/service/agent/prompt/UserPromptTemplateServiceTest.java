package edu.zsc.ai.domain.service.agent.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.api.model.request.ChatUserMention;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.constant.UserPromptTagConstant;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.service.agent.prompt.strategy.DurableFactsPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.ScopeHintsPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.SystemContextPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserMentionPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserPreferencesPromptStrategy;
import edu.zsc.ai.domain.service.agent.prompt.strategy.UserQuestionPromptStrategy;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;

class UserPromptTemplateServiceTest {

    @Test
    void render_buildsStructuredTemplateAndStripReturnsOriginalTask() {
        UserPromptManager manager = new UserPromptManager(
                createChain(),
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
                                                .memoryType("KNOWLEDGE_POINT")
                                                .subType("OBJECT_KNOWLEDGE")
                                                .content("Use analytics catalog orders table rather than staging_orders.")
                                                .score(0.92)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_SEMANTIC)
                                                .build(),
                                        MemoryRecallItem.builder()
                                                .id(3L)
                                                .scope("USER")
                                                .memoryType("BUSINESS_RULE")
                                                .subType("DOMAIN_RULE")
                                                .content("Always confirm write SQL against production-like databases.")
                                                .score(0.91)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC)
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

        assertTrue(prompt.contains("<system_context purpose=\"runtime_environment\""));
        assertTrue(prompt.contains("<task purpose=\"current_user_goal\""));
        assertTrue(prompt.contains("<response_preferences purpose=\"final_response_preferences\""));
        assertTrue(prompt.contains("<scope_hints purpose=\"query_scope_guidance\""));
        assertTrue(prompt.contains("<durable_facts purpose=\"verified_background_facts\""));
        assertTrue(prompt.contains("<explicit_references purpose=\"user_explicit_object_selection\""));
        assertTrue(prompt.contains("Current runtime environment:"));
        assertTrue(prompt.contains("Current task:"));
        assertTrue(prompt.contains("Treat the following as the preferred starting scope for this task."));
        assertTrue(prompt.contains("If the current hints are already specific enough, stay within that scope before considering broader discovery."));
        assertTrue(prompt.contains("Apply the following preferences by default:"));
        assertTrue(prompt.contains("Helpful scope hints for this task:"));
        assertTrue(prompt.contains("Known durable facts:"));
        assertTrue(prompt.contains("The user explicitly referenced:"));
        assertTrue(prompt.contains("today: 2026-03-18"));
        assertTrue(prompt.contains("timezone: Asia/Shanghai"));
        assertTrue(!prompt.contains("language: en"));
        assertTrue(!prompt.contains("agent_mode: normal"));
        assertTrue(!prompt.contains("model_name: qwen3-max"));
        assertTrue(prompt.contains("- Use concise explanations with SQL examples."));
        assertTrue(prompt.contains("Use analytics catalog orders table rather than staging_orders."));
        assertTrue(!prompt.contains("[USER · KNOWLEDGE_POINT/OBJECT_KNOWLEDGE]"));
        assertTrue(prompt.contains("[USER · BUSINESS_RULE/DOMAIN_RULE]"));
        assertTrue(prompt.contains("Always confirm write SQL against production-like databases."));
        assertTrue(prompt.contains("token: @orders; object_type: TABLE; connection: main (id=12); catalog: analytics; schema: public; object: orders"));
        assertTrue(prompt.contains("token: @active_users; object_type: VIEW; connection: main (id=12); catalog: analytics; schema: public; object: active_users"));
        assertTrue(prompt.contains("Please design a safer SQL migration plan"));
        assertTrue(prompt.indexOf("<task purpose=\"current_user_goal\"") < prompt.indexOf("<scope_hints purpose=\"query_scope_guidance\""));
        assertTrue(prompt.indexOf("<scope_hints purpose=\"query_scope_guidance\"") < prompt.indexOf("<response_preferences purpose=\"final_response_preferences\""));
        assertTrue(prompt.indexOf("<scope_hints purpose=\"query_scope_guidance\"") < prompt.indexOf("<durable_facts purpose=\"verified_background_facts\""));
        assertTrue(result.estimatedTokens() > 0);
        assertEquals("Please design a safer SQL migration plan",
                edu.zsc.ai.agent.memory.MemoryUtil.stripInjectedWrapper(prompt));
    }

    @Test
    void render_placesNaturalLanguagePreferencesInResponsePreferencesAndLeavesOtherBlocksEmpty() {
        UserPromptManager manager = new UserPromptManager(
                createChain(),
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

        assertTrue(prompt.contains("<response_preferences purpose=\"final_response_preferences\""));
        assertTrue(prompt.contains("请默认遵循以下偏好："));
        assertTrue(prompt.contains("- 用户偏好使用中文进行交互"));
        assertTrue(prompt.contains("<scope_hints purpose=\"query_scope_guidance\""));
        assertTrue(prompt.contains("Helpful scope hints for this task:\n\nTreat the following as the preferred starting scope for this task.\nIf the current hints are already specific enough, stay within that scope before considering broader discovery.\n\n- none"));
        assertTrue(prompt.contains("<durable_facts purpose=\"verified_background_facts\""));
        assertTrue(prompt.contains("已知事实：\n- none"));
        assertTrue(!prompt.contains("<preference>"));
        assertTrue(!prompt.contains("LANGUAGE_PREFERENCE and RESPONSE_FORMAT from "));
    }

    @Test
    void render_usesStableNoneFallbacksForEmptyBlocks() {
        UserPromptManager manager = new UserPromptManager(
                createChain(),
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

        assertTrue(prompt.contains("请默认遵循以下偏好：\n- none"));
        assertTrue(prompt.contains("Helpful scope hints for this task:\n\nTreat the following as the preferred starting scope for this task.\nIf the current hints are already specific enough, stay within that scope before considering broader discovery.\n\n- none"));
        assertTrue(prompt.contains("已知事实：\n- none"));
        assertTrue(prompt.contains("本轮用户显式引用：\n- none"));
    }

    @Test
    void render_excludesBrowseOnlyNonPreferenceMemoriesFromPromptScopeAndFacts() {
        UserPromptManager manager = new UserPromptManager(
                createChain(),
                PromptConfig.loadClassPathResource(UserPromptManager.TEMPLATE_PATH));

        UserPromptAssemblyContext context = UserPromptAssemblyContext.builder()
                .userMessage("Please analyze 2013 monthly revenue")
                .language("en")
                .agentMode("normal")
                .modelName("qwen3-max")
                .currentDate(LocalDate.of(2026, 3, 24))
                .timezone("Asia/Shanghai")
                .memoryPromptContext(MemoryPromptContext.builder()
                        .recallResult(MemoryRecallResult.builder()
                                .items(List.of(
                                        MemoryRecallItem.builder()
                                                .id(1L)
                                                .memoryType("PREFERENCE")
                                                .subType("RESPONSE_FORMAT")
                                                .content("Use charts with short explanations.")
                                                .score(0.98)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_BROWSE)
                                                .usedFallback(false)
                                                .build(),
                                        MemoryRecallItem.builder()
                                                .id(2L)
                                                .scope("USER")
                                                .memoryType("KNOWLEDGE_POINT")
                                                .subType("OBJECT_KNOWLEDGE")
                                                .content("Use enterprise_gateway_dev.chat2db_user for registration analysis.")
                                                .score(0.0)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_BROWSE)
                                                .usedFallback(false)
                                                .build(),
                                        MemoryRecallItem.builder()
                                                .id(3L)
                                                .scope("USER")
                                                .memoryType("BUSINESS_RULE")
                                                .subType("DOMAIN_RULE")
                                                .content("Registration queries should stay in test3.")
                                                .score(0.0)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_BROWSE_FALLBACK)
                                                .usedFallback(true)
                                                .build()))
                                .build())
                        .build())
                .build();

        String prompt = manager.render(context).renderedPrompt();

        assertTrue(prompt.contains("- Use charts with short explanations."));
        assertTrue(prompt.contains("Helpful scope hints for this task:\n\nTreat the following as the preferred starting scope for this task.\nIf the current hints are already specific enough, stay within that scope before considering broader discovery.\n\n- none"));
        assertTrue(prompt.contains("Known durable facts:\n- none"));
        assertTrue(!prompt.contains("enterprise_gateway_dev.chat2db_user"));
        assertTrue(!prompt.contains("Registration queries should stay in test3."));
    }

    @Test
    void render_includesConversationFallbackMemoriesInPromptButStillExcludesUserFallbackMemories() {
        UserPromptManager manager = new UserPromptManager(
                createChain(),
                PromptConfig.loadClassPathResource(UserPromptManager.TEMPLATE_PATH));

        UserPromptAssemblyContext context = UserPromptAssemblyContext.builder()
                .userMessage("Please analyze 2013 monthly revenue")
                .language("en")
                .agentMode("normal")
                .modelName("qwen3-max")
                .currentDate(LocalDate.of(2026, 3, 24))
                .timezone("Asia/Shanghai")
                .memoryPromptContext(MemoryPromptContext.builder()
                        .recallResult(MemoryRecallResult.builder()
                                .items(List.of(
                                        MemoryRecallItem.builder()
                                                .id(10L)
                                                .scope("CONVERSATION")
                                                .memoryType("WORKFLOW_CONSTRAINT")
                                                .subType("PROCESS_RULE")
                                                .content("Use table analytics.monthly_revenue rather than staging.monthly_revenue for this task.")
                                                .score(0.0)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_CONVERSATION_BROWSE_FALLBACK)
                                                .usedFallback(true)
                                                .build(),
                                        MemoryRecallItem.builder()
                                                .id(11L)
                                                .scope("CONVERSATION")
                                                .memoryType("KNOWLEDGE_POINT")
                                                .subType("OBJECT_KNOWLEDGE")
                                                .content("The authoritative monthly revenue view is analytics.public.monthly_revenue.")
                                                .score(0.0)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_CONVERSATION_BROWSE_FALLBACK)
                                                .usedFallback(true)
                                                .build(),
                                        MemoryRecallItem.builder()
                                                .id(12L)
                                                .scope("USER")
                                                .memoryType("BUSINESS_RULE")
                                                .subType("DOMAIN_RULE")
                                                .content("User-scope fallback memory should still stay out of the prompt.")
                                                .score(0.0)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_BROWSE_FALLBACK)
                                                .usedFallback(true)
                                                .build()))
                                .build())
                        .build())
                .build();

        String prompt = manager.render(context).renderedPrompt();

        assertTrue(prompt.contains("Use table analytics.monthly_revenue rather than staging.monthly_revenue for this task."));
        assertTrue(prompt.contains("The authoritative monthly revenue view is analytics.public.monthly_revenue."));
        assertTrue(!prompt.contains("User-scope fallback memory should still stay out of the prompt."));
    }

    @Test
    void render_throwsWhenSectionHandlerIsMissing() {
        UserPromptHandlerChain chain = new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new UserPreferencesPromptStrategy(),
                new ScopeHintsPromptStrategy(),
                new DurableFactsPromptStrategy(),
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

    private UserPromptHandlerChain createChain() {
        return new UserPromptHandlerChain(List.of(
                new SystemContextPromptStrategy(),
                new UserQuestionPromptStrategy(),
                new UserPreferencesPromptStrategy(),
                new ScopeHintsPromptStrategy(),
                new DurableFactsPromptStrategy(),
                new UserMentionPromptStrategy()));
    }
}
