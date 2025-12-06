package edu.zsc.ai.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test Environment Configuration
 * Uses real Redis connection for testing
 * 
 * @author Data-Agent Team
 */
@TestConfiguration
public class TestConfig {
    // Test environment uses real Redis and cache services
    // All configurations are read from application-test.yml
}
