package edu.zsc.ai.config.sys;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import jakarta.annotation.Resource;
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
//            @Override
//            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
//                // OPTIONS requests directly pass through (CORS preflight requests)
//                if ("OPTIONS".equals(request.getMethod())) {
//                    return true;
//                }
//
//                // Async dispatch: login already checked on initial request, skip to avoid SaTokenContext not set on this thread
//                if (DispatcherType.ASYNC.equals(request.getDispatcherType())) {
//                    return true;
//                }
//
//
//                // 1) Framework login check
//                StpUtil.checkLogin();
//                // 2) Business session active check
//                String token = StpUtil.getTokenValue();
//                long userId = StpUtil.getLoginIdAsLong();
//                FindSessionByTokenRequest req = new FindSessionByTokenRequest();
//                req.setAccessToken(token);
//                req.setUserId(userId);
//                SysSessions s = sessionService.findByAccessTokenAndUserId(req);
//                if (s == null || s.getActive() == null || !s.getActive().equals(SessionStatusEnum.ACTIVE.getValue())) {
//                    throw BusinessException.of(ResponseCode.UNAUTHORIZED, ResponseMessageKey.NOT_LOGIN_MESSAGE);
//                }
//                return true;
//            }
        })
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh",
                        "/api/auth/reset-password",
                        "/api/oauth/google",
                        "/api/oauth/callback/google",
                        "/api/oauth/github",
                        "/api/oauth/callback/github");
    }

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }
}
