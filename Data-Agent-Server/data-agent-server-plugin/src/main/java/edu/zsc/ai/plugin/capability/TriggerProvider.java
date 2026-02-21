package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;

import java.sql.Connection;
import java.util.List;

public interface TriggerProvider {

    List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName);

    default String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        throw new UnsupportedOperationException("Plugin does not support getting trigger DDL");
    }
}
