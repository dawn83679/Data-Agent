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

    @BeforeAll
    static void loadPrompt() {
        promptContent = PromptConfig.getPrompt(PromptEnum.ZH);
        assertNotNull(promptContent, "MainAgent prompt should load");
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
    void workflow_prioritizesMemoryLanguageAndMentionGrounding() {
        assertTrue(promptContent.contains("<user_preferences> 是顶层自然语言偏好区块，也是默认有效的回答协议"),
                "Prompt should keep durable language/output preferences active by default");
        assertTrue(promptContent.contains("只有用户在本轮明确要求切换时才覆盖"),
                "Prompt should require explicit override for response preferences");
        assertTrue(promptContent.contains("不要把 user_question 里偶然出现的英文"),
                "Prompt should reject incidental language as an override signal");
        assertTrue(promptContent.contains("在最终交付前，回看 <user_preferences>"),
                "Prompt should require a final preference compliance check");
        assertTrue(promptContent.contains("user_mention 是 JSON 数组"),
                "Prompt should describe user_mention as structured JSON");
        assertTrue(promptContent.contains("你的目标不是遵循固定流程"),
                "Prompt should emphasize judgment over rigid workflow");
    }

    @Test
    void workflow_allowsDirectReadPathForClearReadOnlyRequests() {
        assertTrue(promptContent.contains("最小、最有效的下一步"),
                "Prompt should prefer minimal effective next actions");
        assertTrue(promptContent.contains("searchObjects 适合做轻量候选发现"),
                "Prompt should describe lightweight discovery capability");
        assertTrue(promptContent.contains("简单只读任务通常可以直接执行"),
                "Prompt should keep a direct execution path for simple reads");
    }

    @Test
    void workflow_usesExamplesInsteadOfBusinessSpecificRules() {
        assertTrue(promptContent.contains("<examples>"),
                "Prompt should include abstract examples");
        assertTrue(promptContent.contains("示例 A：作用域缺失"),
                "Prompt should teach scope reduction through examples");
        assertTrue(promptContent.contains("示例 C：候选不唯一"),
                "Prompt should teach how to handle ambiguous candidates");
        assertTrue(promptContent.contains("示例 D：偏好约束"),
                "Prompt should include a preference-constrained delivery example");
        assertTrue(promptContent.contains("示例 E：读取记忆"),
                "Prompt should include an example about targeted memory recall");
        assertTrue(promptContent.contains("示例 F：写入记忆"),
                "Prompt should include an example about writing durable memory");
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
