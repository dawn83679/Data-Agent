package edu.zsc.ai.plugin.constant;

import java.sql.DatabaseMetaData;

/**
 * JDBC index type from {@link DatabaseMetaData#getIndexInfo}.
 */
public enum IndexTypeEnum {
    CLUSTERED,
    HASHED,
    OTHER;

    public static String fromJdbcType(short type) {
        IndexTypeEnum e = switch (type) {
            case DatabaseMetaData.tableIndexClustered -> CLUSTERED;
            case DatabaseMetaData.tableIndexHashed -> HASHED;
            default -> OTHER;
        };
        return e.name();
    }
}
