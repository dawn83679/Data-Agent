package edu.zsc.ai.plugin.mysql.executor;

import edu.zsc.ai.plugin.model.command.sql.AbstractSqlExecutor;
import java.math.BigDecimal;
import java.sql.*;

/**
 * MySQL-specific SQL executor that handles MySQL data type conversions
 * properly.
 * Handles special types like BLOB, CLOB, DATE, TIME, TIMESTAMP, JSON, etc.
 */
public class MySQLSqlExecutor extends AbstractSqlExecutor {

    @Override
    protected Object extractValue(ResultSet resultSet, int columnIndex, int sqlType) throws SQLException {
        // Handle NULL values first
        Object value = resultSet.getObject(columnIndex);
        if (value == null) {
            return null;
        }

        // Handle different SQL types appropriately
        switch (sqlType) {
            // String types
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                return resultSet.getString(columnIndex);

            // Numeric types
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return resultSet.getInt(columnIndex);

            case Types.BIGINT:
                return resultSet.getLong(columnIndex);

            case Types.FLOAT:
            case Types.REAL:
                return resultSet.getFloat(columnIndex);

            case Types.DOUBLE:
                return resultSet.getDouble(columnIndex);

            case Types.DECIMAL:
            case Types.NUMERIC:
                BigDecimal decimal = resultSet.getBigDecimal(columnIndex);
                return decimal != null ? decimal.toString() : null;

            // Boolean type
            case Types.BOOLEAN:
            case Types.BIT:
                return resultSet.getBoolean(columnIndex);

            // Date and Time types
            case Types.DATE:
                Date date = resultSet.getDate(columnIndex);
                return date != null ? date.toLocalDate().toString() : null;

            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                Time time = resultSet.getTime(columnIndex);
                return time != null ? time.toLocalTime().toString() : null;

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                Timestamp timestamp = resultSet.getTimestamp(columnIndex);
                return timestamp != null ? timestamp.toLocalDateTime().toString() : null;

            // Binary types
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                byte[] bytes = resultSet.getBytes(columnIndex);
                return bytes != null ? bytesToHex(bytes) : null;

            // BLOB types
            case Types.BLOB:
                Blob blob = resultSet.getBlob(columnIndex);
                if (blob != null) {
                    try {
                        byte[] blobBytes = blob.getBytes(1, (int) blob.length());
                        return bytesToHex(blobBytes);
                    } finally {
                        blob.free();
                    }
                }
                return null;

            // CLOB types
            case Types.CLOB:
            case Types.NCLOB:
                Clob clob = resultSet.getClob(columnIndex);
                if (clob != null) {
                    try {
                        return clob.getSubString(1, (int) clob.length());
                    } finally {
                        clob.free();
                    }
                }
                return null;

            // Array type
            case Types.ARRAY:
                Array array = resultSet.getArray(columnIndex);
                if (array != null) {
                    try {
                        Object[] arrayData = (Object[]) array.getArray();
                        return arrayData;
                    } finally {
                        array.free();
                    }
                }
                return null;

            // JSON type (MySQL specific, mapped to VARCHAR or LONGVARCHAR in JDBC)
            // Note: MySQL 5.7+ has native JSON type
            case Types.OTHER:
                // Check if it's a JSON column by column type name
                String columnTypeName = resultSet.getMetaData().getColumnTypeName(columnIndex);
                if ("JSON".equalsIgnoreCase(columnTypeName)) {
                    return resultSet.getString(columnIndex);
                }
                // Fall through to default

                // Default: use getObject
            default:
                return resultSet.getObject(columnIndex);
        }
    }

    /**
     * Convert byte array to hexadecimal string representation.
     * Used for BINARY and BLOB types.
     *
     * @param bytes byte array
     * @return hex string (e.g., "0x1A2B3C")
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "0x";
        }

        StringBuilder hexString = new StringBuilder("0x");
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


}
