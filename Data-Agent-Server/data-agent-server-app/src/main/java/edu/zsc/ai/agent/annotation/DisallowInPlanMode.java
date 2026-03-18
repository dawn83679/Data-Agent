package edu.zsc.ai.agent.annotation;

import edu.zsc.ai.common.enums.ai.ToolNameEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisallowInPlanMode {

    ToolNameEnum value();
}
