package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Supported database type option for API (code + display name, no enum in API).
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbTypeOption {

    /**
     * Database type code (e.g. "mysql"), used in connection/driver APIs.
     */
    private String code;

    /**
     * Display name for UI (e.g. "MySQL").
     */
    private String displayName;

    /**
     * Whether this database type supports the concept of database (catalog).
     */
    private boolean supportDatabase;

    /**
     * Whether this database type supports schema as a separate namespace within a database.
     */
    private boolean supportSchema;
}
