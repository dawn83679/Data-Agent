package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.PromptEnum;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PromptConfig {

    private static final Map<String, String> PROMPT_CACHE = buildPromptCache();

    public static String getPrompt(PromptEnum prompt) {
        return PROMPT_CACHE.get(prompt.getCode());
    }

    private static Map<String, String> buildPromptCache() {
        Map<String, String> cache = new ConcurrentHashMap<>();
        for (PromptEnum prompt : PromptEnum.values()) {
            cache.put(prompt.getCode(), loadClassPathResource(prompt.getSystemPromptResource()));
        }
        return cache;
    }

    @SneakyThrows
    public static String loadClassPathResource(String path) {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
