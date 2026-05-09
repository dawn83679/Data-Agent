package edu.zsc.ai.domain.service.agent.systemprompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.ask.AskUserQuestionTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.export.ExportFileTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingExplorerTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingPlannerTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.sql.GetConnectionsTool;
import edu.zsc.ai.agent.tool.sql.GetDatabasesTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.GetSchemasTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchQuery;
import edu.zsc.ai.agent.tool.todo.TodoActionEnum;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.systemprompt.strategy.ToolUsageRulesSystemPromptStrategy;

class ToolDescriptionAndPromptSeparationTest {

    @Test
    void representativeToolDescriptions_useFiveLineContracts() throws Exception {
        List<String> descriptions = List.of(
                readToolDescription(GetConnectionsTool.class, "getConnections", String.class, InvocationParameters.class),
                readToolDescription(GetDatabasesTool.class, "getDatabases", Long.class, String.class, InvocationParameters.class),
                readToolDescription(GetSchemasTool.class, "getSchemas", Long.class, String.class, String.class, InvocationParameters.class),
                readToolDescription(SearchObjectsTool.class, "searchObjects", ObjectSearchQuery.class, String.class, InvocationParameters.class),
                readToolDescription(GetObjectDetailTool.class, "getObjectDetail", List.class, String.class, InvocationParameters.class),
                readToolDescription(CallingExplorerTool.class, "callingExplorerSubAgent", List.class, Long.class, InvocationParameters.class),
                readToolDescription(CallingPlannerTool.class, "callingPlannerSubAgent", String.class, String.class, Long.class, InvocationParameters.class),
                readToolDescription(ExecuteSqlTool.class, "executeSelectSql", Long.class, String.class, String.class, List.class, String.class, InvocationParameters.class),
                readToolDescription(ExecuteSqlTool.class, "executeNonSelectSql", Long.class, String.class, String.class, List.class, String.class, InvocationParameters.class),
                readToolDescription(AskUserQuestionTool.class, "askUserQuestion", List.class, InvocationParameters.class),
                readToolDescription(TodoTool.class, "todoWrite", TodoActionEnum.class, String.class, List.class),
                readToolDescription(ActivateSkillTool.class, "activateSkill", String.class),
                readToolDescription(ChartTool.class, "renderChart", String.class, String.class, String.class),
                readToolDescription(ExportFileTool.class, "exportFile", String.class, List.class, List.class, String.class, InvocationParameters.class)
        );

        descriptions.forEach(ToolDescriptionAndPromptSeparationTest::assertShortContractDescription);
    }

    @Test
    void representativeParameterDescriptionsStayCompact() throws Exception {
        String explorerTasks = readParameterDescription(
                CallingExplorerTool.class,
                "callingExplorerSubAgent",
                0,
                List.class,
                Long.class,
                InvocationParameters.class);
        String plannerInstruction = readParameterDescription(
                CallingPlannerTool.class,
                "callingPlannerSubAgent",
                0,
                String.class,
                String.class,
                Long.class,
                InvocationParameters.class);
        String skillName = readParameterDescription(
                ActivateSkillTool.class,
                "activateSkill",
                0,
                String.class);

        assertTrue(explorerTasks.contains("Explorer tasks."));
        assertTrue(explorerTasks.length() <= 130);
        assertTrue(plannerInstruction.contains("One concrete SQL goal"));
        assertTrue(plannerInstruction.length() <= 80);
        assertTrue(skillName.contains("<skill_available>"));
        assertTrue(skillName.length() <= 50);
    }

    @Test
    void toolUsageRulesRenderNoSkillBoundaryAndToolContracts() {
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
        assertTrue(content.contains("Tool purpose, parameters, preconditions, result semantics"));
    }

    @Test
    void toolUsageRulesUseToolContractsByLanguage() {
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

        String planContent = strategy.handle(new PromptHandleRequest<>(
                SystemPromptAssemblyContext.builder()
                        .promptEnum(PromptEnum.EN_PLAN)
                        .agentType(AgentTypeEnum.MAIN)
                        .agentMode(AgentModeEnum.PLAN)
                        .language("en")
                        .availableSkills(List.of())
                        .build(),
                SystemPromptSection.TOOL_USAGE_RULES)).content();

        assertTrue(englishContent.contains("Tool purpose, parameters, preconditions, result semantics"));
        assertTrue(englishContent.contains("description, schema, runtime permissions"));
        assertTrue(englishContent.contains("follow its returned state instead of fabricating results"));
        assertTrue(chineseContent.contains("工具的用途、参数、前置条件、结果语义"));
        assertTrue(chineseContent.contains("description、schema、runtime 权限"));
        assertTrue(chineseContent.contains("按工具返回状态继续"));
        assertTrue(planContent.contains("In Plan mode, do not execute SQL or writes"));
    }

    private static void assertShortContractDescription(String description) {
        assertEquals(5, description.lines().count());
        assertTrue(description.contains("Value:"));
        assertTrue(description.contains("Use When:"));
        assertTrue(description.contains("Preconditions:"));
        assertTrue(description.contains("Result:"));
        assertTrue(description.contains("Boundary:"));
    }

    private static String readToolDescription(Class<?> toolClass,
                                              String methodName,
                                              Class<?>... parameterTypes) throws Exception {
        Method method = toolClass.getDeclaredMethod(methodName, parameterTypes);
        Tool annotation = method.getAnnotation(Tool.class);
        return String.join("\n", annotation.value());
    }

    private static String readParameterDescription(Class<?> toolClass,
                                                   String methodName,
                                                   int parameterIndex,
                                                   Class<?>... parameterTypes) throws Exception {
        Method method = toolClass.getDeclaredMethod(methodName, parameterTypes);
        P annotation = method.getParameters()[parameterIndex].getAnnotation(P.class);
        return annotation.value();
    }
}
