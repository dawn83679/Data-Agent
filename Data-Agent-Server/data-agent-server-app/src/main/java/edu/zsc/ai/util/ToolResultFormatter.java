package edu.zsc.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.zsc.ai.service.ai.parse.rule.ToolUseXmlParseRule;
import lombok.extern.slf4j.Slf4j;
import edu.zsc.ai.enums.ai.message.MessageBlockTypeEnum;

import java.util.Objects;

/**
 * Tool call result formatter
 * Unified handling of tool call result formatting logic
 */
@Slf4j
public class ToolResultFormatter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String formatToolCallResult(String resultType, ToolUseXmlParseRule.ToolUseData toolUseData, String toolResult) {
        String toolResultFormat;
        MessageBlockTypeEnum messageBlockTypeEnum;
        try {
            messageBlockTypeEnum = MessageBlockTypeEnum.valueOf(resultType);
        } catch (IllegalArgumentException e) {
            log.error("Unknown result type: {}", resultType, e);
            messageBlockTypeEnum = MessageBlockTypeEnum.TOOL_CALL_RESULT;
        }

        // Most common case: TOOL_CALL_RESULT (default)
        if (messageBlockTypeEnum == MessageBlockTypeEnum.TOOL_CALL_RESULT) {
            toolResultFormat = formatToolCallResult(toolUseData, toolResult);
        } else {
            // Handle error cases with switch
            toolResultFormat = switch (messageBlockTypeEnum) {
                case TOOL_CALL_REJECT -> formatRejectToolCallResult(toolUseData);
                case TOOL_CALL_DUPLICATE -> formatDuplicateToolCallResult(toolUseData);
                case TOOL_CALL_NOT_FOUND -> formatToolNotFoundResult(toolUseData);
                case TOOL_CALL_ERROR -> formatToolCallErrorResult(toolUseData, toolResult);
                case TOOL_CALL_LIMIT_EXCEEDED -> formatToolCallLimitExceededResult(toolUseData);
                case TOOL_CALL_EXTRA_TEXT_ERROR -> formatToolCallExtraTextError(toolUseData);
                default -> formatToolCallResult(toolUseData, toolResult);
            };
        }
        return toolResultFormat;
    }

    /**
     * This is used for normal cases where tool execution results are obtained
     *
     * @param toolUseData Tool usage data
     * @param toolResult  Execution result JSON string
     * @return Formatted result string
     */
    private static String formatToolCallResult(ToolUseXmlParseRule.ToolUseData toolUseData, String toolResult) {
        if (Objects.isNull(toolUseData)) {
            return formatToolCallResult(toolResult);
        }
        return String.format(
                """
                        [SUCCESS] Tool [%s] executed successfully.
                        Parameters: %s
                        Result: %s

                        Please carefully analyze and summarize the above tool return results, and judge whether the results meet the task objectives in combination with the current context and user requirements.
                        Do not mechanically output the original tool results directly. When necessary, provide key information to users in concise and natural language, or confirm the direction of subsequent operations with users to ensure the accuracy and effectiveness of interaction.
                        """,
                toolUseData.getToolName(),
                tryToJsonString(toolUseData.getToolParams()),
                toolResult
        );
    }

    private static String formatToolCallResult(String resultJson) {
        return String.format(
                """
                        [SUCCESS] Tool executed successfully.
                        Result: %s

                        Please immediately analyze and summarize the above tool return results carefully, and judge whether the results meet the task objectives in combination with the current context and user requirements.
                        Do not mechanically output the original tool results directly. When necessary, provide key information to users in concise and natural language, or confirm the direction of subsequent operations with users to ensure the accuracy and effectiveness of interaction.
                        """,
                resultJson
        );
    }

    /*
     * This is used for prompts when users refuse to execute tools
     */
    private static String formatRejectToolCallResult(ToolUseXmlParseRule.ToolUseData toolUseData) {
        if (Objects.isNull(toolUseData)) {
            return formatRejectToolCallResult();
        }
        return String.format(
                """
                        [REJECT_TOOL_CALL_ERROR] User explicitly refused to use tool [%s], parameters: %s

                        You must immediately stop the call request for this tool and carefully analyze the reason for refusal.
                        Please review the current context and user expression to determine whether there are understanding deviations, parameter issues, or requirement misunderstandings.
                        If necessary, you can reconfirm the requirements or reasons for refusal with the user through concise and natural language. You must not continue to try to call this tool until you fully understand the user's intention.""",
                toolUseData.getToolName(),
                tryToJsonString(toolUseData.getToolParams())
        );
    }

    private static String formatRejectToolCallResult() {
        return """
                [REJECT_TOOL_CALL_ERROR] User explicitly refused to use this tool.
                You must immediately stop the call request for this tool and carefully analyze the reason for refusal.
                Please review the current context and user expression to determine whether there are understanding deviations, parameter issues, or requirement misunderstandings.
                If necessary, you can reconfirm the requirements or reasons for refusal with the user through concise and natural language. You must not continue to try to call this tool until you fully understand the user's intention.""";
    }

    private static String formatDuplicateToolCallResult(ToolUseXmlParseRule.ToolUseData toolUseData) {
        if (Objects.isNull(toolUseData)) {
            return formatDuplicateToolCallResult();
        }
        return String.format("""
                        [DUPLICATE_TOOL_CALL_ERROR] Detected duplicate call to tool [%s], parameters: %s

                        This is a serious error. Please stop duplicate calls immediately. You must first comprehensively review the previous call results of this tool to confirm whether the required information has been obtained.
                        You must not call this tool again without new goals or parameter changes. If duplicate calls are necessary, you must explain the new goals or modify parameters, and obtain supplementary information through user confirmation when necessary.""",
                toolUseData.getToolName(),
                tryToJsonString(toolUseData.getToolParams())
        );
    }

    private static String formatDuplicateToolCallResult() {
        return """
                [DUPLICATE_TOOL_CALL_ERROR] Detected duplicate tool call.
                This is a serious error. Please stop duplicate calls immediately. You must first comprehensively review the previous call results of this tool to confirm whether the required information has been obtained.
                You must not call this tool again without new goals or parameter changes. If duplicate calls are necessary, you must explain the new goals or modify parameters, and obtain supplementary information through user confirmation when necessary.""";
    }

    private static String formatToolNotFoundResult(ToolUseXmlParseRule.ToolUseData toolUseData) {
        if (Objects.isNull(toolUseData)) {
            return formatToolNotFoundResult();
        }
        return String.format(
                """
                        [TOOL_CALL_NOT_FOUND_ERROR] Unable to recognize tool [%s].
                        You need to immediately pause the current operation and re-examine the current task objectives.
                        Please determine whether you have selected the wrong tool name, or whether this tool is unavailable in the current context.
                        You can try other available tools when necessary, or confirm the expected functions and goals with users in natural language.""",
                toolUseData.getToolName()
        );
    }

    private static String formatToolNotFoundResult() {
        return """
                [TOOL_CALL_NOT_FOUND_ERROR] Unable to recognize tool.
                You need to immediately pause the current operation and re-examine the current task objectives.
                Please determine whether you have selected the wrong tool name, or whether this tool is unavailable in the current context.
                You can try other available tools when necessary, or confirm the expected functions and goals with users in natural language.""";
    }

    private static String formatToolCallErrorResult(ToolUseXmlParseRule.ToolUseData toolUseData, String toolResult) {
        if (Objects.isNull(toolUseData)) {
            return formatToolCallErrorResult(toolResult);
        }
        return String.format(
                """
                        [TOOL_CALL_ERROR] Tool [%s] execution failed.
                        Parameters: %s
                        Error message: %s

                        You must immediately stop repeated attempts and carefully analyze the above error information.
                        Please judge whether there are parameter errors, tool selection errors, or understanding deviations in combination with the context.
                        Before retrying, you should first adjust the calling strategy, or confirm details with users through natural language to ensure the correct operation direction.""",
                toolUseData.getToolName(),
                tryToJsonString(toolUseData.getToolParams()),
                toolResult
        );
    }

    private static String formatToolCallErrorResult(String toolResult) {
        return String.format(
                """
                        [TOOL_CALL_ERROR] Tool execution failed.
                        Error message: %s

                        You must immediately stop repeated attempts and carefully analyze the above error information.
                        Please judge whether there are parameter errors, tool selection errors, or understanding deviations in combination with the context.
                        Before retrying, you should first adjust the calling strategy, or confirm details with users through natural language to ensure the correct operation direction.""",
                toolResult
        );
    }

    private static String formatToolCallLimitExceededResult(ToolUseXmlParseRule.ToolUseData toolUseData) {
        if (toolUseData == null) {
            return formatToolCallLimitExceededResult();
        }
        return String.format("""
                        [TOOL_CALL_LIMIT_EXCEEDED_ERROR] This session only allows calling one tool, and this [%s] tool call has been rejected.
                        Parameters: %s

                        Please immediately stop this tool call request and carefully review the historical context and tool call results of this session to determine whether the required information has been obtained or the task objectives have been achieved.
                        Do not mechanically repeat tool calls. Only when there are clear new goals or parameter changes and it is necessary, should you try new tool calls.
                        If you have any questions, please confirm requirements or supplementary information with users through natural language to ensure the accuracy and effectiveness of interaction.
                        """,
                toolUseData.getToolName(),
                tryToJsonString(toolUseData.getToolParams())
        );
    }

    private static String formatToolCallLimitExceededResult() {
        return """
                [TOOL_CALL_LIMIT_EXCEEDED_ERROR] This session only allows calling one tool, and this tool call has been rejected.

                Please immediately stop this tool call request and carefully review the historical context and tool call results of this session to determine whether the required information has been obtained or the task objectives have been achieved.
                Do not mechanically repeat tool calls. Only when there are clear new goals or parameter changes and it is necessary, should you try new tool calls.
                If you have any questions, please confirm requirements or supplementary information with users through natural language to ensure the accuracy and effectiveness of interaction.
                """;
    }

    /**
     * Format extra text error after tool call
     */
    private static String formatToolCallExtraTextError(ToolUseXmlParseRule.ToolUseData toolData) {
        if (Objects.isNull(toolData)) {
            return formatToolCallExtraTextError();
        }
        return String.format(
                """
                        [TOOL_CALL_CRITICAL_ERROR] Critical error: You continued to output extra text content after calling tool [%s].
                        Tool parameters: %s

                        This is a serious protocol violation! You must immediately stop the current output and strictly follow the following rules:
                        1. Only one tool can be called per conversation round
                        2. You must immediately stop output after calling a tool and wait for the tool execution result
                        3. You must not output any additional text, explanations, or comments after tool calls
                        4. Tool calls must be the last content of your output

                        Please immediately correct your behavior pattern and strictly follow the tool call protocol in subsequent conversations.
                        """,
                toolData.getToolName(),
                tryToJsonString(toolData.getToolParams())
        );
    }

    private static String formatToolCallExtraTextError() {
        return """
                [TOOL_CALL_CRITICAL_ERROR] Critical error: You continued to output extra text content after calling a tool.

                This is a serious protocol violation! You must immediately stop the current output and strictly follow the following rules:
                1. Only one tool can be called per conversation round
                2. You must immediately stop output after calling a tool and wait for the tool execution result
                3. You must not output any additional text, explanations, or comments after tool calls
                4. Tool calls must be the last content of your output

                Please immediately correct your behavior pattern and strictly follow the tool call protocol in subsequent conversations.
                """;
    }

    /**
     * Helper method to safely convert object to JSON string
     */
    private static String tryToJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON serialization failed for object: {}", obj, e);
            return "\"" + obj.toString() + "\"";
        }
    }
}