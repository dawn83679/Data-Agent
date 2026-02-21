package edu.zsc.ai.config;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.zsc.ai.mcp.config.McpServerConfig;

/**
 * MCP 配置测试
 * 
 * 测试读取和解析 mcp-servers.json 配置文件
 */
@DisplayName("MCP 配置测试")
class McpConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("测试读取 mcp-servers.json 配置文件")
    void testLoadMcpServersJson() throws IOException {
        // 读取配置文件
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");
        assertTrue(resource.exists(), "mcp-servers.json 文件应该存在");

        // 解析配置
        McpServerConfig config = objectMapper.readValue(
            resource.getInputStream(), 
            McpServerConfig.class
        );

        // 验证配置结构
        assertNotNull(config, "配置对象不应为 null");
        assertNotNull(config.getMcpServers(), "服务器映射不应为 null");
        assertEquals(3, config.getMcpServers().size(), "应该有 3 个服务器配置");
    }

    @Test
    @DisplayName("测试 filesystem 服务器配置")
    void testFilesystemServerConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");
        McpServerConfig config = objectMapper.readValue(
            resource.getInputStream(), 
            McpServerConfig.class
        );

        // 验证 filesystem 服务器
        assertTrue(config.getMcpServers().containsKey("filesystem"), "应该包含 filesystem 服务器");
        
        McpServerConfig.McpServer fsServer = config.getMcpServers().get("filesystem");
        assertNotNull(fsServer, "filesystem 服务器配置不应为 null");
        assertEquals("npx", fsServer.getCommand(), "命令应该是 npx");
        assertEquals(3, fsServer.getArgs().length, "应该有 3 个参数");
        assertEquals("-y", fsServer.getArgs()[0], "第一个参数应该是 -y");
        assertEquals("@modelcontextprotocol/server-filesystem", fsServer.getArgs()[1]);
        assertEquals("/tmp", fsServer.getArgs()[2]);
        assertFalse(fsServer.isDisabled(), "filesystem 服务器应该是启用的");
    }

    @Test
    @DisplayName("测试 github 服务器配置")
    void testGithubServerConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");
        McpServerConfig config = objectMapper.readValue(
            resource.getInputStream(), 
            McpServerConfig.class
        );

        // 验证 github 服务器
        assertTrue(config.getMcpServers().containsKey("github"), "应该包含 github 服务器");
        
        McpServerConfig.McpServer githubServer = config.getMcpServers().get("github");
        assertNotNull(githubServer, "github 服务器配置不应为 null");
        assertEquals("npx", githubServer.getCommand());
        assertTrue(githubServer.isDisabled(), "github 服务器应该是禁用的");
        assertNotNull(githubServer.getEnv(), "环境变量不应为 null");
        assertTrue(githubServer.getEnv().containsKey("GITHUB_PERSONAL_ACCESS_TOKEN"), 
            "应该包含 GITHUB_PERSONAL_ACCESS_TOKEN");
    }

    @Test
    @DisplayName("测试 postgres 服务器配置")
    void testPostgresServerConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");
        McpServerConfig config = objectMapper.readValue(
            resource.getInputStream(), 
            McpServerConfig.class
        );

        // 验证 postgres 服务器
        assertTrue(config.getMcpServers().containsKey("postgres"), "应该包含 postgres 服务器");
        
        McpServerConfig.McpServer pgServer = config.getMcpServers().get("postgres");
        assertNotNull(pgServer, "postgres 服务器配置不应为 null");
        assertEquals("npx", pgServer.getCommand());
        assertTrue(pgServer.isDisabled(), "postgres 服务器应该是禁用的");
        assertEquals(3, pgServer.getArgs().length);
        assertTrue(pgServer.getArgs()[2].startsWith("postgresql://"), 
            "第三个参数应该是 PostgreSQL 连接字符串");
    }

    @Test
    @DisplayName("测试统计启用和禁用的服务器")
    void testCountEnabledAndDisabledServers() throws IOException {
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");
        McpServerConfig config = objectMapper.readValue(
            resource.getInputStream(), 
            McpServerConfig.class
        );

        // 统计启用和禁用的服务器
        long enabledCount = config.getMcpServers().values().stream()
            .filter(server -> !server.isDisabled())
            .count();
        
        long disabledCount = config.getMcpServers().values().stream()
            .filter(McpServerConfig.McpServer::isDisabled)
            .count();

        assertEquals(1, enabledCount, "应该有 1 个启用的服务器");
        assertEquals(2, disabledCount, "应该有 2 个禁用的服务器");
    }
}
