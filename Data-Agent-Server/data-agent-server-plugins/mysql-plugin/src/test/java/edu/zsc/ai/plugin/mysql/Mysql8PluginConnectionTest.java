package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.connection.ConnectionConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test MySQL 8 plugin connection functionality.
 */
public class Mysql8PluginConnectionTest {

    private static final String DRIVER_JAR_PATH =
            System.getProperty("mysql.test.driverJar",
                    System.getenv().getOrDefault("MYSQL_TEST_DRIVER_JAR", ""));
    private static final String HOST =
            System.getProperty("mysql.test.host",
                    System.getenv().getOrDefault("MYSQL_TEST_HOST", "localhost"));
    private static final int PORT =
            Integer.parseInt(System.getProperty("mysql.test.port",
                    System.getenv().getOrDefault("MYSQL_TEST_PORT", "3306")));
    private static final String USERNAME =
            System.getProperty("mysql.test.username",
                    System.getenv().getOrDefault("MYSQL_TEST_USERNAME", "root"));
    private static final String PASSWORD =
            System.getProperty("mysql.test.password",
                    System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", "root"));
    private static final String DATABASE =
            System.getProperty("mysql.test.database",
                    System.getenv().getOrDefault("MYSQL_TEST_DATABASE", "mysql"));

    private static void assumeIntegrationEnvironment() {
        Assumptions.assumeTrue(DRIVER_JAR_PATH != null && !DRIVER_JAR_PATH.isBlank(),
                "Skipping MySQL integration test: MYSQL_TEST_DRIVER_JAR or -Dmysql.test.driverJar is not set");
        Assumptions.assumeTrue(Files.exists(Path.of(DRIVER_JAR_PATH)),
                "Skipping MySQL integration test: driver jar does not exist at " + DRIVER_JAR_PATH);
    }

    @Test
    public void testConnect() throws Exception {
        assumeIntegrationEnvironment();
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
        assumeIntegrationEnvironment();
        Mysql8Plugin plugin = new Mysql8Plugin();

        ConnectionConfig config = new ConnectionConfig();
        config.setHost(HOST);
        config.setPort(PORT);
        config.setDatabase(DATABASE);
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
            assertEquals(DATABASE, rs.getString("db_name"), "Should be connected to configured database");

            System.out.println("✅ MySQL 8 connection with database test successful!");

        } finally {
            if (connection != null) {
                plugin.closeConnection(connection);
            }
        }
    }

    @Test
    public void testConnectionWithProperties() throws Exception {
        assumeIntegrationEnvironment();
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
        assumeIntegrationEnvironment();
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
        assumeIntegrationEnvironment();
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
