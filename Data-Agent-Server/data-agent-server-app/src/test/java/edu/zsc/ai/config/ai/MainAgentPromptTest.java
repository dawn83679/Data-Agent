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
}
