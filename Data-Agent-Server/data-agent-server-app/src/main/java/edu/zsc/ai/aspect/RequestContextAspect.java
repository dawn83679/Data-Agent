package edu.zsc.ai.aspect;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.aspect.annotation.EnableRequestContext;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.api.model.request.BaseRequest;
import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import edu.zsc.ai.domain.service.org.OrgAccessService;
import edu.zsc.ai.domain.service.org.OrgMemberContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Request Context Aspect
 * Automatically sets and clears RequestContext for controllers annotated with @EnableRequestContext
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RequestContextAspect {

    private final OrgAccessService orgAccessService;
    
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

                RequestContextInfo existing = RequestContext.snapshot();

                Long userId = null;
                try {
                    if (StpUtil.isLogin()) {
                        userId = StpUtil.getLoginIdAsLong();
                    }
                } catch (Exception e) {
                    log.warn("Failed to get userId from Sa-Token: {}", e.getMessage());
                }
                if (userId == null && existing != null) {
                    userId = existing.getUserId();
                }

                RequestContextInfo.RequestContextInfoBuilder builder =
                        existing != null ? existing.toBuilder() : RequestContextInfo.builder();

                RequestContextInfo contextInfo = builder
                        .conversationId(request.getConversationId())
                        .userId(userId)
                        .connectionId(request.getConnectionId())
                        .catalog(request.getCatalog())
                        .schema(request.getSchema())
                        .build();

                contextInfo = applyChatWorkspaceBodyOverride(contextInfo, request, userId);

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
            }
            AgentExecutionContext.clear();
            AgentRequestContext.clear();
            RequestContext.clear();
        }
    }

    /**
     * When {@link ChatRequest} carries {@code clientWorkspaceType} / {@code clientOrgId}, apply validated
     * org context if HTTP headers did not already establish an organization workspace.
     */
    private RequestContextInfo applyChatWorkspaceBodyOverride(RequestContextInfo contextInfo,
                                                              BaseRequest request,
                                                              Long userId) {
        if (!(request instanceof ChatRequest chat) || userId == null) {
            return contextInfo;
        }
        if (!StringUtils.hasText(chat.getClientWorkspaceType())) {
            return contextInfo;
        }
        if (contextInfo.getWorkspaceType() == WorkspaceTypeEnum.ORGANIZATION
                && contextInfo.getOrgId() != null) {
            return contextInfo;
        }
        String type = chat.getClientWorkspaceType().trim().toUpperCase();
        if ("PERSONAL".equals(type)) {
            return contextInfo.toBuilder()
                    .workspaceType(WorkspaceTypeEnum.PERSONAL)
                    .orgId(null)
                    .orgUserRelId(null)
                    .orgRole(null)
                    .build();
        }
        if ("ORGANIZATION".equals(type) && chat.getClientOrgId() != null) {
            OrgMemberContext membership = orgAccessService.loadActiveMembership(userId, chat.getClientOrgId());
            return contextInfo.toBuilder()
                    .workspaceType(WorkspaceTypeEnum.ORGANIZATION)
                    .orgId(membership.orgId())
                    .orgUserRelId(membership.organizationMemberId())
                    .orgRole(membership.role())
                    .build();
        }
        return contextInfo;
    }
}
