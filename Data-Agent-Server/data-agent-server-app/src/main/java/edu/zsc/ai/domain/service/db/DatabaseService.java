package edu.zsc.ai.domain.service.db;

import java.util.List;

public interface DatabaseService {

    List<String> listDatabases(Long connectionId);

    /**
     * List databases for a connection, with explicit user for ownership. When userId is null, uses current login (StpUtil).
     */
    List<String> listDatabases(Long connectionId, Long userId);

    void deleteDatabase(Long connectionId, String databaseName, Long userId);
}
