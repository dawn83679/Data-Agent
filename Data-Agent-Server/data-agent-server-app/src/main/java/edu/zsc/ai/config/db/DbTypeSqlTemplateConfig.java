package edu.zsc.ai.config.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbTypeSqlTemplateConfig {

    private String tableDoubleClickSelectTemplate;

    private String identifierQuoteStyle;

    public static DbTypeSqlTemplateConfig defaultConfig() {
        return new DbTypeSqlTemplateConfig(
                "SELECT * FROM {{qualifiedName}};",
                "double_quote"
        );
    }

    public DbTypeSqlTemplateConfig merge(DbTypeSqlTemplateConfig override) {
        if (override == null) {
            return new DbTypeSqlTemplateConfig(tableDoubleClickSelectTemplate, identifierQuoteStyle);
        }
        return new DbTypeSqlTemplateConfig(
                StringUtils.defaultIfBlank(override.getTableDoubleClickSelectTemplate(), tableDoubleClickSelectTemplate),
                StringUtils.defaultIfBlank(override.getIdentifierQuoteStyle(), identifierQuoteStyle)
        );
    }
}
