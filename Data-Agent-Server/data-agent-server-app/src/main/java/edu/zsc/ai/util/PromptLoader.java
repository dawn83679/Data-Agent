package edu.zsc.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Prompt loading utility class
 * Used for loading system prompt templates
 *
 * @author zgq
 */
@Slf4j
public class PromptLoader {

    private static final String COMPRESS_SYSTEM_PROMPT;
    private static final String SYSTEM_PROMPT;

    static {
        COMPRESS_SYSTEM_PROMPT = loadPromptFromResource("prompt/Compress_system.md");
        SYSTEM_PROMPT = loadPromptFromResource("prompt/system.md");
    }

    /**
     * Get compressed system prompt
     *
     * @return compressed system prompt content
     */
    public static String getCompressSystemPrompt() {
        return COMPRESS_SYSTEM_PROMPT;
    }

    /**
     * Get system prompt
     *
     * @return system prompt content
     */
    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * Load prompt from resource file
     *
     * @param resourcePath resource file path
     * @return prompt content
     */
    private static String loadPromptFromResource(String resourcePath) {
        try (InputStream inputStream = PromptLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assert inputStream != null;
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}