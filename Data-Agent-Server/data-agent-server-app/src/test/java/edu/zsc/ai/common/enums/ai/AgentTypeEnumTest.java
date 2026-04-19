package edu.zsc.ai.common.enums.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentTypeEnumTest {

    @Test
    void allFourTypesExist() {
        assertEquals(4, AgentTypeEnum.values().length);
        assertNotNull(AgentTypeEnum.MAIN);
        assertNotNull(AgentTypeEnum.EXPLORER);
        assertNotNull(AgentTypeEnum.PLANNER);
        assertNotNull(AgentTypeEnum.MEMORY_WRITER);
    }

    @Test
    void codesAreCorrect() {
        assertEquals("main", AgentTypeEnum.MAIN.getCode());
        assertEquals("explorer", AgentTypeEnum.EXPLORER.getCode());
        assertEquals("planner", AgentTypeEnum.PLANNER.getCode());
        assertEquals("memory-writer", AgentTypeEnum.MEMORY_WRITER.getCode());
    }
}
