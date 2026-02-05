package edu.zsc.ai.aspect;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.annotation.EnableRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.model.request.BaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Request Context Aspect
 * Automatically sets and clears RequestContext for controllers annotated with @EnableRequestContext
 */
@Aspect
@Component
@Slf4j
public class RequestContextAspect {
    
    /**
     * Intercept all methods in classes annotated with @EnableRequestContext
     */
    @Around("@within(enableRequestContext)")
    public Object handleRequestContext(ProceedingJoinPoint joinPoint, EnableRequestContext enableRequestContext) throws Throwable {
        try {
            // Find BaseRequest in method arguments
            for (Object arg : joinPoint.getArgs()) {
                if (!(arg instanceof BaseRequest)) {
                    continue;
                }
                
                BaseRequest request = (BaseRequest) arg;
                
                // Get userId from Sa-Token
                Long userId = null;
                try {
                    if (StpUtil.isLogin()) {
                        userId = StpUtil.getLoginIdAsLong();
                    }
                } catch (Exception e) {
                    log.warn("Failed to get userId from Sa-Token: {}", e.getMessage());
                }
                
                // Build and set context
                RequestContextInfo contextInfo = RequestContextInfo.builder()
                    .conversationId(request.getConversationId())
                    .userId(userId)
                    .connectionId(request.getConnectionId())
                    .databaseName(request.getDatabaseName())
                    .schemaName(request.getSchemaName())
                    .build();
                
                RequestContext.set(contextInfo);
                
                log.debug("RequestContext set for method: {}.{}, conversationId: {}, userId: {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    contextInfo.getConversationId(),
                    contextInfo.getUserId());
                
                break;  // Found and set context, no need to check remaining args
            }
            
            // Execute the actual method
            return joinPoint.proceed();
            
        } finally {
            // Always clear context after method execution
            if (RequestContext.hasContext()) {
                log.debug("Clearing RequestContext after method: {}.{}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
                RequestContext.clear();
            }
        }
    }
}
