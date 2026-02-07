package edu.zsc.ai.plugin.mysql.view;

import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.capability.ViewProvider;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.model.command.view.ViewCommandRequest;
import edu.zsc.ai.plugin.model.command.view.ViewCommandResult;
import edu.zsc.ai.plugin.model.command.view.ViewInfo;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * MySQL implementation of ViewProvider.
 * Handles view operations for MySQL databases.
 */
public class MySQLViewProvider implements ViewProvider {

    private static final Logger logger = Logger.getLogger(MySQLViewProvider.class.getName());
    
    private final ConnectionProvider connectionProvider;
    private ConnectionConfig connectionConfig;

    /**
     * Constructor with ConnectionProvider dependency
     */
    public MySQLViewProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Set connection configuration for database operations
     */
    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    @Override
    public ViewCommandResult createView(ViewCommandRequest request) {
        if (request.getViewName() == null || request.getViewName().trim().isEmpty()) {
            return new ViewCommandResult(false, "View name is required");
        }
        if (request.getViewDefinition() == null || request.getViewDefinition().trim().isEmpty()) {
            return new ViewCommandResult(false, "View definition is required");
        }

        String sql = String.format("CREATE VIEW %s AS %s", 
            escapeIdentifier(request.getViewName()), 
            request.getViewDefinition());

        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            statement.executeUpdate(sql);
            logger.info(String.format("Successfully created view: %s", request.getViewName()));
            
            ViewCommandResult result = new ViewCommandResult(true, "View created successfully");
            result.setViewName(request.getViewName());
            result.setAffectedRows(1);
            return result;
            
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to create view %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Failed to create view %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        }
    }

    @Override
    public ViewCommandResult getViewDefinition(ViewCommandRequest request) {
        if (request.getViewName() == null || request.getViewName().trim().isEmpty()) {
            return new ViewCommandResult(false, "View name is required");
        }

        String sql = "SHOW CREATE VIEW " + escapeIdentifier(request.getViewName());

        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            if (resultSet.next()) {
                String viewDefinition = resultSet.getString("Create View");
                logger.info(String.format("Successfully retrieved view definition for: %s", request.getViewName()));
                
                ViewCommandResult result = new ViewCommandResult(request.getViewName(), viewDefinition, true);
                result.setMessage("View definition retrieved successfully");
                return result;
            } else {
                return new ViewCommandResult(false, String.format("View %s not found", request.getViewName()));
            }
            
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to get view definition for %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Failed to get view definition for %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        }
    }

    @Override
    public ViewCommandResult alterView(ViewCommandRequest request) {
        if (request.getViewName() == null || request.getViewName().trim().isEmpty()) {
            return new ViewCommandResult(false, "View name is required");
        }
        if (request.getViewDefinition() == null || request.getViewDefinition().trim().isEmpty()) {
            return new ViewCommandResult(false, "View definition is required");
        }

        String sql = String.format("ALTER VIEW %s AS %s", 
            escapeIdentifier(request.getViewName()), 
            request.getViewDefinition());

        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            statement.executeUpdate(sql);
            logger.info(String.format("Successfully altered view: %s", request.getViewName()));
            
            ViewCommandResult result = new ViewCommandResult(true, "View altered successfully");
            result.setViewName(request.getViewName());
            result.setAffectedRows(1);
            return result;
            
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to alter view %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Failed to alter view %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        }
    }

    @Override
    public ViewCommandResult dropView(ViewCommandRequest request) {
        if (request.getViewName() == null || request.getViewName().trim().isEmpty()) {
            return new ViewCommandResult(false, "View name is required");
        }

        String sql = "DROP VIEW " + escapeIdentifier(request.getViewName());

        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            statement.executeUpdate(sql);
            logger.info(String.format("Successfully dropped view: %s", request.getViewName()));
            
            ViewCommandResult result = new ViewCommandResult(true, "View dropped successfully");
            result.setViewName(request.getViewName());
            result.setAffectedRows(1);
            return result;
            
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to drop view %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Failed to drop view %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        }
    }

    @Override
    public ViewCommandResult listViews(ViewCommandRequest request) {
        String database = request.getDatabase();
        String sql;
        
        if (database != null && !database.trim().isEmpty()) {
            sql = "SELECT TABLE_NAME, VIEW_DEFINITION, TABLE_SCHEMA " +
                  "FROM INFORMATION_SCHEMA.VIEWS " +
                  "WHERE TABLE_SCHEMA = ? " +
                  "ORDER BY TABLE_NAME";
        } else {
            sql = "SELECT TABLE_NAME, VIEW_DEFINITION, TABLE_SCHEMA " +
                  "FROM INFORMATION_SCHEMA.VIEWS " +
                  "ORDER BY TABLE_SCHEMA, TABLE_NAME";
        }

        try (Connection connection = getConnection(request);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            if (database != null && !database.trim().isEmpty()) {
                statement.setString(1, database);
            }
            
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ViewInfo> viewList = new ArrayList<>();
                
                while (resultSet.next()) {
                    ViewInfo viewInfo = new ViewInfo();
                    viewInfo.setViewName(resultSet.getString("TABLE_NAME"));
                    viewInfo.setViewDefinition(resultSet.getString("VIEW_DEFINITION"));
                    viewInfo.setDatabase(resultSet.getString("TABLE_SCHEMA"));
                    viewInfo.setCreateTime(LocalDateTime.now()); // MySQL doesn't store creation time for views
                    viewList.add(viewInfo);
                }
                
                logger.info(String.format("Successfully retrieved %d views", viewList.size()));
                
                ViewCommandResult result = new ViewCommandResult(viewList, true);
                result.setMessage(String.format("Found %d views", viewList.size()));
                return result;
            }
            
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to list views: %s", e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Failed to list views: %s", e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        }
    }

    @Override
    public ViewCommandResult viewExists(ViewCommandRequest request) {
        if (request.getViewName() == null || request.getViewName().trim().isEmpty()) {
            return new ViewCommandResult(false, "View name is required");
        }

        String sql = "SELECT COUNT(*) as view_count FROM INFORMATION_SCHEMA.VIEWS " +
                     "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = ?";

        try (Connection connection = getConnection(request);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, request.getViewName());
            statement.setString(2, request.getDatabase() != null ? request.getDatabase() : connection.getCatalog());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean exists = resultSet.getInt("view_count") > 0;
                    String message = exists ? 
                        String.format("View %s exists", request.getViewName()) :
                        String.format("View %s does not exist", request.getViewName());
                    
                    ViewCommandResult result = new ViewCommandResult(true, message);
                    result.setViewName(request.getViewName());
                    return result;
                }
            }
            
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to check view existence for %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Failed to check view existence for %s: %s", request.getViewName(), e.getMessage());
            logger.severe(errorMsg);
            return new ViewCommandResult(false, errorMsg);
        }
        
        return new ViewCommandResult(false, "Unable to check view existence");
    }

    /**
     * Get database connection using the ConnectionProvider.
     */
    private Connection getConnection(ViewCommandRequest request) throws SQLException {
        if (connectionProvider == null) {
            throw new IllegalStateException("ConnectionProvider is not set");
        }
        
        if (connectionConfig == null) {
            throw new IllegalStateException("ConnectionConfig is not set. Call setConnectionConfig() first.");
        }
        
        try {
            // Use the database from request if provided, otherwise use the one from config
            ConnectionConfig requestConfig = connectionConfig;
            if (request.getDatabase() != null && !request.getDatabase().trim().isEmpty()) {
                // Create a copy of the config with the request database
                requestConfig = new ConnectionConfig();
                requestConfig.setHost(connectionConfig.getHost());
                requestConfig.setPort(connectionConfig.getPort());
                requestConfig.setDatabase(request.getDatabase());
                requestConfig.setUsername(connectionConfig.getUsername());
                requestConfig.setPassword(connectionConfig.getPassword());
                requestConfig.setProperties(connectionConfig.getProperties());
                requestConfig.setDriverJarPath(connectionConfig.getDriverJarPath());
                requestConfig.setTimeout(connectionConfig.getTimeout());
            }
            
            return connectionProvider.connect(requestConfig);
        } catch (RuntimeException e) {
            throw new SQLException("Failed to get database connection: " + e.getMessage(), e);
        }
    }

    /**
     * Escape SQL identifier to prevent SQL injection.
     */
    private String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        // Simple backtick escaping for MySQL
        return "`" + identifier.replace("`", "``") + "`";
    }
}