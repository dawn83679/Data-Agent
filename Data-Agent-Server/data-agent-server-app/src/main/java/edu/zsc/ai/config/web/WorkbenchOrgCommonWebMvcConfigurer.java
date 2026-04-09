package edu.zsc.ai.config.web;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Blocks organization {@link OrganizationRoleEnum#COMMON} from REST workbench / metadata APIs.
 * AI chat and read-only connection listing (GET) remain allowed; see {@link #isAllowedConnectionsGet(String, String)}.
 */
@Configuration
@Order(1)
public class WorkbenchOrgCommonWebMvcConfigurer implements WebMvcConfigurer {

    private static final List<String> WORKBENCH_API_PREFIXES = List.of(
            "/api/db/",
            "/api/tables",
            "/api/views",
            "/api/schemas",
            "/api/databases",
            "/api/columns",
            "/api/indexes",
            "/api/primary-keys",
            "/api/procedures",
            "/api/functions",
            "/api/triggers",
            "/api/drivers",
            "/api/permissions",
            "/api/ai/observability"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        if (!shouldEnforce(request)) {
                            return true;
                        }
                        String method = request.getMethod();
                        String path = request.getServletPath();
                        if (isAllowedConnectionsGet(method, path)) {
                            return true;
                        }
                        if (isForbiddenForOrgCommon(path)) {
                            throw new BusinessException(ResponseCode.FORBIDDEN, ResponseMessageKey.WORKSPACE_COMMON_WORKBENCH_FORBIDDEN);
                        }
                        return true;
                    }
                })
                .addPathPatterns("/api/**")
                .order(1);
    }

    private static boolean shouldEnforce(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return false;
        }
        if (DispatcherType.ASYNC.equals(request.getDispatcherType())) {
            return false;
        }
        if (RequestContext.getWorkspaceTypeOrPersonal() != WorkspaceTypeEnum.ORGANIZATION) {
            return false;
        }
        return RequestContext.getOrgRole() == OrganizationRoleEnum.COMMON;
    }

    /**
     * Allow listing connections and fetching a single connection by numeric id (read-only).
     */
    private static boolean isAllowedConnectionsGet(String method, String path) {
        if (!"GET".equals(method) || path == null || !path.startsWith("/api/connections")) {
            return false;
        }
        if ("/api/connections".equals(path) || "/api/connections/".equals(path)) {
            return true;
        }
        if (!path.startsWith("/api/connections/")) {
            return false;
        }
        String suffix = path.substring("/api/connections/".length());
        if (suffix.endsWith("/")) {
            suffix = suffix.substring(0, suffix.length() - 1);
        }
        return suffix.matches("\\d+");
    }

    private static boolean isForbiddenForOrgCommon(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith("/api/connections")) {
            return true;
        }
        for (String prefix : WORKBENCH_API_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
