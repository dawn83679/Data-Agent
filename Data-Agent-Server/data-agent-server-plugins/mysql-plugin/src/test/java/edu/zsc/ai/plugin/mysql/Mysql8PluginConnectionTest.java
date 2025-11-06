package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.model.ConnectionConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test MySQL 8 plugin connection functionality.
 */
public class Mysql8PluginConnectionTest {

    private static final String DRIVER_JAR_PATH = "/Users/dawn/Desktop/mysql-connector-j-8.2.0.jar";
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    @Test
    public void testConnect() throws Exception {
        Mysql8Plugin plugin = new Mysql8Plugin();

        ConnectionConfig config = new ConnectionConfig();
        config.setHost(HOST);
        config.setPort(PORT);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setDriverJarPath(DRIVER_JAR_PATH);

        Connection connection = null;
        try {
            connection = plugin.connect(config);
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");

            // Test query
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 as test");
            assertTrue(rs.next(), "Should have result");
            assertEquals(1, rs.getInt("test"), "Result should be 1");

            System.out.println("✅ MySQL 8 connection test successful!");

        } finally {
            if (connection != null) {
                plugin.closeConnection(connection);
            }
        }
    }

    @Test
    public void testConnectWithDatabase() throws Exception {
        Mysql8Plugin plugin = new Mysql8Plugin();

        ConnectionConfig config = new ConnectionConfig();
        config.setHost(HOST);
        config.setPort(PORT);
        config.setDatabase("mysql");
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setDriverJarPath(DRIVER_JAR_PATH);

        Connection connection = null;
        try {
            connection = plugin.connect(config);
            assertNotNull(connection, "Connection should not be null");

            // Verify we're connected to the mysql database
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATABASE() as db_name");
            assertTrue(rs.next(), "Should have result");
            assertEquals("mysql", rs.getString("db_name"), "Should be connected to mysql database");

            System.out.println("✅ MySQL 8 connection with database test successful!");

        } finally {
            if (connection != null) {
                plugin.closeConnection(connection);
            }
        }
    }

    @Test
    public void testConnectionWithProperties() throws Exception {
        Mysql8Plugin plugin = new Mysql8Plugin();

        // Build properties map
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(HOST);
        config.setPort(PORT);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setDriverJarPath(DRIVER_JAR_PATH);
        
        // Add custom properties
        config.addProperty("useSSL", "false");
        config.addProperty("allowPublicKeyRetrieval", "true");
        config.addProperty("serverTimezone", "Asia/Shanghai");

        Connection connection = null;
        try {
            connection = plugin.connect(config);
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");

            System.out.println("✅ MySQL 8 connection with properties test successful!");

        } finally {
            if (connection != null) {
                plugin.closeConnection(connection);
            }
        }
    }

    @Test
    public void testTestConnection() {
        Mysql8Plugin plugin = new Mysql8Plugin();

        ConnectionConfig config = new ConnectionConfig();
        config.setHost(HOST);
        config.setPort(PORT);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setDriverJarPath(DRIVER_JAR_PATH);

        boolean result = plugin.testConnection(config);
        assertTrue(result, "Connection test should succeed");

        System.out.println("✅ MySQL 8 testConnection() test successful!");
    }

    @Test
    public void testTestConnectionFailure() {
        Mysql8Plugin plugin = new Mysql8Plugin();

        ConnectionConfig config = new ConnectionConfig();
        config.setHost(HOST);
        config.setPort(PORT);
        config.setUsername(USERNAME);
        config.setPassword("wrong_password");
        config.setDriverJarPath(DRIVER_JAR_PATH);

        boolean result = plugin.testConnection(config);
        assertFalse(result, "Connection test should fail with wrong password");

        System.out.println("✅ MySQL 8 testConnection() failure test successful!");
    }
}

