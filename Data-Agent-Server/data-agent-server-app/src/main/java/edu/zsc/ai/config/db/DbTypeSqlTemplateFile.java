package edu.zsc.ai.config.db;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DbTypeSqlTemplateFile {

    private DbTypeSqlTemplateConfig defaults = new DbTypeSqlTemplateConfig();

    private Map<String, DbTypeSqlTemplateConfig> dbTypes = new HashMap<>();
}
