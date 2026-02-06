package edu.zsc.ai.util;

import org.apache.commons.lang3.StringUtils;

/**
 * ToolResultFormatter
 * Standardizes the output format of AI tools.
 * Format: TYPE:content
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public class ToolResultFormatter {

    private static final String ERROR_PREFIX = "ERROR:";
    private static final String EMPTY_PREFIX = "EMPTY:";
    private static final String SUCCESS_PREFIX = "SUCCESS:";

    private static final String DEFAULT_ERROR_MSG = "Internal error occurred during tool execution. Please check if the tool parameters are correct, and consider asking the user for more details if you are unsure about the task requirements.";
    private static final String DEFAULT_EMPTY_MSG = "No data found or result is empty.";
    private static final String DEFAULT_SUCCESS_MSG = "Operation completed successfully.";

    /**
     * Format an error result using default message.
     *
     * @return Formatted error string
     */
    public static String error() {
        return error(null);
    }

    /**
     * Format an error result.
     *
     * @param message Custom error message, if blank, use default.
     * @return Formatted error string
     */
    public static String error(String message) {
        return ERROR_PREFIX + (StringUtils.isNotBlank(message) ? message : DEFAULT_ERROR_MSG);
    }

    /**
     * Format an empty result using default message.
     *
     * @return Formatted empty string
     */
    public static String empty() {
        return empty(null);
    }

    /**
     * Format an empty result.
     *
     * @param message Custom empty message, if blank, use default.
     * @return Formatted empty string
     */
    public static String empty(String message) {
        return EMPTY_PREFIX + (StringUtils.isNotBlank(message) ? message : DEFAULT_EMPTY_MSG);
    }

    /**
     * Format a success result using default message.
     *
     * @return Formatted success string
     */
    public static String success() {
        return success(null);
    }

    /**
     * Format a success result.
     *
     * @param message Custom success message, if blank, use default.
     * @return Formatted success string
     */
    public static String success(String message) {
        return SUCCESS_PREFIX + (StringUtils.isNotBlank(message) ? message : DEFAULT_SUCCESS_MSG);
    }
}
