package edu.zsc.ai.plugin.mysql.value;

import java.sql.Types;

public enum MySQLDataTypeEnum {
    TINYINT(Types.INTEGER),
    SMALLINT(Types.INTEGER),
    MEDIUMINT(Types.INTEGER),
    INT(Types.INTEGER),
    INTEGER(Types.INTEGER),
    BIGINT(Types.BIGINT),
    FLOAT(Types.FLOAT),
    DOUBLE(Types.DOUBLE),
    REAL(Types.DOUBLE),
    DECIMAL(Types.DECIMAL),
    NUMERIC(Types.DECIMAL),
    DATE(Types.DATE),
    TIME(Types.TIME),
    DATETIME(Types.TIMESTAMP),
    TIMESTAMP(Types.TIMESTAMP),
    YEAR(Types.SMALLINT),
    CHAR(Types.CHAR),
    VARCHAR(Types.VARCHAR),
    TEXT(Types.LONGVARCHAR),
    TINYTEXT(Types.LONGVARCHAR),
    MEDIUMTEXT(Types.LONGVARCHAR),
    LONGTEXT(Types.LONGVARCHAR),
    BINARY(Types.VARBINARY),
    VARBINARY(Types.VARBINARY),
    BLOB(Types.LONGVARBINARY),
    TINYBLOB(Types.LONGVARBINARY),
    MEDIUMBLOB(Types.LONGVARBINARY),
    LONGBLOB(Types.LONGVARBINARY),
    JSON(Types.OTHER),
    ENUM(Types.VARCHAR),
    SET(Types.VARCHAR),
    BIT(Types.BIT),
    BOOLEAN(Types.BIT),
    BOOL(Types.BIT);

    private final int sqlType;

    MySQLDataTypeEnum(int sqlType) {
        this.sqlType = sqlType;
    }

    public int getSqlType() {
        return sqlType;
    }

    public static MySQLDataTypeEnum fromTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        try {
            return valueOf(typeName.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int toSqlType(String mysqlDataType) {
        if (mysqlDataType == null) return Types.OTHER;
        MySQLDataTypeEnum type = fromTypeName(mysqlDataType);
        return type == null ? Types.OTHER : type.getSqlType();
    }
}
