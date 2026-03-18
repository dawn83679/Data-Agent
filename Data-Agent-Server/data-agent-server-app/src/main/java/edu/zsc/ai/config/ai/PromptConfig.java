package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.PromptEnum;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PromptConfig {

    private static final String CALLING_RULE_RESOURCE = "prompt/calling-rule.md";
    private static final String CALLING_RULE_PLACEHOLDER = "{{CALLING_RULE}}";

    private static final Map<String, String> PROMPT_CACHE = buildPromptCache();

    public static String getPrompt(PromptEnum prompt) {
        return PROMPT_CACHE.get(prompt.getCode());
    }

    private static Map<String, String> buildPromptCache() {
        Map<String, String> cache = new ConcurrentHashMap<>();
        String callingRule = null;

        for (PromptEnum prompt : PromptEnum.values()) {
            String content = loadClassPathResource(prompt.getSystemPromptResource());
            if (content.contains(CALLING_RULE_PLACEHOLDER)) {
                if (callingRule == null) {
                    callingRule = loadCallingRule();
                }
                content = content.replace(CALLING_RULE_PLACEHOLDER, callingRule);
                log.info("Injected calling-rule into prompt: {}", prompt.getCode());
            }
            cache.put(prompt.getCode(), content);
        }
        return cache;
    }

    private static String loadCallingRule() {
        return loadClassPathResource(CALLING_RULE_RESOURCE);
    }

    @SneakyThrows
    public static String loadClassPathResource(String path) {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
