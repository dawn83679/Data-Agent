package edu.zsc.ai.plugin.capability;

import java.util.List;

/**
 * Capability interface for splitting a multi-statement SQL string into individual statements.
 * Plugins may implement this to handle dialect-specific delimiters
 * (e.g., PL/SQL BEGIN...END blocks, custom DELIMITER syntax).
 * If a plugin does not implement this interface, DefaultPluginManager falls back to DefaultSqlSplitter.
 */
public interface SqlSplitter {

    /**
     * Split the given SQL text into individual executable statements.
     * Implementations must trim each statement and exclude empty strings.
     *
     * @param sql the raw SQL text, potentially containing multiple statements
     * @return ordered list of non-empty, trimmed SQL statements
     */
    List<String> split(String sql);
}
