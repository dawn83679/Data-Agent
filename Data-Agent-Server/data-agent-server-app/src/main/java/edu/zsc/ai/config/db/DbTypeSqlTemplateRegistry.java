package edu.zsc.ai.config.db;

import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DbTypeSqlTemplateRegistry {

    static final String RESOURCE_PATH = "db-type-sql-templates.json";
    static final String QUALIFIED_NAME_PLACEHOLDER = "{{qualifiedName}}";
    private static final Set<String> SUPPORTED_QUOTE_STYLES = Set.of("backtick", "double_quote", "none");

    private final DbTypeSqlTemplateConfig defaults;
    private final Map<String, DbTypeSqlTemplateConfig> configsByDbType;

    public DbTypeSqlTemplateRegistry() {
        DbTypeSqlTemplateFile file = loadTemplateFile();
        this.defaults = validate("defaults", DbTypeSqlTemplateConfig.defaultConfig().merge(file.getDefaults()));
        Map<String, DbTypeSqlTemplateConfig> dbTypes = file.getDbTypes() == null ? Collections.emptyMap() : file.getDbTypes();
        this.configsByDbType = dbTypes.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> validate(entry.getKey(), defaults.merge(entry.getValue()))
                ));
        log.info("Loaded db type SQL templates: {}", configsByDbType.keySet());
    }

    public DbTypeSqlTemplateConfig resolve(String dbTypeCode) {
        if (StringUtils.isBlank(dbTypeCode)) {
            return defaults;
        }
        return configsByDbType.getOrDefault(dbTypeCode.toLowerCase(), defaults);
    }

    private DbTypeSqlTemplateFile loadTemplateFile() {
        try {
            String content = new ClassPathResource(RESOURCE_PATH).getContentAsString(StandardCharsets.UTF_8);
            return JsonUtil.json2Object(content, DbTypeSqlTemplateFile.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load db type SQL templates from classpath: " + RESOURCE_PATH, e);
        }
    }

    private DbTypeSqlTemplateConfig validate(String key, DbTypeSqlTemplateConfig config) {
        if (config == null) {
            throw new IllegalStateException("Missing SQL template config for dbType: " + key);
        }
        if (StringUtils.isBlank(config.getTableDoubleClickSelectTemplate())) {
            throw new IllegalStateException("tableDoubleClickSelectTemplate cannot be blank for dbType: " + key);
        }
        if (!config.getTableDoubleClickSelectTemplate().contains(QUALIFIED_NAME_PLACEHOLDER)) {
            throw new IllegalStateException("tableDoubleClickSelectTemplate must contain "
                    + QUALIFIED_NAME_PLACEHOLDER + " for dbType: " + key);
        }
        if (!SUPPORTED_QUOTE_STYLES.contains(config.getIdentifierQuoteStyle())) {
            throw new IllegalStateException("Unsupported identifierQuoteStyle '" + config.getIdentifierQuoteStyle()
                    + "' for dbType: " + key);
        }
        return config;
    }
}
