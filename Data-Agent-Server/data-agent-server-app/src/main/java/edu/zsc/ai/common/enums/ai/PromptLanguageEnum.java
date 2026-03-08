package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

import java.util.Locale;

/**
 * Prompt language configuration: request language code + system prompt resource path.
 */
@Getter
public enum PromptLanguageEnum {

    EN("en", "prompt/system_agent_en.xml"),
    ZH("zh", "prompt/system_agent_zh.xml");

    private final String code;
    private final String systemPromptResource;

    PromptLanguageEnum(String code, String systemPromptResource) {
        this.code = code;
        this.systemPromptResource = systemPromptResource;
    }

    /**
     * Resolve request language to prompt language.
     * Unknown/blank values fallback to EN by design.
     */
    public static PromptLanguageEnum fromRequestLanguage(String language) {
        if (language == null || language.isBlank()) {
            return EN;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(ZH.code)) {
            return ZH;
        }
        if (normalized.startsWith(EN.code)) {
            return EN;
        }
        return EN;
    }
}
