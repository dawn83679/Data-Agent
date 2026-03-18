package edu.zsc.ai.aspect;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.context.AgentInvocationContext;
import edu.zsc.ai.agent.tool.error.ToolErrorMapper;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AgentToolContextAspect {

    private final ToolErrorMapper toolErrorMapper;

    public AgentToolContextAspect(ToolErrorMapper toolErrorMapper) {
        this.toolErrorMapper = toolErrorMapper;
    }

    @Around("@within(edu.zsc.ai.agent.annotation.AgentTool) || @annotation(edu.zsc.ai.agent.annotation.AgentTool)")
    public Object handleToolContext(ProceedingJoinPoint joinPoint) {
        InvocationParameters parameters = findInvocationParameters(joinPoint.getArgs());
        AgentInvocationContext ctx = parameters != null ? AgentInvocationContext.from(parameters) : null;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        try (AgentInvocationContext ignored = ctx) {
            Object result = joinPoint.proceed();
            return enrichResult(ctx, result);
        } catch (Throwable t) {
            Object mappedFailure = toolErrorMapper.mapFailure(signature.getMethod(), joinPoint.getArgs(), t);
            return enrichResult(ctx, mappedFailure);
        }
    }

    private Object enrichResult(AgentInvocationContext ctx, Object result) {
        if (ctx != null && result instanceof AgentToolResult agentToolResult) {
            return ctx.timed(agentToolResult);
        }
        return result;
    }

    private InvocationParameters findInvocationParameters(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof InvocationParameters parameters) {
                return parameters;
            }
        }
        return null;
    }
}
