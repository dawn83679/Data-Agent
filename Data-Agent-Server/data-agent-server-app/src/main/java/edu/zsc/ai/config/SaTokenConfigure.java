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
 * Sa-Token Configuration
 * Configures JWT stateless mode and interceptors
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfigure implements WebMvcConfigurer {

    private final SessionService sessionService;

    /**
     * Register Sa-Token JWT stateless mode
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }

    /**
     * Register Sa-Token interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register Sa-Token interceptor to validate login status and session
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 1. Verify login status
            StpUtil.checkLogin();
            
            // 2. Get current user ID and AccessToken
            Long userId = StpUtil.getLoginIdAsLong();
            String accessToken = StpUtil.getTokenValue();
            
            // 3. Verify Session
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
        .addPathPatterns("/**")  // Intercept all paths
        .excludePathPatterns(
            // Exclude authentication endpoints
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/reset-password",
            "/api/auth/send-code",
            "/api/auth/login/email-code",
            "/api/auth/google/login",
            "/api/auth/google/callback",
            "/api/auth/test-callback",
            "/api/auth/verify-email",
            // Exclude health check endpoints
            "/api/health",
            // Exclude Swagger documentation endpoints
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**"
        );
    }
}
