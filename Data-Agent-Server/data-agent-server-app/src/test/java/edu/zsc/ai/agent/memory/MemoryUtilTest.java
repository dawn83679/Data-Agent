package edu.zsc.ai.agent.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.constant.UserPromptTagConstant;

class MemoryUtilTest {

    @Test
    void stripInjectedWrapper_supportsModernPromptTemplateWithoutRuntimeWrapper() {
        String wrapped = """
                %s
                today: 2026-03-18
                timezone: Asia/Shanghai
                %s

                %s
                show me the current memory design
                %s
                """.formatted(
                UserPromptTagConstant.SYSTEM_CONTEXT_OPEN,
                UserPromptTagConstant.SYSTEM_CONTEXT_CLOSE,
                UserPromptTagConstant.USER_QUESTION_OPEN,
                UserPromptTagConstant.USER_QUESTION_CLOSE);

        assertEquals("show me the current memory design", MemoryUtil.stripInjectedWrapper(wrapped));
    }

    @Test
    void stripInjectedWrapper_returnsOriginalTextWhenNoNewWrapperExists() {
        String wrapped = """
                <memory_context>
                - [M1][PREFERENCE][0.9000] prefer concise output
                </memory_context>

                explain the current flow
                """;

        assertEquals(wrapped, MemoryUtil.stripInjectedWrapper(wrapped));
    }
}
