package edu.zsc.ai.plugin.model.metadata;

import edu.zsc.ai.plugin.constant.IndexNameEnum;

import java.util.List;

public record IndexMetadata(
        String name,
        String type,
        List<String> columns,
        boolean unique,
        boolean isPrimaryKey
) {

    public IndexMetadata(String name, String type, List<String> columns, boolean unique) {
        this(name, type, columns, unique, IndexNameEnum.isPrimaryKeyIndex(name));
    }
}
