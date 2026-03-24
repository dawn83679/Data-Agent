package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.ViewManager;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;
import edu.zsc.ai.plugin.mysql.constant.MysqlShowColumnConstants;
import edu.zsc.ai.plugin.mysql.support.MysqlCapabilitySupport;

import java.sql.Connection;
import java.util.Objects;

public final class MysqlViewManager implements ViewManager {

    private final MysqlCapabilitySupport support;

    public MysqlViewManager(MysqlCapabilitySupport support) {
        this.support = Objects.requireNonNull(support, "support");
    }

    @Override
    public long countViews(Connection connection, String catalog, String schema, String viewNamePattern) {
        String database = support.resolveDatabase(catalog, schema);
        return support.countObjectsByName(
                connection,
                database,
                MySqlTemplate.SQL_COUNT_VIEWS,
                viewNamePattern,
                MySqlTemplate.SQL_COUNT_TABLES_NAME_CLAUSE
        );
    }

    @Override
    public String getViewDdl(Connection connection, String catalog, String schema, String viewName) {
        return support.getObjectDdl(
                connection,
                catalog,
                viewName,
                MySqlTemplate.SQL_SHOW_CREATE_VIEW,
                MysqlShowColumnConstants.CREATE_VIEW,
                DatabaseObjectTypeEnum.VIEW.getValue()
        );
    }

    @Override
    public void deleteView(Connection connection, String catalog, String schema, String viewName) {
        support.dropObject(
                connection,
                catalog,
                viewName,
                MySqlTemplate.SQL_DROP_VIEW,
                DatabaseObjectTypeEnum.VIEW.getValue(),
                true
        );
    }

    @Override
    public SqlCommandResult getViewData(Connection connection, String catalog, String schema,
                                        String viewName, int offset, int pageSize) {
        return support.getTableLikeData(connection, catalog, viewName, offset, pageSize);
    }

    @Override
    public long getViewDataCount(Connection connection, String catalog, String schema, String viewName) {
        return support.getTableLikeDataCount(connection, catalog, viewName);
    }

    @Override
    public SqlCommandResult getViewData(Connection connection, String catalog, String schema, String viewName,
                                        int offset, int pageSize, String whereClause,
                                        String orderByColumn, String orderByDirection) {
        return support.getTableLikeData(
                connection,
                catalog,
                viewName,
                offset,
                pageSize,
                whereClause,
                orderByColumn,
                orderByDirection
        );
    }

    @Override
    public long getViewDataCount(Connection connection, String catalog, String schema,
                                 String viewName, String whereClause) {
        return support.getTableLikeDataCount(connection, catalog, viewName, whereClause);
    }
}
