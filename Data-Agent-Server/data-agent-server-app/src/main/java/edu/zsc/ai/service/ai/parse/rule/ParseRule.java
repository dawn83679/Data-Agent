package edu.zsc.ai.service.ai.parse.rule;

import edu.zsc.ai.model.dto.response.ai.ParseResult;

import java.util.List;

/**
 * Parse rule interface
 */
public interface ParseRule {
    /**
     * Get rule name
     */
    String getName();

    /**
     * Try to parse buffer content
     *
     * @param buffer buffer content
     */
    List<ParseResult> tryParse(String buffer);

    /**
     * Rule priority (lower number = higher priority)
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Parse complete callback interface
     */
    @FunctionalInterface
    interface ParseCompleteCallback {
        /**
         * Callback when parsing is complete
         *
         * @param rule    parse rule
         * @param results parse results
         */
        void onParseComplete(ParseRule rule, List<ParseResult> results);
    }

    /**
     * Parse error callback interface
     */
    @FunctionalInterface
    interface ParseErrorCallback {
        /**
         * Callback when parsing error occurs
         *
         * @param rule      parse rule
         * @param buffer    buffer content
         * @param error     error message
         * @param exception exception object (maybe null)
         */
        void onParseError(ParseRule rule, String buffer, String error, Exception exception);
    }

    /**
     * Real-time tool parsing callback for showing tool parsing progress to users
     */
    @FunctionalInterface
    interface ParsingCallBack {
        /**
         * Called when tool parsing parts are detected in real-time
         *
         * @param toolPartType the type of part being parsed: "tool_name", "tool_param",
         *                     "tool_description"
         * @param jsonValue    JSON value representing the parsed part:
         *                     - For tool_name: {"toolName": "create_todo"}
         *                     - For tool_description: {"toolDescription": "Create a new
         *                     todo item"}
         *                     - For tool_param: {"title": "My task title", "priority":
         *                     "high"}
         */
        void onToolPartParsed(String toolPartType, String jsonValue);
    }

    /**
     * Set parse complete callback
     *
     * @param callback callback function
     */
    default void setParseCompleteCallback(ParseCompleteCallback callback) {
        // Default implementation is empty, subclasses can override
    }

    /**
     * Set parse error callback
     *
     * @param callback callback function
     */
    default void setParseErrorCallback(ParseErrorCallback callback) {
        // Default implementation is empty, subclasses can override
    }

    default void setParsingCallBack(ParsingCallBack callback) {
        // Default implementation is empty, subclasses can override
    }

    /**
     * Get parse complete callback
     */
    default ParseCompleteCallback getParseCompleteCallback() {
        return null;
    }

    /**
     * Get parse error callback
     */
    default ParseErrorCallback getParseErrorCallback() {
        return null;
    }

    default ParsingCallBack getParsingCallBack() {
        return null;
    }
}