package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;

/**
 * Common MySQL type processors.
 * Groups simple processors together to reduce file count.
 *
 * @author hhz
 * @date 2025-11-14
 */
public class MySQLCommonProcessors {

    // ==================== Numeric Types ====================

    public static class MySQLTinyIntProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            int value = resultSet.getInt(columnIndex);
            return resultSet.wasNull() ? null : value;
        }
    }

    public static class MySQLSmallIntProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            int value = resultSet.getInt(columnIndex);
            return resultSet.wasNull() ? null : value;
        }
    }

    public static class MySQLBigIntProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            long value = resultSet.getLong(columnIndex);
            if (resultSet.wasNull()) {
                return null;
            }
            
            // Handle UNSIGNED BIGINT
            if (MySQLValueProcessorFactory.isUnsigned(columnTypeName) && value < 0) {
                // For unsigned bigint, we need to use BigInteger
                return Long.toUnsignedString(value);
            }
            
            return value;
        }
    }

    public static class MySQLFloatProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            float value = resultSet.getFloat(columnIndex);
            return resultSet.wasNull() ? null : value;
        }
    }

    public static class MySQLDoubleProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            double value = resultSet.getDouble(columnIndex);
            return resultSet.wasNull() ? null : value;
        }
    }

    public static class MySQLDecimalProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            java.math.BigDecimal decimal = resultSet.getBigDecimal(columnIndex);
            return decimal != null ? decimal.toString() : null;
        }
    }

    // ==================== Date/Time Types ====================

    public static class MySQLTimeProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            java.sql.Time time = resultSet.getTime(columnIndex);
            return time != null ? time.toLocalTime().toString() : null;
        }
    }

    public static class MySQLDateTimeProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            try {
                java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
                if (timestamp != null) {
                    return timestamp.toLocalDateTime().toString();
                }
            } catch (SQLException e) {
                // MySQL may have invalid datetime like "0000-00-00 00:00:00"
                String stringValue = resultSet.getString(columnIndex);
                if (stringValue != null && !stringValue.isEmpty()) {
                    return stringValue;
                }
            }
            return null;
        }
    }

    public static class MySQLTimestampProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
            return timestamp != null ? timestamp.toLocalDateTime().toString() : null;
        }
    }

    public static class MySQLYearProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            int value = resultSet.getInt(columnIndex);
            return resultSet.wasNull() ? null : value;
        }
    }

    // ==================== String Types ====================

    public static class MySQLStringProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            return resultSet.getString(columnIndex);
        }
    }

    public static class MySQLTextProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            return resultSet.getString(columnIndex);
        }
    }

    // ==================== Binary Types ====================

    public static class MySQLBinaryProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            byte[] bytes = resultSet.getBytes(columnIndex);
            return bytes != null ? bytesToHex(bytes) : null;
        }
        
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

    public static class MySQLBlobProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            java.sql.Blob blob = resultSet.getBlob(columnIndex);
            if (blob != null) {
                try {
                    byte[] blobBytes = blob.getBytes(1, (int) blob.length());
                    return bytesToHex(blobBytes);
                } finally {
                    blob.free();
                }
            }
            return null;
        }
        
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

    // ==================== Special Types ====================

    public static class MySQLEnumProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            return resultSet.getString(columnIndex);
        }
    }

    public static class MySQLSetProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            return resultSet.getString(columnIndex);
        }
    }

    public static class MySQLBitProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            // MySQL BIT(1) is often used as boolean
            boolean value = resultSet.getBoolean(columnIndex);
            return resultSet.wasNull() ? null : value;
        }
    }

    public static class MySQLBooleanProcessor extends DefaultValueProcessor {
        @Override
        public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
                throws SQLException {
            boolean value = resultSet.getBoolean(columnIndex);
            return resultSet.wasNull() ? null : value;
        }
    }
}
