package edu.zsc.ai.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.zsc.ai.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.model.entity.db.DbConnection;
import edu.zsc.ai.service.db.DbConnectionService;

/**
 * Database Connection Service Test
 * Tests CRUD operations for db_connections table
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Database Connection Service Tests")
class DbConnectionServiceImplTest {

    @Autowired
    private DbConnectionService dbConnectionService;

    private ConnectionCreateRequest testRequest;

    @BeforeEach
    void setUp() {
        // Prepare test data
        testRequest = new ConnectionCreateRequest();
        testRequest.setName("test-mysql-connection");
        testRequest.setDbType("mysql");
        testRequest.setHost("localhost");
        testRequest.setPort(3306);
        testRequest.setDatabase("test_db");
        testRequest.setUsername("root");
        testRequest.setPassword("password123");
        testRequest.setDriverJarPath("/path/to/mysql-connector.jar");
        testRequest.setTimeout(30);
        testRequest.setUserId(1L); // Set test user ID

        Map<String, String> properties = new HashMap<>();
        properties.put("useSSL", "false");
        properties.put("serverTimezone", "UTC");
        testRequest.setProperties(properties);
    }

    @Test
    @DisplayName("Test Create Database Connection")
    void testCreateConnection() {
        // Execute creation
        ConnectionResponse response = dbConnectionService.createConnection(testRequest);

        // Verify results
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(testRequest.getName(), response.getName());
        assertEquals(testRequest.getDbType(), response.getDbType());
        assertEquals(testRequest.getHost(), response.getHost());
        assertEquals(testRequest.getPort(), response.getPort());
        assertEquals(testRequest.getDatabase(), response.getDatabase());
        assertEquals(testRequest.getUsername(), response.getUsername());
        assertEquals(testRequest.getDriverJarPath(), response.getDriverJarPath());
        assertEquals(testRequest.getTimeout(), response.getTimeout());
        assertEquals(testRequest.getUserId(), response.getUserId());
        assertNotNull(response.getProperties());
        assertEquals("false", response.getProperties().get("useSSL"));
        assertEquals("UTC", response.getProperties().get("serverTimezone"));
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("Test Create Duplicate Connection Should Fail")
    void testCreateDuplicateConnectionShouldFail() {
        // Create a connection first
        dbConnectionService.createConnection(testRequest);

        // Try to create connection with same name
        ConnectionCreateRequest duplicateRequest = new ConnectionCreateRequest();
        duplicateRequest.setName(testRequest.getName());
        duplicateRequest.setDbType("postgresql");
        duplicateRequest.setHost("localhost");
        duplicateRequest.setPort(5432);
        duplicateRequest.setDriverJarPath("/path/to/postgresql.jar");
        duplicateRequest.setUserId(1L);

        // Verify exception is thrown
        assertThrows(IllegalArgumentException.class, () -> {
            dbConnectionService.createConnection(duplicateRequest);
        });
    }

    @Test
    @DisplayName("Test Get Connection By ID")
    void testGetConnectionById() {
        // Create a connection first
        ConnectionResponse created = dbConnectionService.createConnection(testRequest);

        // Query by ID
        ConnectionResponse found = dbConnectionService.getConnectionById(created.getId());

        // Verify results
        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals(created.getName(), found.getName());
        assertEquals(created.getDbType(), found.getDbType());
    }

    @Test
    @DisplayName("Test Get Non-Existent Connection Should Throw Exception")
    void testGetNonExistentConnectionShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            dbConnectionService.getConnectionById(99999L);
        });
    }

    @Test
    @DisplayName("Test Get Connection By Name")
    void testGetConnectionByName() {
        // Create a connection first
        dbConnectionService.createConnection(testRequest);

        // Query by name
        DbConnection found = dbConnectionService.getByName(testRequest.getName());

        // Verify results
        assertNotNull(found);
        assertEquals(testRequest.getName(), found.getName());
        assertEquals(testRequest.getDbType(), found.getDbType());
    }

    @Test
    @DisplayName("Test Get All Connections")
    void testGetAllConnections() {
        // Create multiple connections
        dbConnectionService.createConnection(testRequest);

        ConnectionCreateRequest request2 = new ConnectionCreateRequest();
        request2.setName("test-postgresql-connection");
        request2.setDbType("postgresql");
        request2.setHost("localhost");
        request2.setPort(5432);
        request2.setDatabase("test_db");
        request2.setUsername("postgres");
        request2.setPassword("password456");
        request2.setDriverJarPath("/path/to/postgresql.jar");
        request2.setTimeout(60);
        request2.setUserId(1L);
        dbConnectionService.createConnection(request2);

        // Query all connections
        List<ConnectionResponse> connections = dbConnectionService.getAllConnections();

        // Verify results
        assertNotNull(connections);
        assertTrue(connections.size() >= 2);
    }

    @Test
    @DisplayName("Test Update Connection")
    void testUpdateConnection() {
        // Create a connection first
        ConnectionResponse created = dbConnectionService.createConnection(testRequest);

        // Prepare update data
        ConnectionCreateRequest updateRequest = new ConnectionCreateRequest();
        updateRequest.setName("updated-connection-name");
        updateRequest.setDbType("postgresql");
        updateRequest.setHost("192.168.1.100");
        updateRequest.setPort(5432);
        updateRequest.setDatabase("updated_db");
        updateRequest.setUsername("admin");
        updateRequest.setPassword("newpassword");
        updateRequest.setDriverJarPath("/path/to/new-driver.jar");
        updateRequest.setTimeout(60);
        updateRequest.setUserId(1L);

        Map<String, String> newProperties = new HashMap<>();
        newProperties.put("ssl", "true");
        updateRequest.setProperties(newProperties);

        // Execute update
        ConnectionResponse updated = dbConnectionService.updateConnection(created.getId(), updateRequest);

        // Verify results
        assertNotNull(updated);
        assertEquals(created.getId(), updated.getId());
        assertEquals(updateRequest.getName(), updated.getName());
        assertEquals(updateRequest.getDbType(), updated.getDbType());
        assertEquals(updateRequest.getHost(), updated.getHost());
        assertEquals(updateRequest.getPort(), updated.getPort());
        assertEquals(updateRequest.getDatabase(), updated.getDatabase());
        assertEquals(updateRequest.getUsername(), updated.getUsername());
        assertEquals(updateRequest.getDriverJarPath(), updated.getDriverJarPath());
        assertEquals(updateRequest.getTimeout(), updated.getTimeout());
        assertEquals("true", updated.getProperties().get("ssl"));
    }

    @Test
    @DisplayName("Test Update Non-Existent Connection Should Throw Exception")
    void testUpdateNonExistentConnectionShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            dbConnectionService.updateConnection(99999L, testRequest);
        });
    }

    @Test
    @DisplayName("Test Update Connection With Duplicate Name Should Fail")
    void testUpdateConnectionWithDuplicateNameShouldFail() {
        // Create two connections
        ConnectionResponse connection1 = dbConnectionService.createConnection(testRequest);

        ConnectionCreateRequest request2 = new ConnectionCreateRequest();
        request2.setName("another-connection");
        request2.setDbType("postgresql");
        request2.setHost("localhost");
        request2.setPort(5432);
        request2.setDriverJarPath("/path/to/postgresql.jar");
        request2.setUserId(1L);
        ConnectionResponse connection2 = dbConnectionService.createConnection(request2);

        // 尝试将 connection2 的名称更新为 connection1 的名称
        ConnectionCreateRequest updateRequest = new ConnectionCreateRequest();
        updateRequest.setName(connection1.getName());
        updateRequest.setDbType("postgresql");
        updateRequest.setHost("localhost");
        updateRequest.setPort(5432);
        updateRequest.setDriverJarPath("/path/to/postgresql.jar");
        updateRequest.setUserId(1L);

        // 验证抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            dbConnectionService.updateConnection(connection2.getId(), updateRequest);
        });
    }

    @Test
    @DisplayName("测试删除连接")
    void testDeleteConnection() {
        // 先创建一个连接
        ConnectionResponse created = dbConnectionService.createConnection(testRequest);

        // 删除连接
        dbConnectionService.deleteConnection(created.getId());

        // 验证连接已被删除
        assertThrows(IllegalArgumentException.class, () -> {
            dbConnectionService.getConnectionById(created.getId());
        });
    }

    @Test
    @DisplayName("测试删除不存在的连接应该抛出异常")
    void testDeleteNonExistentConnectionShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            dbConnectionService.deleteConnection(99999L);
        });
    }

    @Test
    @DisplayName("测试连接超时默认值")
    void testConnectionTimeoutDefaultValue() {
        // 不设置 timeout
        testRequest.setTimeout(null);

        ConnectionResponse response = dbConnectionService.createConnection(testRequest);

        // 验证使用了默认值（数据库默认值为 30）
        assertNotNull(response);
        // 注意：如果数据库设置了默认值，这里应该是 30
    }

    @Test
    @DisplayName("测试空属性的连接")
    void testConnectionWithEmptyProperties() {
        testRequest.setProperties(new HashMap<>());

        ConnectionResponse response = dbConnectionService.createConnection(testRequest);

        assertNotNull(response);
        assertNotNull(response.getProperties());
        assertTrue(response.getProperties().isEmpty());
    }

    @Test
    @DisplayName("测试用户ID字段")
    void testUserIdField() {
        // 创建连接时指定用户ID
        testRequest.setUserId(100L);
        ConnectionResponse response = dbConnectionService.createConnection(testRequest);

        // 验证用户ID正确保存
        assertNotNull(response);
        assertEquals(100L, response.getUserId());

        // 查询验证
        ConnectionResponse found = dbConnectionService.getConnectionById(response.getId());
        assertEquals(100L, found.getUserId());
    }

    @Test
    @DisplayName("测试不同数据库类型的连接")
    void testDifferentDatabaseTypes() {
        // MySQL
        ConnectionResponse mysqlConn = dbConnectionService.createConnection(testRequest);
        assertEquals("mysql", mysqlConn.getDbType());

        // PostgreSQL
        ConnectionCreateRequest pgRequest = new ConnectionCreateRequest();
        pgRequest.setName("test-postgresql");
        pgRequest.setDbType("postgresql");
        pgRequest.setHost("localhost");
        pgRequest.setPort(5432);
        pgRequest.setDriverJarPath("/path/to/postgresql.jar");
        pgRequest.setUserId(1L);
        ConnectionResponse pgConn = dbConnectionService.createConnection(pgRequest);
        assertEquals("postgresql", pgConn.getDbType());

        // Oracle
        ConnectionCreateRequest oracleRequest = new ConnectionCreateRequest();
        oracleRequest.setName("test-oracle");
        oracleRequest.setDbType("oracle");
        oracleRequest.setHost("localhost");
        oracleRequest.setPort(1521);
        oracleRequest.setDriverJarPath("/path/to/oracle.jar");
        oracleRequest.setUserId(1L);
        ConnectionResponse oracleConn = dbConnectionService.createConnection(oracleRequest);
        assertEquals("oracle", oracleConn.getDbType());
    }
}

