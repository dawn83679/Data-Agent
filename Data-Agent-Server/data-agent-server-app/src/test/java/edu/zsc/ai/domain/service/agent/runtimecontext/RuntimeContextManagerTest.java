package edu.zsc.ai.domain.service.agent.runtimecontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.api.model.request.ChatUserMention;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.service.agent.prompt.PromptRenderResult;
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.DurableFactsStrategy;
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.ExplicitReferencesStrategy;
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.CurrentConversationMemoryStrategy;
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.ResponsePreferencesStrategy;
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.ScopeHintsStrategy;
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.SystemContextStrategy;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;

class RuntimeContextManagerTest {

    @Test
    void render_allSectionsPopulated() {
        RuntimeContextManager manager = createManager();

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("en")
                .currentDate(LocalDate.of(2026, 3, 31))
                .timezone("Asia/Shanghai")
                .memoryPromptContext(MemoryPromptContext.builder()
                        .currentConversationMemory("# Current Task\nImplement the background memory writer\n\n## Confirmed Scope\n- Only the conversation working memory is mandatory.")
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
                                .build()))
                .build();

        PromptRenderResult<RuntimeContextSection> result = manager.render(context);
        String rendered = result.renderedPrompt();

        assertTrue(rendered.contains("<系统上下文 purpose=\"运行时环境\""));
        assertTrue(rendered.contains("今天：2026-03-31"));
        assertTrue(rendered.contains("时区：Asia/Shanghai"));
        assertTrue(rendered.contains("<当前会话记忆 purpose=\"当前任务工作记忆\""));
        assertTrue(rendered.contains("Implement the background memory writer"));

        assertTrue(rendered.contains("<范围提示 purpose=\"查询范围提示\""));
        assertTrue(rendered.contains("Use analytics catalog orders table rather than staging_orders."));

        assertTrue(rendered.contains("<回答偏好 purpose=\"最终回答偏好\""));
        assertTrue(rendered.contains("Use concise explanations with SQL examples."));

        assertTrue(rendered.contains("<持久事实 purpose=\"已验证背景事实\""));
        assertTrue(rendered.contains("Always confirm write SQL against production-like databases."));

        assertTrue(rendered.contains("<显式引用 purpose=\"用户显式引用对象\""));
        assertTrue(rendered.contains("token: @orders; 对象类型: TABLE; 连接: main (id=12)"));

