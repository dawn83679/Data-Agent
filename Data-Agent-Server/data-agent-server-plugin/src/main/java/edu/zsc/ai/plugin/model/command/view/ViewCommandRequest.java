package edu.zsc.ai.plugin.model.command.view;

import edu.zsc.ai.plugin.model.command.CommandRequest;

/**
 * View command request.
 * Represents a request to perform operations on database views.
 */
public class ViewCommandRequest implements CommandRequest {

    /**
     * View name
     */
    private String viewName;

    /**
     * View definition SQL
     */
    private String viewDefinition;

    /**
     * Operation type: CREATE, ALTER, DROP, QUERY, LIST
     */
    private ViewOperation operation;

    /**
     * Database name
     */
    private String database;

    /**
     * Schema name
     */
    private String schema;

    /**
     * View description
     */
    private String description;

    /**
     * Default constructor
     */
    public ViewCommandRequest() {
    }

    /**
     * Constructor with essential fields
     */
    public ViewCommandRequest(String viewName, ViewOperation operation, String database) {
        this.viewName = viewName;
        this.operation = operation;
        this.database = database;
    }

    // Getters and Setters
    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getViewDefinition() {
        return viewDefinition;
    }

    public void setViewDefinition(String viewDefinition) {
        this.viewDefinition = viewDefinition;
    }

    public ViewOperation getOperation() {
        return operation;
    }

    public void setOperation(ViewOperation operation) {
        this.operation = operation;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getCommand() {
        // Generate command based on operation type
        if (operation == null) {
            return null;
        }
        
        switch (operation) {
            case CREATE:
                return String.format("CREATE VIEW %s AS %s", viewName, viewDefinition);
            case ALTER:
                return String.format("ALTER VIEW %s AS %s", viewName, viewDefinition);
            case DROP:
                return String.format("DROP VIEW %s", viewName);
            case QUERY:
                return String.format("SHOW CREATE VIEW %s", viewName);
            case LIST:
                return "SHOW FULL TABLES WHERE Table_type = 'VIEW'";
            default:
                return operation.getCode();
        }
    }

    @Override
    public String toString() {
        return "ViewCommandRequest{" +
                "viewName='" + viewName + '\'' +
                ", operation=" + operation +
                ", database='" + database + '\'' +
                ", schema='" + schema + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    /**
     * View operation enumeration
     */
    public enum ViewOperation {
        CREATE("CREATE", "Create a new view"),
        ALTER("ALTER", "Modify an existing view"),
        DROP("DROP", "Delete a view"),
        QUERY("QUERY", "Query view definition"),
        LIST("LIST", "List all views");

        private final String code;
        private final String description;

        ViewOperation(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}