package edu.zsc.ai.aspect;

import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order
@RequiredArgsConstructor
public class AgentToolLoggingAspect {

    private final AgentLogService agentLogService;

    @Around("@within(edu.zsc.ai.agent.annotation.AgentTool) || @annotation(edu.zsc.ai.agent.annotation.AgentTool)")
    public Object logToolInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        long startTime = System.currentTimeMillis();
        agentLogService.record(
                AgentLogType.TOOL_METHOD_START,
                signature.getDeclaringType().getSimpleName(),
                signature.getMethod().getName(),
                AgentLogFields.of("method", signature.getMethod().getName()));
        try {
            Object result = joinPoint.proceed();
            agentLogService.record(
                    AgentLogType.TOOL_METHOD_COMPLETE,
                    signature.getDeclaringType().getSimpleName(),
                    signature.getMethod().getName(),
                    AgentLogFields.of(
                            "method", signature.getMethod().getName(),
                            "elapsedMs", System.currentTimeMillis() - startTime,
                            "success", !(result instanceof AgentToolResult toolResult) || toolResult.isSuccess()));
            return result;
        } catch (Throwable throwable) {
            agentLogService.recordError(
                    AgentLogType.TOOL_METHOD_ERROR,
                    signature.getDeclaringType().getSimpleName(),
                    signature.getMethod().getName(),
                    throwable,
                    AgentLogFields.of("method", signature.getMethod().getName(), "elapsedMs", System.currentTimeMillis() - startTime));
            throw throwable;
        }
    }
}
