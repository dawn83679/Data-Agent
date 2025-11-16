package edu.zsc.ai.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * Configure the pagination interceptor.
     * Since v3.5.9 the pagination plugin requires the mybatis-plus-jsqlparser dependency.
     *
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // Add the pagination interceptor and target PostgreSQL.
        // When you register multiple inner interceptors, pagination must be added last.
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        return interceptor;
    }
}