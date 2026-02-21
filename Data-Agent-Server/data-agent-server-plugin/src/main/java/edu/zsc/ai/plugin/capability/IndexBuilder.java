package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.model.metadata.IndexMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builder for aggregating index columns from JDBC getIndexInfo result set.
 */
public final class IndexBuilder {
    private final String name;
    private final String type;
    private final boolean unique;
    private final Map<Integer, String> columnsByOrdinal = new TreeMap<>();

    public IndexBuilder(String name, String type, boolean unique) {
        this.name = name;
        this.type = type;
        this.unique = unique;
    }

    public void addColumn(int ordinal, String column) {
        columnsByOrdinal.put(ordinal, column);
    }

    public IndexMetadata build() {
        List<String> columns = new ArrayList<>(columnsByOrdinal.values());
        return new IndexMetadata(name, type, columns, unique);
    }
}
