package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.PromptEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MainAgent Prompt v2 — validates the decision-driven paradigm shift.
 * Verifies acceptance criteria from TODO 3.3.
 */
class MainAgentPromptTest {

    private static String promptContent;
    private static String promptContentEn;

    @BeforeAll
    static void loadPrompt() {
        promptContent = PromptConfig.getPrompt(PromptEnum.ZH);
        assertNotNull(promptContent, "MainAgent prompt should load");
        promptContentEn = PromptConfig.getPrompt(PromptEnum.EN);
        assertNotNull(promptContentEn, "MainAgent EN prompt should load");
    }

    // ==================== 3.3.1: No <tool-mastery> ====================

    @Test
    void noToolMasteryBlock() {
        assertFalse(promptContent.contains("<tool-mastery>"),
                "v2 prompt should NOT have <tool-mastery> block");
    }

    @Test
    void noDiscoveryBlock() {
        assertFalse(promptContent.contains("<discovery>"),
                "v2 prompt should NOT have <discovery> block");
    }

    @Test
    void noExecutionBlock() {
        assertFalse(promptContent.contains("<execution>"),
                "v2 prompt should NOT have <execution> block (old style)");
    }

    // ==================== 3.3.2: No SQL expertise ====================

    @Test
    void noSqlOperationBlock() {
        assertFalse(promptContent.contains("<sql-operation>"),
                "v2 prompt should NOT have <sql-operation> block — SQL expertise in Planner");
    }

    @Test
    void noDqlBlock() {
        assertFalse(promptContent.contains("<dql "),
                "v2 prompt should NOT have <dql> block — SQL expertise in Planner");
    }

    @Test
    void noDmlBlock() {
        assertFalse(promptContent.contains("<dml "),
                "v2 prompt should NOT have <dml> block — SQL expertise in Planner");
    }

    @Test
    void noDdlBlock() {
        assertFalse(promptContent.contains("<ddl "),
                "v2 prompt should NOT have <ddl> block — SQL expertise in Planner");
    }

    // ==================== 3.3.3: Decision framework ====================

    @Test
    void workflow_hasPhases() {
        assertTrue(promptContent.contains("阶段 1"), "Should have phase 1");
        assertTrue(promptContent.contains("阶段 2"), "Should have phase 2");
        assertTrue(promptContent.contains("阶段 3"), "Should have phase 3");
        assertTrue(promptContent.contains("阶段 4"), "Should have phase 4");
        assertTrue(promptContent.contains("阶段 5"), "Should have phase 5 (error handling)");
    }

    @Test
    void workflow_usesSemanticConstraintsInsteadOfUserPromptTags() {
        assertTrue(promptContent.contains("稳定偏好"),
                "Prompt should describe stable preferences semantically");
        assertTrue(promptContent.contains("durable context"),
                "Prompt should describe durable context semantically");
        assertTrue(promptContent.contains("不要把任务文本里偶然出现的英文"),
                "Prompt should reject incidental language as an override signal");
        assertTrue(promptContent.contains("确认最终语言、回答格式和可视化方式"),
                "Prompt should require a final preference compliance check");
        assertTrue(promptContent.contains("你的目标不是遵循固定流程"),
                "Prompt should emphasize judgment over rigid workflow");
        assertFalse(promptContent.contains("user_question"),
                "Prompt should not couple to user_question field name");
        assertFalse(promptContent.contains("user_memory"),
                "Prompt should not couple to user_memory field name");
        assertFalse(promptContent.contains("user_preferences"),
                "Prompt should not couple to user_preferences field name");
        assertFalse(promptContent.contains("user_mention"),
                "Prompt should not couple to user_mention field name");
        assertFalse(promptContent.contains("<user_preferences>"),
                "Prompt should not couple to user prompt XML tags");
    }

    @Test
    void workflow_reflectsRuntimeToolFlow() {
        assertTrue(promptContent.contains("最小、最有效的下一步"),
                "Prompt should prefer minimal effective next actions");
        assertTrue(promptContent.contains("searchObjects 适合做轻量候选发现"),
                "Prompt should describe lightweight discovery capability");
        assertTrue(promptContent.contains("简单只读任务通常可以直接调用 executeSelectSql"),
                "Prompt should keep a direct execution path for simple reads");
        assertTrue(promptContent.contains("getEnvironmentOverview：只有当连接或 catalog 本身仍是待判断前提时，才使用"),
                "Prompt should describe environment overview as a runtime scoping tool");
        assertTrue(promptContent.contains("使用 renderChart 交付更直观的结果"),
                "Prompt should include visualization in the runtime workflow");
    }

