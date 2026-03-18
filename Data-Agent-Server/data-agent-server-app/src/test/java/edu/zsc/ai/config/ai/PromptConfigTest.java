package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.PromptEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PromptConfig — resource loading and placeholder injection.
 */
class PromptConfigTest {

    @Test
    void allPromptEnums_loadSuccessfully() {
        for (PromptEnum prompt : PromptEnum.values()) {
            String content = PromptConfig.getPrompt(prompt);
            assertNotNull(content, "Prompt " + prompt.getCode() + " should load");
            assertFalse(content.isBlank(), "Prompt " + prompt.getCode() + " should not be blank");
        }
    }

    @Test
    void explorerPrompt_noPlaceholderIssue() {
        String content = PromptConfig.getPrompt(PromptEnum.EXPLORER);
        assertFalse(content.contains("{{CALLING_RULE}}"),
                "Explorer prompt should not have unreplaced placeholders");
    }

    @Test
    void plannerPrompt_noPlaceholderIssue() {
        String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
        assertFalse(content.contains("{{CALLING_RULE}}"),
                "Planner prompt should not have unreplaced placeholders");
    }

    @Test
    void explorerPrompt_mentionsRelevanceScoreThresholds() {
        String content = PromptConfig.getPrompt(PromptEnum.EXPLORER);
        assertTrue(content.contains("relevanceScore"));
        assertTrue(content.contains("0-100"));
        assertTrue(content.contains("80-100"));
        assertTrue(content.contains("50-79"));
        assertTrue(content.contains("0-49"));
    }
}
