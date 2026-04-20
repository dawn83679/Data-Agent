package edu.zsc.ai.domain.service.agent.systemprompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.sql.GetConnectionsTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchQuery;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.systemprompt.strategy.ToolUsageRulesSystemPromptStrategy;

class ToolDescriptionAndPromptSeparationTest {

    @Test
    void representativeToolDescriptions_keepContractLanguageInsteadOfWorkflowOrdering() throws Exception {
        String searchObjectsDescription = readToolDescription(
                SearchObjectsTool.class,
                "searchObjects",
                ObjectSearchQuery.class,
                InvocationParameters.class);
        String getObjectDetailDescription = readToolDescription(
                GetObjectDetailTool.class,
                "getObjectDetail",
                List.class,
                InvocationParameters.class);
        String executeSelectSqlDescription = readToolDescription(
                ExecuteSqlTool.class,
                "executeSelectSql",
                Long.class,
                String.class,
                String.class,
                List.class,
                InvocationParameters.class);
        String getConnectionsDescription = readToolDescription(
                GetConnectionsTool.class,
                "getConnections",
                InvocationParameters.class);

        assertTrue(searchObjectsDescription.contains("Value:"));
        assertTrue(searchObjectsDescription.contains("Preconditions:"));
        assertFalse(searchObjectsDescription.contains("Relation:"));
        assertFalse(searchObjectsDescription.contains("Use When:"));

        assertTrue(getObjectDetailDescription.contains("Value:"));
        assertTrue(getObjectDetailDescription.contains("Preconditions:"));
        assertFalse(getObjectDetailDescription.contains("Relation:"));
        assertFalse(getObjectDetailDescription.contains("Use When:"));

        assertTrue(executeSelectSqlDescription.contains("Value:"));
        assertTrue(executeSelectSqlDescription.contains("Preconditions:"));
        assertFalse(executeSelectSqlDescription.contains("Relation:"));
        assertFalse(executeSelectSqlDescription.contains("Use When:"));

        assertTrue(getConnectionsDescription.contains("Value:"));
        assertTrue(getConnectionsDescription.contains("Preconditions:"));
        assertFalse(getConnectionsDescription.contains("Relation:"));
        assertFalse(getConnectionsDescription.contains("Use When:"));
    }

    @Test
    void representativeToolPrompts_useWorkflowLanguageWithoutRepeatingRepresentativeContracts() {
        ResourceBundleMessageSource messageSource = bundleMessageSource();

        String searchObjectsPrompt = messageSource.getMessage("agent.tool_prompt.search_objects", null, Locale.US);
        String getObjectDetailPrompt = messageSource.getMessage("agent.tool_prompt.get_object_detail", null, Locale.US);
        String executeSelectSqlPrompt = messageSource.getMessage("agent.tool_prompt.execute_select_sql", null, Locale.US);

        assertTrue(searchObjectsPrompt.contains("ask the user"));
        assertTrue(searchObjectsPrompt.contains("explorer"));
        assertFalse(searchObjectsPrompt.contains("wildcards"));
        assertFalse(searchObjectsPrompt.contains("capped at 100"));

        assertTrue(getObjectDetailPrompt.contains("If a detail lookup fails"));
        assertTrue(getObjectDetailPrompt.contains("before generating SQL"));
        assertFalse(getObjectDetailPrompt.contains("DDL"));
        assertFalse(getObjectDetailPrompt.contains("row counts"));
        assertFalse(getObjectDetailPrompt.contains("indexes"));

        assertTrue(executeSelectSqlPrompt.contains("switch from discovery to answer synthesis"));
        assertTrue(executeSelectSqlPrompt.contains("chart or export"));
        assertFalse(executeSelectSqlPrompt.contains("read-only SQL"));
        assertFalse(executeSelectSqlPrompt.contains("results array"));
    }

    @Test
    void representativeToolPrompts_keepTheSameWorkflowFocusInChinese() {
        ResourceBundleMessageSource messageSource = bundleMessageSource();

        String searchObjectsPrompt = messageSource.getMessage("agent.tool_prompt.search_objects", null, Locale.SIMPLIFIED_CHINESE);
        String getObjectDetailPrompt = messageSource.getMessage("agent.tool_prompt.get_object_detail", null, Locale.SIMPLIFIED_CHINESE);
        String executeSelectSqlPrompt = messageSource.getMessage("agent.tool_prompt.execute_select_sql", null, Locale.SIMPLIFIED_CHINESE);

        assertTrue(searchObjectsPrompt.contains("先向用户确认"));
        assertTrue(searchObjectsPrompt.contains("Explorer"));

        assertTrue(getObjectDetailPrompt.contains("如果某个对象的 detail 获取失败"));
        assertTrue(getObjectDetailPrompt.contains("生成 SQL 之前"));

        assertTrue(executeSelectSqlPrompt.contains("从发现模式切换到回答整理"));
        assertTrue(executeSelectSqlPrompt.contains("图表或导出"));
    }

    @Test
    void missingPromptKeys_surfaceTheKeyStringInRenderedRules() {
        ToolUsageRulesSystemPromptStrategy strategy = new ToolUsageRulesSystemPromptStrategy();

        String content = strategy.handle(new PromptHandleRequest<>(
                SystemPromptAssemblyContext.builder()
                        .promptEnum(PromptEnum.EN)
                        .agentType(AgentTypeEnum.MAIN)
                        .agentMode(AgentModeEnum.AGENT)
                        .language("en")
                        .availableSkills(List.of())
                        .build(),
                SystemPromptSection.TOOL_USAGE_RULES)).content();

        assertTrue(content.contains("no optional skills are available"));
        assertTrue(content.contains("internal tool names"));
    }

    @Test
    void toolUsageRulesMentionRoutingGuidanceByLanguage() {
        ToolUsageRulesSystemPromptStrategy strategy = new ToolUsageRulesSystemPromptStrategy();

        String englishContent = strategy.handle(new PromptHandleRequest<>(
                SystemPromptAssemblyContext.builder()
                        .promptEnum(PromptEnum.EN)
                        .agentType(AgentTypeEnum.MAIN)
                        .agentMode(AgentModeEnum.AGENT)
                        .language("en")
                        .availableSkills(List.of())
                        .build(),
                SystemPromptSection.TOOL_USAGE_RULES)).content();

        String chineseContent = strategy.handle(new PromptHandleRequest<>(
                SystemPromptAssemblyContext.builder()
                        .promptEnum(PromptEnum.ZH)
                        .agentType(AgentTypeEnum.MAIN)
                        .agentMode(AgentModeEnum.AGENT)
                        .language("zh")
                        .availableSkills(List.of())
                        .build(),
                SystemPromptSection.TOOL_USAGE_RULES)).content();

        assertTrue(englishContent.contains("Only call getConnections"));
        assertTrue(englishContent.contains("explicit references"));
        assertTrue(chineseContent.contains("仅在当前轮次"));
        assertTrue(chineseContent.contains("显式引用"));
    }

    private static ResourceBundleMessageSource bundleMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    private static String readToolDescription(Class<?> toolClass,
                                              String methodName,
                                              Class<?>... parameterTypes) throws Exception {
        Method method = toolClass.getDeclaredMethod(methodName, parameterTypes);
        Tool annotation = method.getAnnotation(Tool.class);
        return String.join("\n", annotation.value());
    }
}
