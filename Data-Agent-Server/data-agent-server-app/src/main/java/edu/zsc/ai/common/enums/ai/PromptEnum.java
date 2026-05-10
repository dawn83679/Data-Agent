package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

/**
 * Prompt language configuration: request language code + system prompt resource path.
 */
@Getter
public enum PromptEnum {

    ZH("zh", "prompt/main-agent_zh.md"),
    ZH_PLAN("zh-plan", "prompt/main-agent-plan_zh.md"),
    COMPRESSION("compression", "prompt/compression.md"),
    MEMORY_WRITER("memory-writer", "prompt/memory-writer.md"),
    EXPLORER("explorer", "prompt/explorer.md"),
    PLANNER("planner", "prompt/planner.md");

    private final String code;
    private final String systemPromptResource;

    PromptEnum(String code, String systemPromptResource) {
        this.code = code;
        this.systemPromptResource = systemPromptResource;
    }

    /**
     * Resolve request language to the only supported main-agent prompt language.
     */
    public static PromptEnum fromRequestLanguage(String language) {
        return ZH;
    }
}
