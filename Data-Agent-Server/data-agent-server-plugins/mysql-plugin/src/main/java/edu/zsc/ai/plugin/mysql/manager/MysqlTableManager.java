package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.TableManager;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.db.TableRowValue;
import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;
import edu.zsc.ai.plugin.mysql.support.MysqlCapabilitySupport;
import edu.zsc.ai.plugin.mysql.support.MysqlRowWriteSupport;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

public final class MysqlTableManager implements TableManager {

    private final MysqlCapabilitySupport support;
    private final MysqlRowWriteSupport rowWriteSupport = new MysqlRowWriteSupport();

    public MysqlTableManager(MysqlCapabilitySupport support) {
        this.support = Objects.requireNonNull(support, "support");
    }

    @Override
    public long countTables(Connection connection, String catalog, String schema, String tableNamePattern) {
        String database = support.resolveDatabase(catalog, schema);
        return support.countObjectsByName(
                connection,
                database,
                MySqlTemplate.SQL_COUNT_TABLES,
                tableNamePattern,
                MySqlTemplate.SQL_COUNT_TABLES_NAME_CLAUSE
        );
    }

    @Override
    public String getTableDdl(Connection connection, String catalog, String schema, String tableName) {
        return support.getObjectDdl(
                connection,
                catalog,
                tableName,
                MySqlTemplate.SQL_SHOW_CREATE_TABLE,
                "Create Table",
                DatabaseObjectTypeEnum.TABLE.getValue()
        );
    }

    @Override
    public void deleteTable(Connection connection, String catalog, String schema, String tableName) {
        support.dropObject(
                connection,
                catalog,
                tableName,
                MySqlTemplate.SQL_DROP_TABLE,
                DatabaseObjectTypeEnum.TABLE.getValue(),
                true
        );
    }

    @Override
    public SqlCommandResult insertRow(Connection connection, String catalog, String schema, String tableName,
                                      List<TableRowValue> values) {
        return rowWriteSupport.insertRow(connection, catalog, schema, tableName, values);
    }

    @Override
    public SqlCommandResult deleteRow(Connection connection, String catalog, String schema, String tableName,
                                      List<TableRowValue> matchValues, boolean force) {
        return rowWriteSupport.deleteRow(connection, catalog, schema, tableName, matchValues, force);
    }

    @Override
    public SqlCommandResult getTableData(Connection connection, String catalog, String schema,
                                         String tableName, int offset, int pageSize) {
        return support.getTableLikeData(connection, catalog, tableName, offset, pageSize);
    }

    @Override
    public long getTableDataCount(Connection connection, String catalog, String schema, String tableName) {
        return support.getTableLikeDataCount(connection, catalog, tableName);
    }

    @Override
    public SqlCommandResult getTableData(Connection connection, String catalog, String schema, String tableName,
                                         int offset, int pageSize, String whereClause,
                                         String orderByColumn, String orderByDirection) {
        return support.getTableLikeData(
                connection,
                catalog,
                tableName,
                offset,
                pageSize,
                whereClause,
                orderByColumn,
                orderByDirection
        );
    }

    @Override
    public long getTableDataCount(Connection connection, String catalog, String schema,
                                  String tableName, String whereClause) {
        return support.getTableLikeDataCount(connection, catalog, tableName, whereClause);
    }
}
