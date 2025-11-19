package edu.zsc.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API Documentation Configuration
 *
 * @author Data-Agent Team
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Data-Agent API Documentation")
                        .version("1.0.0")
                        .description("Database AI-Agent API - Intelligent database connection and query management system")
                        .contact(new Contact()
                                .name("Data-Agent Team")
                                .email("support@data-agent.com")
                                .url("https://github.com/your-org/data-agent"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    @Bean
    public GroupedOpenApi connectionApi() {
        return GroupedOpenApi.builder()
                .group("1. Connection Management")
                .pathsToMatch("/api/connection/**")
                .build();
    }

    @Bean
    public GroupedOpenApi driverApi() {
        return GroupedOpenApi.builder()
                .group("2. Driver Management")
                .pathsToMatch("/api/driver/**")
                .build();
    }

    @Bean
    public GroupedOpenApi healthApi() {
        return GroupedOpenApi.builder()
                .group("3. System Health")
                .pathsToMatch("/api/health/**", "/actuator/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("All APIs")
                .pathsToMatch("/api/**")
                .build();
    }
}
