package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ColumnProvider {

    default List<ColumnMetadata> getColumns(Connection connection, String catalog, String schema, String tableOrViewName) {
        try {
            Set<String> primaryKeyColumns = new HashSet<>();
            try (ResultSet pkRs = connection.getMetaData().getPrimaryKeys(catalog, schema, tableOrViewName)) {
                while (pkRs.next()) {
                    String col = pkRs.getString(JdbcMetaDataConstants.COLUMN_NAME);
                    if (col != null) {
                        primaryKeyColumns.add(col);
                    }
                }
            }

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
                    String columnDef = rs.getString(JdbcMetaDataConstants.COLUMN_DEF);
                    if (remarks == null) {
                        remarks = "";
                    }
                    boolean isPk = primaryKeyColumns.contains(name);
                    list.add(new ColumnMetadata(
                            name,
                            dataType,
                            typeName != null ? typeName : "",
                            columnSize,
                            decimalDigits,
                            nullable == ResultSetMetaData.columnNullable,
                            ordinalPosition,
                            remarks,
                            isPk,
                            false,
                            false,
                            columnDef
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
