package edu.zsc.ai.plugin.exception;

/**
 * Plugin error code constants.
 * Defines all error codes used in plugin exceptions.
 */
public class PluginErrorCode {
    
    /**
     * Generic plugin error
     */
    public static final String PLUGIN_ERROR = "PLUGIN_ERROR";
    
    /**
     * Plugin metadata missing (e.g., @PluginInfo annotation not found)
     */
    public static final String PLUGIN_METADATA_MISSING = "PLUGIN_METADATA_MISSING";
    
    /**
     * Plugin initialization failed
     */
    public static final String PLUGIN_INIT_FAILED = "PLUGIN_INIT_FAILED";
    
    /**
     * Plugin start failed
     */
    public static final String PLUGIN_START_FAILED = "PLUGIN_START_FAILED";
    
    /**
     * Plugin stop failed
     */
    public static final String PLUGIN_STOP_FAILED = "PLUGIN_STOP_FAILED";
    
    /**
     * Plugin destroy failed
     */
    public static final String PLUGIN_DESTROY_FAILED = "PLUGIN_DESTROY_FAILED";
    
    /**
     * Plugin not found
     */
    public static final String PLUGIN_NOT_FOUND = "PLUGIN_NOT_FOUND";
    
    /**
     * Plugin already exists
     */
    public static final String PLUGIN_ALREADY_EXISTS = "PLUGIN_ALREADY_EXISTS";
    
    /**
     * Plugin capability not supported
     */
    public static final String CAPABILITY_NOT_SUPPORTED = "CAPABILITY_NOT_SUPPORTED";
    
    /**
     * Database connection failed
     */
    public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
    
    /**
     * SQL execution failed
     */
    public static final String SQL_EXECUTION_FAILED = "SQL_EXECUTION_FAILED";
    
    /**
     * Invalid configuration
     */
    public static final String INVALID_CONFIGURATION = "INVALID_CONFIGURATION";
    
    /**
     * Driver version not supported
     */
    public static final String DRIVER_NOT_SUPPORTED = "DRIVER_NOT_SUPPORTED";
    
    private PluginErrorCode() {
        // Private constructor to prevent instantiation
    }
}

