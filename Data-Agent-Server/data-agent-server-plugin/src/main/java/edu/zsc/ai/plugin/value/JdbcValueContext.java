package edu.zsc.ai.plugin.value;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * JDBC-specific implementation of ValueContext.
 * Encapsulates all parameters needed for JDBC value processing.
 *
 * <p>This class is specific to JDBC data sources and should not be used
 * for other data source types (MongoDB, Redis, etc.).
 *
 * @author hhz
 */
@Data
@Builder
public class JdbcValueContext implements ValueContext {

    /**
     * The ResultSet to extract value from
     */
    private ResultSet resultSet;

    /**
     * The column index (1-based)
     */
    private int columnIndex;

    /**
     * The SQL type from java.sql.Types
     */
    private int sqlType;

    /**
     * The database-specific type name (e.g., "JSON", "UUID", "GEOMETRY")
     */
    private String columnTypeName;

    /**
     * Optional: Column label/alias
     */
    private String columnLabel;

    /**
     * Optional: Column name
     */
    private String columnName;

    /**
     * Optional: Precision (for numeric types)
     */
    private Integer precision;

    /**
     * Optional: Scale (for decimal types)
     */
    private Integer scale;

    /**
     * Optional: Whether the column is nullable
     */
    private Boolean nullable;

    /**
     * Additional metadata storage
     */
    @Builder.Default
    private Map<String, Object> additionalMetadata = new HashMap<>();

    // ==================== ValueContext Interface Implementation ====================

    @Override
    public String getDataSourceType() {
        return "JDBC";
    }

    @Override
    public int getIndex() {
        return columnIndex;
    }

    @Override
    public String getName() {
        return columnName != null ? columnName : columnLabel;
    }

    @Override
    public String getTypeName() {
        return columnTypeName;
    }

    @Override
    public Object getMetadata(String key) {
        return additionalMetadata.get(key);
    }

    @Override
    public boolean isNullable() {
        return nullable != null ? nullable : true;
    }

    /**
     * Add additional metadata.
     *
     * @param key the metadata key
     * @param value the metadata value
     */
    public void addMetadata(String key, Object value) {
        additionalMetadata.put(key, value);
    }

}
