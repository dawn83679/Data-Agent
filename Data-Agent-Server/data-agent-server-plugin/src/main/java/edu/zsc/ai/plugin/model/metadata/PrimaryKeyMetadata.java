package edu.zsc.ai.plugin.model.metadata;

import java.util.List;

public record PrimaryKeyMetadata(
        String name,
        List<String> columnNames
) {
}
