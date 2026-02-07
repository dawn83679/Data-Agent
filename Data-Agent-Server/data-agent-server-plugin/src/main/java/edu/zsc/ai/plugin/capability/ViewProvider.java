package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.model.command.view.ViewCommandRequest;
import edu.zsc.ai.plugin.model.command.view.ViewCommandResult;

/**
 * View provider capability interface.
 * Defines operations for managing database views.
 */
public interface ViewProvider {

    /**
     * Create a new view
     *
     * @param request view command request containing view definition
     * @return view command result with operation status
     */
    ViewCommandResult createView(ViewCommandRequest request);

    /**
     * Get view definition
     *
     * @param request view command request containing view name
     * @return view command result with view definition
     */
    ViewCommandResult getViewDefinition(ViewCommandRequest request);

    /**
     * Alter an existing view
     *
     * @param request view command request containing new view definition
     * @return view command result with operation status
     */
    ViewCommandResult alterView(ViewCommandRequest request);

    /**
     * Drop a view
     *
     * @param request view command request containing view name
     * @return view command result with operation status
     */
    ViewCommandResult dropView(ViewCommandRequest request);

    /**
     * List all views in the database/schema
     *
     * @param request view command request containing database/schema info
     * @return view command result with list of views
     */
    ViewCommandResult listViews(ViewCommandRequest request);

    /**
     * Check if a view exists
     *
     * @param request view command request containing view name
     * @return view command result with existence status
     */
    ViewCommandResult viewExists(ViewCommandRequest request);

    /**
     * Set connection configuration for view operations.
     * This method should be called before performing any view operations.
     *
     * @param connectionConfig database connection configuration
     */
    default void setConnectionConfig(ConnectionConfig connectionConfig) {
        // Default implementation does nothing - implementations can override if needed
    }
}