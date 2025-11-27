package edu.zsc.ai.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * 测试环境配置
 * 使用真实的 Redis 连接进行测试
 * 
 * @author Data-Agent Team
 */
@TestConfiguration
public class TestConfig {
    // 测试环境使用真实的 Redis 和缓存服务
    // 所有配置从 application-test.yml 读取
}
