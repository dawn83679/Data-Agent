package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiAgentPromptConfig {

    private final Map<String, String> promptCache = new ConcurrentHashMap<>();
    private static final String ENGLISH = "en";

    public String getOrchestratorPrompt(String language) {
        return promptCache.computeIfAbsent("orchestrator::" + ENGLISH,
                ignored -> PromptConfig.loadClassPathResource("prompt/multi-agent/orchestrator_en.md"));
    }

    public String getPrompt(AgentRoleEnum role, String language) {
        String cacheKey = role.getCode() + "::" + ENGLISH;
        return promptCache.computeIfAbsent(cacheKey, ignored ->
                PromptConfig.loadClassPathResource("prompt/multi-agent/%s_en.md".formatted(role.getCode())));
    }
}
