package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;

import java.util.Collections;
import java.util.List;

final class MysqlRowWriteSqlTemplate {

    String buildInsertRowSql(String fullTableName, List<String> quotedColumns) {
        String placeholders = String.join(", ", Collections.nCopies(quotedColumns.size(), "?"));
        return String.format(
                MySqlTemplate.SQL_INSERT_TABLE_ROW,
                fullTableName,
                String.join(", ", quotedColumns),
                placeholders
        );
    }

    String buildDeleteRowSql(String fullTableName, String whereSql) {
        return String.format(MySqlTemplate.SQL_DELETE_TABLE_ROW, fullTableName, whereSql);
    }

    String buildCountMatchingRowsSql(String fullTableName, String whereSql) {
        return String.format(MySqlTemplate.SQL_COUNT_MATCHING_TABLE_ROWS, fullTableName, whereSql);
    }
}
