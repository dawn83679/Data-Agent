package edu.zsc.ai.plugin.value;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Factory for creating JdbcValueContext instances.
 * Separates the creation logic from the data class.
 *
 * <p>This factory provides convenient methods to create JdbcValueContext
 * from various JDBC sources (ResultSet, ResultSetMetaData, etc.).
 *
 * @author hhz
 */
public class JdbcValueContextFactory {

    /**
     * Create a JdbcValueContext from ResultSetMetaData.
     * This is the recommended way to create context with full metadata.
     *
     * @param resultSet the ResultSet
     * @param metaData the ResultSetMetaData
     * @param columnIndex the column index (1-based)
     * @return a new JdbcValueContext with full metadata
     * @throws SQLException if metadata access fails
     */
    public static JdbcValueContext fromMetaData(ResultSet resultSet, ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        return JdbcValueContext.builder()
                .resultSet(resultSet)
                .columnIndex(columnIndex)
                .sqlType(metaData.getColumnType(columnIndex))
                .columnTypeName(metaData.getColumnTypeName(columnIndex))
                .columnLabel(metaData.getColumnLabel(columnIndex))
                .columnName(metaData.getColumnName(columnIndex))
                .precision(metaData.getPrecision(columnIndex))
                .scale(metaData.getScale(columnIndex))
                .nullable(metaData.isNullable(columnIndex) == ResultSetMetaData.columnNullable)
                .build();
    }

    /**
     * Create a minimal JdbcValueContext with only required fields.
     * Useful for simple scenarios where full metadata is not needed.
     *
     * @param resultSet the ResultSet
     * @param columnIndex the column index (1-based)
     * @param sqlType the SQL type from java.sql.Types
     * @param columnTypeName the column type name
     * @return a new JdbcValueContext with minimal metadata
     */
    public static JdbcValueContext create(ResultSet resultSet, int columnIndex, int sqlType, String columnTypeName) {
        return JdbcValueContext.builder()
                .resultSet(resultSet)
                .columnIndex(columnIndex)
                .sqlType(sqlType)
                .columnTypeName(columnTypeName)
                .build();
    }

    /**
     * Create a JdbcValueContext with custom metadata.
     * Allows fine-grained control over all fields.
     *
     * @return a builder for custom configuration
     */
    public static JdbcValueContext.JdbcValueContextBuilder builder() {
        return JdbcValueContext.builder();
    }
}
