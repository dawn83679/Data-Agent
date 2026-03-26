package edu.zsc.ai.agent.tool.skill;

import edu.zsc.ai.common.enums.ai.SkillEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for SkillEnum — verifies the remaining skill registration. */
class SkillEnumTest {

    @Test
    void chartSkill_exists() {
        SkillEnum skill = SkillEnum.fromName("chart");
        assertNotNull(skill);
        assertEquals("chart", skill.getSkillName());
    }

    @Test
    void validNames_includesRegisteredSkills() {
        String names = SkillEnum.validNames();
        assertTrue(names.contains("chart"));
        assertFalse(names.contains("file-export"));
        assertFalse(names.contains("sql-optimization"));
        assertFalse(names.contains("memory"));
    }

    @Test
    void unknownSkill_returnsNull() {
        assertNull(SkillEnum.fromName("nonexistent"));
        assertNull(SkillEnum.fromName(null));
    }

    @Test
    void fromName_caseInsensitive() {
        assertNotNull(SkillEnum.fromName("CHART"));
    }
}
