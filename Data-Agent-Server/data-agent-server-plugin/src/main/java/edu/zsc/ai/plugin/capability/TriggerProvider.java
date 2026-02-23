package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;

public interface TriggerProvider {

    default List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support listing triggers");
    }

    default String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        throw new UnsupportedOperationException("Plugin does not support getting trigger DDL");
    }

    default void deleteTrigger(Connection connection, String catalog, String schema, String triggerName) {
        throw new UnsupportedOperationException("Plugin does not support deleting trigger");
    }
}
