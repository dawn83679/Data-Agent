package edu.zsc.ai.domain.service.db;

import java.util.List;

/**
 * Schema Service Interface
 * Provides operations for listing schemas on an active connection.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public interface SchemaService {

    /**
     * List schemas in the given catalog for the given connection.
     *
     * @param connectionId unique connection identifier (from open connection)
     * @param catalog      catalog/database name; may be null to mean current catalog
     * @return list of schema names, never null
     * @throws edu.zsc.ai.util.exception.BusinessException if connection not found or plugin does not support SchemaProvider
     */
    List<String> listSchemas(String connectionId, String catalog);
}
