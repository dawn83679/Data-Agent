package edu.zsc.ai.agent.tool.skill;

import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SkillEnum — verifies all skills are registered and loadable.
 */
class SkillEnumTest {

    @Test
    void chartSkill_exists() {
        SkillEnum skill = SkillEnum.fromName("chart");
        assertNotNull(skill);
        assertEquals("chart", skill.getSkillName());
    }

    @Test
    void sqlOptimizationSkill_exists() {
        SkillEnum skill = SkillEnum.fromName("sql-optimization");
        assertNotNull(skill);
        assertEquals("sql-optimization", skill.getSkillName());
        assertEquals("skills/sql-optimization.md", skill.getResourcePath());
    }

    @Test
    void sqlOptimizationSkill_resourceLoadable() {
        String content = PromptConfig.loadClassPathResource("skills/sql-optimization.md");
        assertNotNull(content);
        assertFalse(content.isBlank());
        assertTrue(content.contains("Index Utilization"), "Should contain index optimization rules");
        assertTrue(content.contains("JOIN Optimization"), "Should contain JOIN optimization rules");
        assertTrue(content.contains("optimizedSql"), "Should describe output format");
    }

    @Test
    void memorySkill_exists() {
        SkillEnum skill = SkillEnum.fromName("memory");
        assertNotNull(skill);
        assertEquals("memory", skill.getSkillName());
        assertEquals("skills/memory.md", skill.getResourcePath());
        assertFalse(skill.getDescription().isBlank());
    }

    @Test
    void memorySkill_resourceLoadable() {
        String content = PromptConfig.loadClassPathResource("skills/memory.md");
        assertNotNull(content);
        assertFalse(content.isBlank());
        assertTrue(content.contains("readMemory"));
        assertTrue(content.contains("writeMemory"));
        assertTrue(content.contains("PREFERENCE"));
        assertTrue(content.contains("WORKSPACE"));
    }

    @Test
    void validNames_includesBothSkills() {
        String names = SkillEnum.validNames();
        assertTrue(names.contains("chart"));
        assertTrue(names.contains("sql-optimization"));
        assertTrue(names.contains("memory"));
    }

    @Test
    void unknownSkill_returnsNull() {
        assertNull(SkillEnum.fromName("nonexistent"));
        assertNull(SkillEnum.fromName(null));
    }

    @Test
    void fromName_caseInsensitive() {
        assertNotNull(SkillEnum.fromName("SQL-OPTIMIZATION"));
    }
}
