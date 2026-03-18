package edu.zsc.ai.aspect;

import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.guard.AgentModeGuard;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AnnotationUtils;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AgentModeGuardAspect {

    @Around("@annotation(edu.zsc.ai.agent.annotation.DisallowInPlanMode)")
    public Object guardPlanMode(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        DisallowInPlanMode disallowInPlanMode = AnnotationUtils.findAnnotation(
                signature.getMethod(), DisallowInPlanMode.class);
        if (disallowInPlanMode != null) {
            AgentModeGuard.assertNotPlanMode(disallowInPlanMode.value());
        }
        return joinPoint.proceed();
    }
}
