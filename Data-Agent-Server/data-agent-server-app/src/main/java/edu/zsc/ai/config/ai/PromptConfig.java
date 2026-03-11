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

    /**
     * Compose multiple classpath resources into a single prompt string.
     */
    public static String composePrompt(String... resourcePaths) {
        StringBuilder sb = new StringBuilder();
        for (String path : resourcePaths) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(loadClassPathResource(path));
        }
        return sb.toString();
    }

    @SneakyThrows
    public static String loadClassPathResource(String path) {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
