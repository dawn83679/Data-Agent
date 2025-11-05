package edu.zsc.ai.plugin.annotation;

import edu.zsc.ai.plugin.enums.DbType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Plugin metadata annotation.
 * Used to declare plugin identification information and basic properties.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginInfo {
    
    /**
     * Plugin unique identifier (lowercase, e.g., "mysql", "postgresql")
     */
    String id();
    
    /**
     * Plugin display name (e.g., "MySQL Database")
     */
    String name();
    
    /**
     * Plugin version (semantic versioning recommended, e.g., "1.0.0")
     */
    String version();
    
    /**
     * Database type
     */
    DbType dbType();
    
    /**
     * Plugin description
     */
    String description() default "";
    
    /**
     * Vendor or author name
     */
    String vendor() default "Data-Agent Team";
    
    /**
     * Official website or documentation URL
     */
    String website() default "https://github.com/dawn83679/Data-Agent";
    
    /**
     * Minimum supported database version (e.g., "5.7.0", "8.0.0")
     */
    String supportMinVersion() default "";
    
    /**
     * Maximum supported database version (e.g., "5.7.99", "8.9.99").
     * Empty string means supporting all future versions.
     */
    String supportMaxVersion() default "";
}

