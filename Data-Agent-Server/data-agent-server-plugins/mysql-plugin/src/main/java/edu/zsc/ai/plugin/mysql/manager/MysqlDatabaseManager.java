package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.DatabaseManager;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;
import edu.zsc.ai.plugin.mysql.support.MysqlCapabilitySupport;

import java.sql.Connection;
import java.util.Objects;

public final class MysqlDatabaseManager implements DatabaseManager {

    private final MysqlCapabilitySupport support;

    public MysqlDatabaseManager(MysqlCapabilitySupport support) {
        this.support = Objects.requireNonNull(support, "support");
    }

    @Override
    public void deleteDatabase(Connection connection, String catalog) {
        support.dropObject(
                connection,
                catalog,
                catalog,
                MySqlTemplate.SQL_DROP_DATABASE,
                DatabaseObjectTypeEnum.DATABASE.getValue(),
                false
        );
    }
}
