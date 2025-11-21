package edu.zsc.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.enums.error.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.entity.Session;
import edu.zsc.ai.service.SessionService;
import lombok.RequiredArgsConstructor;

/**
 * Sa-Token 配置类
 * 配置 JWT 无状态模式和拦截器
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfigure implements WebMvcConfigurer {

    private final SessionService sessionService;

    /**
     * 注册 Sa-Token 的 JWT 无状态模式
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }

    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验登录状态和 Session
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 1. 验证登录状态
            StpUtil.checkLogin();
            
            // 2. 获取当前用户 ID 和 AccessToken
            Long userId = StpUtil.getLoginIdAsLong();
            String accessToken = StpUtil.getTokenValue();
            
            // 3. 验证 Session
            Session session = sessionService.getByAccessToken(accessToken);
            if (session == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Session not found");
            }
            
            if (!session.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Session user mismatch");
            }
            
            if (session.getStatus() != 0) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Session is not active");
            }
            
            // 4. Update session activity
            sessionService.updateActivity(session.getId());
        }))
        .addPathPatterns("/**")  // 拦截所有路径
        .excludePathPatterns(
            // 排除认证相关接口
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/reset-password",
            "/api/auth/send-code",
            "/api/auth/login/email-code",
            "/api/auth/google/login",
            "/api/auth/google/callback",
            // 排除健康检查接口
            "/api/health",
            // 排除 Swagger 文档接口
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**"
        );
    }
}
