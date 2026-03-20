package edu.zsc.ai.common.enums.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MemoryEnumTest {

    @Test
    void memoryType_parsesCodeCaseInsensitively() {
        assertEquals(MemoryTypeEnum.PREFERENCE, MemoryTypeEnum.fromCode("preference"));
    }

    @Test
    void memorySubType_knowsItsParentType() {
        assertEquals(MemoryTypeEnum.WORKFLOW_CONSTRAINT, MemorySubTypeEnum.IMPLEMENTATION_CONSTRAINT.getMemoryType());
        assertTrue(MemorySubTypeEnum.IMPLEMENTATION_CONSTRAINT.belongsTo(MemoryTypeEnum.WORKFLOW_CONSTRAINT));
        assertFalse(MemorySubTypeEnum.IMPLEMENTATION_CONSTRAINT.belongsTo(MemoryTypeEnum.PREFERENCE));
        assertEquals(MemoryTypeEnum.PREFERENCE, MemorySubTypeEnum.LANGUAGE_PREFERENCE.getMemoryType());
    }

    @Test
    void sourceType_returnNullForUnknownCodes() {
        assertNull(MemorySourceTypeEnum.fromCode("bad"));
    }
}
