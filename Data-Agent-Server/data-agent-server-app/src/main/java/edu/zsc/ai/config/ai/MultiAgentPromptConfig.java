package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiAgentPromptConfig {

    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    public String getOrchestratorPrompt(String language) {
        String lang = resolveLanguage(language);
        return promptCache.computeIfAbsent("orchestrator::" + lang,
                ignored -> PromptConfig.loadClassPathResource(
                        "prompt/multi-agent/orchestrator_%s.md".formatted(lang)));
    }

    public String getPrompt(AgentRoleEnum role, String language) {
        String lang = resolveLanguage(language);
        String cacheKey = role.getCode() + "::" + lang;
        return promptCache.computeIfAbsent(cacheKey, ignored -> {
            String rolePrompt = PromptConfig.loadClassPathResource(
                    "prompt/multi-agent/%s_%s.md".formatted(role.getCode(), lang));
            if (role == AgentRoleEnum.DATA_ANALYST || role == AgentRoleEnum.DATA_WRITER) {
                String sqlRules = PromptConfig.loadClassPathResource(
                        "prompt/shared/sql-rules-%s.md".formatted(lang));
                return rolePrompt + "\n\n" + sqlRules;
            }
            return rolePrompt;
        });
    }

    private static String resolveLanguage(String language) {
        if (language != null && language.trim().toLowerCase().startsWith("zh")) {
            return "zh";
        }
        return "en";
    }
}
