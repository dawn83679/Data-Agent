package edu.zsc.ai.util;

import edu.zsc.ai.common.constant.HttpHeaderConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class HttpRequestUtil {
    private HttpRequestUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader(HttpHeaderConstant.X_FORWARDED_FOR);
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader(HttpHeaderConstant.USER_AGENT);
    }

    // Overloads: fetch from current request context; handle nulls inside
    public static String extractClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return extractClientIp(attrs != null ? attrs.getRequest() : null);
    }

    public static String extractUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return extractUserAgent(attrs != null ? attrs.getRequest() : null);
    }

    // Alias methods for consistency
    public static String getClientIp(HttpServletRequest request) {
        return extractClientIp(request);
    }

    public static String getUserAgent(HttpServletRequest request) {
        return extractUserAgent(request);
    }
}


