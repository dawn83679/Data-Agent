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

        assertTrue(explorerTasks.contains("探索任务列表"));
        assertTrue(explorerTasks.length() <= 160);
        assertTrue(plannerInstruction.contains("具体 SQL 目标"));
        assertTrue(plannerInstruction.length() <= 80);
        assertTrue(skillName.contains("可用技能列表"));
        assertTrue(skillName.length() <= 60);
    }

    @Test
    void toolUsageRulesRenderNoSkillBoundaryAndToolContracts() {
        ToolUsageRulesSystemPromptStrategy strategy = new ToolUsageRulesSystemPromptStrategy();

        String content = strategy.handle(new PromptHandleRequest<>(
                SystemPromptAssemblyContext.builder()
                        .promptEnum(PromptEnum.ZH)
                        .agentType(AgentTypeEnum.MAIN)
                        .agentMode(AgentModeEnum.AGENT)
                        .language("zh")
                        .availableSkills(List.of())
                        .build(),
                SystemPromptSection.TOOL_USAGE_RULES)).content();

        assertTrue(content.contains("本轮没有可选技能可激活"));
        assertTrue(content.contains("内部工具名"));
        assertTrue(content.contains("工具的用途、参数、前置条件、结果语义"));
    }

    @Test
    void toolUsageRulesAlwaysUseChineseToolContracts() {
        ToolUsageRulesSystemPromptStrategy strategy = new ToolUsageRulesSystemPromptStrategy();

        String content = strategy.handle(new PromptHandleRequest<>(
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
                        .promptEnum(PromptEnum.ZH_PLAN)
                        .agentType(AgentTypeEnum.MAIN)
                        .agentMode(AgentModeEnum.PLAN)
                        .language("zh")
                        .availableSkills(List.of())
                        .build(),
                SystemPromptSection.TOOL_USAGE_RULES)).content();

        assertTrue(content.contains("工具的用途、参数、前置条件、结果语义"));
        assertTrue(content.contains("以工具自身描述、参数结构、运行时权限和工具返回消息为准"));
        assertTrue(content.contains("按工具返回状态继续，不要伪造结果"));
        assertTrue(planContent.contains("计划模式下不要执行 SQL 或写操作"));
    }

    private static void assertShortContractDescription(String description) {
        assertEquals(5, description.lines().count());
        assertTrue(description.contains("价值："));
        assertTrue(description.contains("使用时机："));
        assertTrue(description.contains("前置条件："));
        assertTrue(description.contains("结果："));
        assertTrue(description.contains("边界："));
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
