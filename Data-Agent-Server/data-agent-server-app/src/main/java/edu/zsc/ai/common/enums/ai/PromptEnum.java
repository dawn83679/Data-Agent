package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

import java.util.Locale;

/**
 * Prompt language configuration: request language code + system prompt resource path.
 */
@Getter
public enum PromptEnum {

    EN("en", "prompt/main-agent_en.md"),
    ZH("zh", "prompt/main-agent_zh.md"),
    EN_PLAN("en-plan", "prompt/main-agent-plan_en.md"),
    ZH_PLAN("zh-plan", "prompt/main-agent-plan_zh.md"),
    COMPRESSION("compression", "prompt/compression.md"),
    MEMORY_AUTO_WRITE("memory-auto-write", "prompt/memory-auto-write.md"),
    EXPLORER("explorer", "prompt/explorer.md"),
    PLANNER("planner", "prompt/planner.md");

    private final String code;
    private final String systemPromptResource;

    PromptEnum(String code, String systemPromptResource) {
        this.code = code;
        this.systemPromptResource = systemPromptResource;
    }

    /**
     * Resolve request language to prompt language.
     * Unknown/blank values fallback to EN by design.
     */
    public static PromptEnum fromRequestLanguage(String language) {
        if (language == null || language.isBlank()) {
            return EN;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(ZH.code)) {
            return ZH;
        }
        return EN;
    }
}
