package edu.zsc.ai.config.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbTypeSqlTemplateRegistryTest {

    private final DbTypeSqlTemplateRegistry registry = new DbTypeSqlTemplateRegistry();

    @Test
    void shouldLoadMysqlTemplateOverridesFromClasspathFile() {
        DbTypeSqlTemplateConfig config = registry.resolve("mysql");

        assertEquals("SELECT * FROM {{qualifiedName}};", config.getTableDoubleClickSelectTemplate());
        assertEquals("backtick", config.getIdentifierQuoteStyle());
    }

    @Test
    void shouldFallbackToDefaultTemplateForUnknownDbType() {
        DbTypeSqlTemplateConfig config = registry.resolve("postgresql");

        assertEquals("SELECT * FROM {{qualifiedName}};", config.getTableDoubleClickSelectTemplate());
        assertEquals("double_quote", config.getIdentifierQuoteStyle());
    }
}
