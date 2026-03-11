package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiAgentPromptConfigTest {

    private final MultiAgentPromptConfig promptConfig = new MultiAgentPromptConfig();

    @Test
    void shouldAlwaysLoadEnglishOrchestratorPrompt() {
        String prompt = promptConfig.getOrchestratorPrompt("zh");

        assertTrue(prompt.contains("orchestrator") || prompt.contains("Orchestrator") || prompt.contains("Dax"));
    }

    @Test
    void shouldAlwaysLoadEnglishSubAgentPrompt() {
        String prompt = promptConfig.getPrompt(AgentRoleEnum.SCHEMA_EXPLORER, "zh");

        assertTrue(prompt.contains("schema") || prompt.contains("discovery") || prompt.contains("Schema"));
    }
}
