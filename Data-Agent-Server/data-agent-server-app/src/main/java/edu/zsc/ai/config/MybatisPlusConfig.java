package edu.zsc.ai.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 分页插件
     * 自 v3.5.9 起，分页插件需要额外引入 mybatis-plus-jsqlparser 依赖
     *
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件，指定数据库类型为 PostgreSQL
        // 注意：如配置多个插件，分页插件需放在最后
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        return interceptor;
    }
}