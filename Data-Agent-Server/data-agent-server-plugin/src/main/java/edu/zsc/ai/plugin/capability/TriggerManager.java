package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface TriggerManager {

    default List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support listing triggers");
    }

    default List<TriggerMetadata> searchTriggers(Connection connection, String catalog, String schema,
                                                 String tableName, String triggerNamePattern) {
        List<TriggerMetadata> triggers = getTriggers(connection, catalog, schema, tableName);
        if (triggers == null || triggers.isEmpty() || StringUtils.isBlank(triggerNamePattern)
                || "%".equals(triggerNamePattern)) {
            return triggers;
        }

        Pattern regex = Pattern.compile(toJdbcLikeRegex(triggerNamePattern), Pattern.CASE_INSENSITIVE);
        return triggers.stream()
                .filter(trigger -> trigger != null && StringUtils.isNotBlank(trigger.name()))
                .filter(trigger -> regex.matcher(trigger.name()).matches())
                .collect(Collectors.toList());
    }

    default String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        throw new UnsupportedOperationException("Plugin does not support getting trigger DDL");
    }

    default void deleteTrigger(Connection connection, String catalog, String schema, String triggerName) {
        throw new UnsupportedOperationException("Plugin does not support deleting trigger");
    }

    private static String toJdbcLikeRegex(String jdbcPattern) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : jdbcPattern.toCharArray()) {
            switch (c) {
                case '%' -> regex.append(".*");
                case '_' -> regex.append('.');
                case '\\', '^', '$', '.', '|', '?', '*', '+', '(', ')', '[', ']', '{', '}' ->
                        regex.append('\\').append(c);
                default -> regex.append(c);
            }
        }
        regex.append('$');
        return regex.toString();
    }
}