    @Test
    void workflow_prefersGroundedScopeBeforeEnvironmentWideDiscovery() {
        assertTrue(promptContent.contains("这些线索可以来自当前任务、runtime context、mention、explicit references、durable context，以及 scope hints。"),
                "ZH prompt should name the scope-grounding signals");
        assertTrue(promptContent.contains("不要因为习惯性 discovery 而先扩大到整个环境"),
                "ZH prompt should discourage habitual environment-wide discovery");
        assertTrue(promptContent.contains("只有当连接或 catalog 本身仍是待判断前提时，才使用"),
                "ZH prompt should narrow when getEnvironmentOverview is appropriate");
        assertTrue(promptContent.contains("例如 memory 或当前提示已经指出了具体数据源、数据库或表。"),
                "ZH prompt example B should cover already-grounded object scope");

        assertTrue(promptContentEn.contains("These signals can come from the current task, runtime context, mentions, explicit references, durable context, and scope hints."),
                "EN prompt should name the scope-grounding signals");
        assertTrue(promptContentEn.contains("do not expand back to the whole environment just because discovery is available"),
                "EN prompt should discourage habitual environment-wide discovery");
        assertTrue(promptContentEn.contains("use it only when the available connections or catalogs are themselves part of the decision"),
                "EN prompt should narrow when getEnvironmentOverview is appropriate");
        assertTrue(promptContentEn.contains("memory or the current prompt already points to a specific data source, database, or table"),
                "EN prompt example B should cover already-grounded object scope");
    }

    @Test
    void workflow_usesSimplifiedExamples() {
        assertTrue(promptContent.contains("<examples>"),
                "Prompt should include abstract examples");
        assertTrue(promptContent.contains("示例 A：范围未定"),
                "Prompt should teach scope reduction through examples");
        assertTrue(promptContent.contains("示例 C：结构仍不明确"),
                "Prompt should teach how to resolve structural ambiguity");
        assertTrue(promptContent.contains("示例 D：需要 SQL 方案"),
                "Prompt should include a planner-oriented example");
        assertTrue(promptContent.contains("示例 E：稳定约束已存在"),
                "Prompt should include an example about existing stable constraints");
        assertFalse(promptContent.contains("示例 F：写入记忆"),
                "Prompt examples should stay compact instead of expanding memory-specific cases");
    }

    @Test
    void mentionsCallingSubAgent() {
        assertTrue(promptContent.contains("callingExplorerSubAgent") && promptContent.contains("callingPlannerSubAgent"),
                "v2 prompt should reference callingExplorerSubAgent and callingPlannerSubAgent tools");
    }

    @Test
    void doesNotMentionHiddenPlanTransitionTools() {
        assertFalse(promptContent.contains("enterPlanMode"),
                "Main prompt should not mention hidden enterPlanMode tool");
        assertFalse(promptContent.contains("exitPlanMode"),
                "Main prompt should not mention hidden exitPlanMode tool");
    }

    @Test
    void noOldRoleBlock() {
        long roleBlockLength = 0;
        if (promptContent.contains("<role>")) {
            int start = promptContent.indexOf("<role>");
            int end = promptContent.indexOf("</role>");
            roleBlockLength = promptContent.substring(start, end).lines().count();
        }
        assertTrue(roleBlockLength == 0 || roleBlockLength <= 5,
                "Old verbose <role> block should be removed or very short");
    }

    @Test
    void noPrinciplesBlock() {
        assertFalse(promptContent.contains("<principles>"),
                "v2 uses <iron-rules> instead of <principles>");
    }

    @Test
    void noForbiddenBlock() {
        assertFalse(promptContent.contains("<forbidden>"),
                "v2 merges forbidden into <iron-rules>");
    }

    @Test
    void examplesAvoidBusinessSpecificHardcoding() {
        assertFalse(promptContent.contains("删除所有测试用户"),
                "Prompt examples should not hardcode business-specific cases");
        assertFalse(promptContent.contains("vip_levels"),
                "Prompt examples should remain abstract");
    }
}
