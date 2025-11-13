package edu.zsc.ai.plugin.value;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Value processor interface for handling database-specific type conversions.
 * Inspired by Chat2DB's design, this interface separates type handling logic
 * from SQL execution logic.
 *
 * <p>Each database type should provide its own implementation to handle
 * special types like BLOB, CLOB, JSON, UUID, etc.
 *
 * @author Data-Agent Team
 */
public interface ValueProcessor {

    /**
     * Extract and convert value from ResultSet at the specified column.
     *
     * @param resultSet the ResultSet to extract from
     * @param columnIndex the column index (1-based)
     * @param sqlType the SQL type from java.sql.Types
     * @param columnTypeName the database-specific type name (e.g., "JSON", "UUID")
     * @return the extracted and converted value, may be null
     * @throws SQLException if value extraction fails
     */
    Object getJdbcValue(ResultSet resultSet, int columnIndex, int sqlType, String columnTypeName) throws SQLException;
}
