package edu.zsc.ai.plugin.mysql.view;

import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.model.command.view.ViewCommandRequest;
import edu.zsc.ai.plugin.model.command.view.ViewCommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for MySQLViewProvider.
 * Tests the basic functionality of view operations with proper connection management.
 */
class MySQLViewProviderTest {

    @Mock
    private ConnectionProvider mockConnectionProvider;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    private MySQLViewProvider viewProvider;
    private ConnectionConfig connectionConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        viewProvider = new MySQLViewProvider(mockConnectionProvider);
        
        // Setup connection config
        connectionConfig = new ConnectionConfig();
        connectionConfig.setHost("localhost");
        connectionConfig.setPort(3306);
        connectionConfig.setDatabase("test_db");
        connectionConfig.setUsername("test_user");
        connectionConfig.setPassword("test_password");
        connectionConfig.setDriverJarPath("/path/to/mysql-connector.jar");
        
        viewProvider.setConnectionConfig(connectionConfig);
    }

    @Test
    void testCreateView_WithValidRequest_ShouldReturnSuccess() throws SQLException {
        // Given
        ViewCommandRequest request = new ViewCommandRequest();
        request.setViewName("test_view");
        request.setViewDefinition("SELECT * FROM users");
        request.setDatabase("test_db");
        request.setOperation(ViewCommandRequest.ViewOperation.CREATE);

        // Mock connection behavior
        when(mockConnectionProvider.connect(any(ConnectionConfig.class))).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeUpdate(anyString())).thenReturn(0);

        // When
        ViewCommandResult result = viewProvider.createView(request);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("View created successfully", result.getMessage());
        assertEquals("test_view", result.getViewName());
        assertEquals(1, result.getAffectedRows());
        
        // Verify SQL execution
        verify(mockStatement).executeUpdate(contains("CREATE VIEW"));
    }

    @Test
    void testCreateView_WithNullViewName_ShouldReturnError() {
        // Given
        ViewCommandRequest request = new ViewCommandRequest();
        request.setViewName(null);
        request.setViewDefinition("SELECT * FROM users");
        request.setOperation(ViewCommandRequest.ViewOperation.CREATE);

        // When
        ViewCommandResult result = viewProvider.createView(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("View name is required", result.getMessage());
        
        // Verify no connection attempt was made
        verifyNoInteractions(mockConnectionProvider);
    }

    @Test
    void testCreateView_WithEmptyViewDefinition_ShouldReturnError() {
        // Given
        ViewCommandRequest request = new ViewCommandRequest();
        request.setViewName("test_view");
        request.setViewDefinition("");
        request.setOperation(ViewCommandRequest.ViewOperation.CREATE);

        // When
        ViewCommandResult result = viewProvider.createView(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("View definition is required", result.getMessage());
        
        // Verify no connection attempt was made
        verifyNoInteractions(mockConnectionProvider);
    }

    @Test
    void testGetViewDefinition_WithNullViewName_ShouldReturnError() {
        // Given
        ViewCommandRequest request = new ViewCommandRequest();
        request.setViewName(null);
        request.setOperation(ViewCommandRequest.ViewOperation.QUERY);

        // When
        ViewCommandResult result = viewProvider.getViewDefinition(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("View name is required", result.getMessage());
        
        // Verify no connection attempt was made
        verifyNoInteractions(mockConnectionProvider);
    }

    @Test
    void testDropView_WithConnectionError_ShouldReturnError() {
        // Given
        ViewCommandRequest request = new ViewCommandRequest();
        request.setViewName("test_view");
        request.setOperation(ViewCommandRequest.ViewOperation.DROP);

        // Mock connection failure
        when(mockConnectionProvider.connect(any(ConnectionConfig.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // When
        ViewCommandResult result = viewProvider.dropView(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Failed to drop view test_view"));
        assertTrue(result.getMessage().contains("Connection failed"));
    }

    @Test
    void testListViews_WithConnectionError_ShouldReturnError() {
        // Given
        ViewCommandRequest request = new ViewCommandRequest();
        request.setDatabase("test_db");
        request.setOperation(ViewCommandRequest.ViewOperation.LIST);

        // Mock connection failure
        when(mockConnectionProvider.connect(any(ConnectionConfig.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // When
        ViewCommandResult result = viewProvider.listViews(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Failed to list views"));
        assertTrue(result.getMessage().contains("Connection failed"));
    }

    @Test
    void testViewProvider_WithoutConnectionConfig_ShouldReturnError() {
        // Given
        MySQLViewProvider providerWithoutConfig = new MySQLViewProvider(mockConnectionProvider);
        ViewCommandRequest request = new ViewCommandRequest();
        request.setViewName("test_view");
        request.setViewDefinition("SELECT * FROM users");
        request.setOperation(ViewCommandRequest.ViewOperation.CREATE);

        // When
        ViewCommandResult result = providerWithoutConfig.createView(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("ConnectionConfig is not set"));
    }

    @Test
    void testViewProvider_WithoutConnectionProvider_ShouldReturnError() {
        // Given
        MySQLViewProvider providerWithoutProvider = new MySQLViewProvider(null);
        providerWithoutProvider.setConnectionConfig(connectionConfig);
        
        ViewCommandRequest request = new ViewCommandRequest();
        request.setViewName("test_view");
        request.setViewDefinition("SELECT * FROM users");
        request.setOperation(ViewCommandRequest.ViewOperation.CREATE);

        // When
        ViewCommandResult result = providerWithoutProvider.createView(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("ConnectionProvider is not set"));
    }
}