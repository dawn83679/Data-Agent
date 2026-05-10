package edu.zsc.ai.config.ai;

import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentManager multi-agent support.
 * These are design-level tests that verify the AgentTypeEnum is complete
 * and cache key generation works correctly for multi-agent architecture.
 */
class AgentManagerTest {

    @Test
    void agentTypeEnum_coversAllFourTypes() {
        assertEquals(4, AgentTypeEnum.values().length);
    }

    @Test
    void cacheKey_includesAgentType() {
        // Cache key format: modelName::language::mode::agentType
        String key = buildCacheKey("qwen3-max-2026-01-23", "zh", "agent", AgentTypeEnum.MAIN);
        assertTrue(key.contains("main"));

        String explorerKey = buildCacheKey("qwen3-max-2026-01-23", "zh", "agent", AgentTypeEnum.EXPLORER);
        assertTrue(explorerKey.contains("explorer"));

        // Different agent types should produce different cache keys
        assertNotEquals(key, explorerKey);
    }

    @Test
    void cacheKey_sameParamsProduceSameKey() {
        String key1 = buildCacheKey("qwen3-max-2026-01-23", "zh", "agent", AgentTypeEnum.MAIN);
        String key2 = buildCacheKey("qwen3-max-2026-01-23", "zh", "agent", AgentTypeEnum.MAIN);
        assertEquals(key1, key2);
    }

    @Test
    void cacheKey_differentModelProducesDifferentKey() {
        String key1 = buildCacheKey("qwen3-max-2026-01-23", "zh", "agent", AgentTypeEnum.EXPLORER);
        String key2 = buildCacheKey("qwen3.6-max-preview", "zh", "agent", AgentTypeEnum.EXPLORER);
        assertNotEquals(key1, key2);
    }

    @Test
    void mainAgentDoesNotAppendRuntimeContextSuffixToModelPrompt() throws IOException {
        String agentManagerSource = Files.readString(Path.of(
                "src/main/java/edu/zsc/ai/config/ai/AgentManager.java"));
        String chatSessionFactorySource = Files.readString(Path.of(
                "src/main/java/edu/zsc/ai/domain/service/agent/ChatSessionFactory.java"));

        assertFalse(agentManagerSource.contains("systemMessageTransformer"));
        assertFalse(agentManagerSource.contains("runtimeSystemPromptSuffix"));
        assertFalse(chatSessionFactorySource.contains("runtimeSystemPromptSuffix"));
        assertFalse(chatSessionFactorySource.contains("runtimeContextManager.render"));
        assertFalse(chatSessionFactorySource.contains("memoryContextService.loadPromptContext"));
    }

    /**
     * Replicates the cache key generation that AgentManager will use.
     * This validates the format before we modify AgentManager.
     */
    private String buildCacheKey(String modelName, String language, String mode, AgentTypeEnum agentType) {
        return modelName + "::" + language + "::" + mode + "::" + agentType.getCode();
    }
}