        assertTrue(result.estimatedTokens() > 0);
    }

    @Test
    void render_emptyContext_usesNoneFallbacks() {
        RuntimeContextManager manager = createManager();

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("zh")
                .currentDate(LocalDate.of(2026, 3, 31))
                .timezone("Asia/Shanghai")
                .build();

        PromptRenderResult<RuntimeContextSection> result = manager.render(context);
        String rendered = result.renderedPrompt();

        assertTrue(rendered.contains("请默认遵循以下偏好：\n- none"));
        assertTrue(rendered.contains("已知事实：\n- none"));
        assertTrue(rendered.contains("本轮用户显式引用：\n- none"));
        assertTrue(rendered.contains("当前会话工作记忆：\n- none"));
        assertTrue(rendered.contains("当前任务的范围提示："));
        assertTrue(rendered.contains("- none"));
    }

    @Test
    void render_chineseLanguage_usesChinese() {
        RuntimeContextManager manager = createManager();

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("zh")
                .currentDate(LocalDate.of(2026, 3, 31))
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

        PromptRenderResult<RuntimeContextSection> result = manager.render(context);
        String rendered = result.renderedPrompt();

        assertTrue(rendered.contains("当前运行时环境："));
        assertTrue(rendered.contains("请默认遵循以下偏好："));
        assertTrue(rendered.contains("用户偏好使用中文进行交互"));
        assertTrue(rendered.contains("已知事实：\n- none"));
    }

    @Test
    void render_availableConnectionsSection_includesKnownConnections() {
        RuntimeContextManager manager = createManager();

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("en")
                .currentDate(LocalDate.of(2026, 3, 31))
                .timezone("Asia/Shanghai")
                .build();

        PromptRenderResult<RuntimeContextSection> result = manager.render(context);
        String rendered = result.renderedPrompt();

        assertTrue(!rendered.contains("<available_connections"));
        assertTrue(!rendered.contains("analytics-prod"));
        assertTrue(!rendered.contains("crm-main"));
    }

    @Test
    void render_excludesBrowseOnlyNonPreferenceMemories() {
        RuntimeContextManager manager = createManager();

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("en")
                .currentDate(LocalDate.of(2026, 3, 31))
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
                                                .build()))
                                .build())
                        .build())
                .build();

        PromptRenderResult<RuntimeContextSection> result = manager.render(context);
        String rendered = result.renderedPrompt();

        // Preferences are always included regardless of execution path
        assertTrue(rendered.contains("Use charts with short explanations."));
        // Browse-only non-preference memories are excluded
        assertTrue(!rendered.contains("enterprise_gateway_dev.chat2db_user"));
    }

    @Test
    void render_includesConversationFallbackMemories() {
        RuntimeContextManager manager = createManager();

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("en")
                .currentDate(LocalDate.of(2026, 3, 31))
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
                                                .id(12L)
                                                .scope("USER")
                                                .memoryType("BUSINESS_RULE")
                                                .subType("DOMAIN_RULE")
                                                .content("User-scope fallback should stay out of prompt.")
                                                .score(0.0)
                                                .executionPath(MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_BROWSE_FALLBACK)
                                                .usedFallback(true)
                                                .build()))
                                .build())
                        .build())
                .build();

        PromptRenderResult<RuntimeContextSection> result = manager.render(context);
        String rendered = result.renderedPrompt();

        assertTrue(rendered.contains("Use table analytics.monthly_revenue"));
        assertTrue(!rendered.contains("User-scope fallback should stay out of prompt."));
    }

    @Test
    void render_throwsWhenHandlerMissing() {
        RuntimeContextHandlerChain incompleteChain = new RuntimeContextHandlerChain(List.of(
                new SystemContextStrategy(),
                new ScopeHintsStrategy(),
                new ResponsePreferencesStrategy(),
                new DurableFactsStrategy()));
        RuntimeContextManager manager = new RuntimeContextManager(
                incompleteChain,
                PromptConfig.loadClassPathResource(RuntimeContextManager.TEMPLATE_PATH));

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("en")
                .currentDate(LocalDate.of(2026, 3, 31))
                .timezone("Asia/Shanghai")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> manager.render(context));
        assertTrue(ex.getMessage().contains("No handler matched input"));
    }

    @Test
    void render_sectionOrderMatchesTemplate() {
        RuntimeContextManager manager = createManager();

        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("en")
                .currentDate(LocalDate.of(2026, 3, 31))
                .timezone("Asia/Shanghai")
                .build();

        String rendered = manager.render(context).renderedPrompt();

        int systemCtxPos = rendered.indexOf("<系统上下文");
        int conversationMemoryPos = rendered.indexOf("<当前会话记忆");
        int scopePos = rendered.indexOf("<范围提示");
        int prefPos = rendered.indexOf("<回答偏好");
        int factsPos = rendered.indexOf("<持久事实");
        int refsPos = rendered.indexOf("<显式引用");

        assertTrue(systemCtxPos < conversationMemoryPos);
        assertTrue(conversationMemoryPos < scopePos);
        assertTrue(scopePos < prefPos);
        assertTrue(prefPos < factsPos);
        assertTrue(factsPos < refsPos);
    }

    private RuntimeContextManager createManager() {
        RuntimeContextHandlerChain chain = new RuntimeContextHandlerChain(List.of(
                new SystemContextStrategy(),
                new CurrentConversationMemoryStrategy(),
                new ScopeHintsStrategy(),
                new ResponsePreferencesStrategy(),
                new DurableFactsStrategy(),
                new ExplicitReferencesStrategy()));
        return new RuntimeContextManager(
                chain,
                PromptConfig.loadClassPathResource(RuntimeContextManager.TEMPLATE_PATH));
    }
}
