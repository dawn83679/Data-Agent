package edu.zsc.ai.domain.service.agent;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Resolves the system prompt by request language.
 * Unknown/empty languages fall back to English prompt.
 */
@Service
@Slf4j
public class AgentSystemPromptService {

    private static final String ZH_PROMPT_PATH = "prompt/system_zh.xml";
    private static final String EN_PROMPT_PATH = "prompt/system_en.xml";

    private String zhPrompt;
    private String enPrompt;

    @PostConstruct
    public void loadPrompts() {
        this.zhPrompt = readPrompt(ZH_PROMPT_PATH);
        this.enPrompt = readPrompt(EN_PROMPT_PATH);
        log.info("Loaded system prompts: zh={}, en={}", ZH_PROMPT_PATH, EN_PROMPT_PATH);
    }

    public String resolvePrompt(String acceptLanguage) {
        if (StringUtils.isBlank(acceptLanguage)) {
            return enPrompt;
        }

        String[] languageRanges = acceptLanguage.split(",");
        for (String range : languageRanges) {
            String tag = StringUtils.substringBefore(range.trim(), ";");
            if (StringUtils.isBlank(tag)) {
                continue;
            }

            String normalized = tag.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("zh")) {
                return zhPrompt;
            }
            if (normalized.startsWith("en")) {
                return enPrompt;
            }
        }
        return enPrompt;
    }

    private String readPrompt(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try {
            if (!resource.exists()) {
                throw new IllegalStateException("Prompt resource does not exist: " + path);
            }

            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isBlank(content)) {
                throw new IllegalStateException("Prompt resource is empty: " + path);
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt resource: " + path, e);
        }
    }
}
