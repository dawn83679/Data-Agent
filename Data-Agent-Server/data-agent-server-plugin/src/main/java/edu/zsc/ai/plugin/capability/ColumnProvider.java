package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capability for listing columns of a table or view.
 * Plugins that implement this can provide column metadata for schema browsing, SQL editing, etc.
 */
public interface ColumnProvider {

    /**
     * Get column metadata for the specified table or view.
     *
     * @param connection      the active connection
     * @param catalog         catalog/database name; may be null
     * @param schema          schema name; may be null
     * @param tableOrViewName table or view name
     * @return list of column metadata, ordered by ordinal position; never null
     * @throws RuntimeException if listing fails
     */
    default List<ColumnMetadata> getColumns(Connection connection, String catalog, String schema, String tableOrViewName) {
        try {
            List<ColumnMetadata> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getColumns(catalog, schema, tableOrViewName, null)) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.COLUMN_NAME);
                    int dataType = rs.getInt(JdbcMetaDataConstants.DATA_TYPE);
                    String typeName = rs.getString(JdbcMetaDataConstants.TYPE_NAME);
                    int columnSize = rs.getInt(JdbcMetaDataConstants.COLUMN_SIZE);
                    int decimalDigits = rs.getInt(JdbcMetaDataConstants.DECIMAL_DIGITS);
                    int nullable = rs.getInt(JdbcMetaDataConstants.NULLABLE);
                    int ordinalPosition = rs.getInt(JdbcMetaDataConstants.ORDINAL_POSITION);
                    String remarks = rs.getString(JdbcMetaDataConstants.REMARKS);
                    if (remarks == null) {
                        remarks = "";
                    }
                    list.add(new ColumnMetadata(
                            name,
                            dataType,
                            typeName != null ? typeName : "",
                            columnSize,
                            decimalDigits,
                            nullable == ResultSetMetaData.columnNullable,
                            ordinalPosition,
                            remarks
                    ));
                }
            }
            list.sort((a, b) -> Integer.compare(a.ordinalPosition(), b.ordinalPosition()));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list columns for " + tableOrViewName + ": " + e.getMessage(), e);
        }
    }
}
