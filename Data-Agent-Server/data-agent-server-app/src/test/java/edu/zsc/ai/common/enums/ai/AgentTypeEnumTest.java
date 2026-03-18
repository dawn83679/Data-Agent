package edu.zsc.ai.common.enums.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentTypeEnumTest {

    @Test
    void allThreeTypesExist() {
        assertEquals(3, AgentTypeEnum.values().length);
        assertNotNull(AgentTypeEnum.MAIN);
        assertNotNull(AgentTypeEnum.EXPLORER);
        assertNotNull(AgentTypeEnum.PLANNER);
    }

    @Test
    void codesAreCorrect() {
        assertEquals("main", AgentTypeEnum.MAIN.getCode());
        assertEquals("explorer", AgentTypeEnum.EXPLORER.getCode());
        assertEquals("planner", AgentTypeEnum.PLANNER.getCode());
    }
}
