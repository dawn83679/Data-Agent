package edu.zsc.ai.plugin.mysql.value;

import edu.zsc.ai.plugin.value.ValueProcessor;

import java.math.BigDecimal;
import java.sql.*;

/**
 * MySQL 特定的值处理器，处理 MySQL 数据类型转换。
 * 处理特殊类型如 BLOB、CLOB、DATE、TIME、TIMESTAMP、JSON 等。
 *
 * @author Data-Agent Team
 */
public class MySQLValueProcessor implements ValueProcessor {

    @Override
    public Object getJdbcValue(ResultSet resultSet, int columnIndex, int sqlType, String columnTypeName)
            throws SQLException {
        // 根据不同的 SQL 类型使用对应的方法处理
        switch (sqlType) {
            // 字符串类型
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                String strValue = resultSet.getString(columnIndex);
                return resultSet.wasNull() ? null : strValue;

            // 数值类型
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                int intValue = resultSet.getInt(columnIndex);
                return resultSet.wasNull() ? null : intValue;

            case Types.BIGINT:
                long longValue = resultSet.getLong(columnIndex);
                return resultSet.wasNull() ? null : longValue;

            case Types.FLOAT:
            case Types.REAL:
                float floatValue = resultSet.getFloat(columnIndex);
                return resultSet.wasNull() ? null : floatValue;

            case Types.DOUBLE:
                double doubleValue = resultSet.getDouble(columnIndex);
                return resultSet.wasNull() ? null : doubleValue;

            case Types.DECIMAL:
            case Types.NUMERIC:
                BigDecimal decimal = resultSet.getBigDecimal(columnIndex);
                return decimal != null ? decimal.toString() : null;

            // 布尔类型
            case Types.BOOLEAN:
            case Types.BIT:
                boolean boolValue = resultSet.getBoolean(columnIndex);
                return resultSet.wasNull() ? null : boolValue;

            // 日期和时间类型
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

            // 二进制类型
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                byte[] bytes = resultSet.getBytes(columnIndex);
                return bytes != null ? bytesToHex(bytes) : null;

            // BLOB 类型
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

            // CLOB 类型
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

            // 数组类型
            case Types.ARRAY:
                Array array = resultSet.getArray(columnIndex);
                if (array != null) {
                    try {
                        return array.getArray();
                    } finally {
                        array.free();
                    }
                }
                return null;

            // JSON 类型（MySQL 特定，在 JDBC 中映射为 OTHER）
            case Types.OTHER:
                // 通过类型名称检查是否为 JSON 列
                if ("JSON".equalsIgnoreCase(columnTypeName)) {
                    return resultSet.getString(columnIndex);
                }
                // 继续执行默认处理

            // 默认：使用 getObject
            default:
                return resultSet.getObject(columnIndex);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串表示。
     * 用于 BINARY 和 BLOB 类型。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串（例如："0x1A2B3C"）
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
