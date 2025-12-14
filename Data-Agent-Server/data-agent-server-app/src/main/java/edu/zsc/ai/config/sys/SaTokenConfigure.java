package edu.zsc.ai.config.sys;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.common.constant.ResponseConstant;
import edu.zsc.ai.domain.model.enums.SessionStatusEnum;
import edu.zsc.ai.domain.model.dto.request.sys.FindSessionByTokenRequest;
import edu.zsc.ai.domain.model.entity.sys.SysSessions;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import edu.zsc.ai.util.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Resource
    private SysSessionsService sessionService;

    // Register interceptor
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Custom interceptor to exclude OPTIONS requests
        registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        // OPTIONS requests directly pass through (CORS preflight requests)
                        if ("OPTIONS".equals(request.getMethod())) {
                            return true;
                        }

                        // 1) Framework login check
                        StpUtil.checkLogin();
                        // 2) Business session active check
                        String token = StpUtil.getTokenValue();
                        long userId = StpUtil.getLoginIdAsLong();
                        FindSessionByTokenRequest req = new FindSessionByTokenRequest();
                        req.setAccessToken(token);
                        req.setUserId(userId);
                        SysSessions s = sessionService.findByAccessTokenAndUserId(req);
                        if (s == null || s.getActive() == null || s.getActive() != SessionStatusEnum.ACTIVE.getValue()) {
                            throw BusinessException.of(ResponseConstant.UNAUTHORIZED, ResponseConstant.NOT_LOGIN_MESSAGE);
                        }
                        return true;
                    }
                })
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/reset-password");
    }

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }
}
