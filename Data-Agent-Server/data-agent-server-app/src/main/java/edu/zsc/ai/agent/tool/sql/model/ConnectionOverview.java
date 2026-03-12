package edu.zsc.ai.agent.tool.sql.model;

import java.util.List;

/**
 * Connection overview with nested catalog/schema hierarchy.
 * When the connection could not be reached (e.g. timeout, refused), {@code error} is set so the model can see the failure.
 */
public record ConnectionOverview(Long id, String name, String dbType, List<CatalogInfo> catalogs, String error) {

    public ConnectionOverview(Long id, String name, String dbType, List<CatalogInfo> catalogs) {
        this(id, name, dbType, catalogs, null);
    }
}
