package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactionContextSupportTest {

    @Test
    void buildContinuationMessage_containsRequiredSections() {
        String message = CompactionContextSupport.buildContinuationMessage("Keep database id 42.", true, true);

        assertTrue(message.contains(CompactionContextSupport.PREAMBLE));
        assertTrue(message.contains("Summary:\nKeep database id 42."));
        assertTrue(message.contains(CompactionContextSupport.RECENT_MESSAGES_PRESERVED));
        assertTrue(message.contains(CompactionContextSupport.DIRECT_RESUME));
    }

    @Test
    void isCompactionContextMessage_onlyRecognizesNewSystemMessage() {
        assertTrue(CompactionContextSupport.isCompactionContextMessage(SystemMessage.from(
                CompactionContextSupport.buildContinuationMessage("facts", true, true)
        )));
        assertFalse(CompactionContextSupport.isCompactionContextMessage(UserMessage.from("[CONVERSATION_SUMMARY]\nfacts")));
        assertFalse(CompactionContextSupport.isCompactionContextMessage(SystemMessage.from("Summary:\nfacts")));
    }

    @Test
    void formatCompactionSummary_stripsAnalysisAndNormalizesSummaryTag() {
        String formatted = CompactionContextSupport.formatCompactionSummary("""
                <analysis>
                hidden reasoning
                </analysis>
                <summary>
                - visible fact
                </summary>
                """);

        assertEquals("Summary:\n- visible fact", formatted);
    }

    @Test
    void extractExistingCompactedSummary_returnsSummaryBody() {
        String message = CompactionContextSupport.buildContinuationMessage("old summary", true, true);

        assertEquals("old summary", CompactionContextSupport.extractExistingCompactedSummary(SystemMessage.from(message)));
    }
}
