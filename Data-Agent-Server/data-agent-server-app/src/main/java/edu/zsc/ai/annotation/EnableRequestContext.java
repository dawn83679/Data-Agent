package edu.zsc.ai.annotation;

import java.lang.annotation.*;

/**
 * Enable Request Context
 * Annotate on Controller class to automatically manage RequestContext for all methods
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableRequestContext {
}
