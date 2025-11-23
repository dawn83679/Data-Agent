package edu.zsc.ai.model.dto.request.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.zsc.ai.enums.ai.message.MessageBlockTypeEnum;
import edu.zsc.ai.service.ai.parse.rule.ToolUseXmlParseRule;
import edu.zsc.ai.util.ToolResultFormatter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Data
@NoArgsConstructor
@Slf4j
public class ToolCallResult {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Tool name
    private String toolName;
    // Tool parameters
    private Map<String, String> toolParams;
    // Whether successful
    private boolean success;
    // Error message
    private String errorMessage;
    // Actual execution result of the tool
    private Object originToolResult;
    // JSON format tool result, used for saving to database and sending to frontend
    private String jsonToolResult = null;
    // Formatted tool result, used for sending to model
    private String formatToolResult;
    /**
     * @see MessageBlockTypeEnum
     */
    private String toolCallResultType;

    public static ToolCallResult toolCallError(String toolName, Map<String, String> params) {
        return toolCallError(toolName, params, null);
    }

    public static ToolCallResult toolCallError(String toolName, Map<String, String> params, String errorMessage) {
        ToolCallResult toolCallResult = new ToolCallResult();
        toolCallResult.setToolName(toolName);
        toolCallResult.setToolParams(params);
        toolCallResult.setSuccess(false);
        toolCallResult.setErrorMessage(errorMessage);
        String resultType = MessageBlockTypeEnum.TOOL_CALL_ERROR.name();
        toolCallResult.setToolCallResultType(resultType);
        toolCallResult.setOriginToolResult(errorMessage);
        String jsonErrorMessage;
        try {
            jsonErrorMessage = objectMapper.writeValueAsString(errorMessage);
        } catch (Exception e) {
            jsonErrorMessage = "\"Serialization error\"";
            log.error("JSON serialization failed", e);
        }
        toolCallResult.setJsonToolResult(jsonErrorMessage);
        toolCallResult.setFormatToolResult(ToolResultFormatter.formatToolCallResult(resultType,
                ToolUseXmlParseRule.ToolUseData.builder().toolName(toolName).toolParams(params).build(), jsonErrorMessage));
        return toolCallResult;
    }

    public static ToolCallResult toolCallError(String toolName, Map<String, String> params, String errorMessage, Object originResult) {
        ToolCallResult result = toolCallError(toolName, params, errorMessage);
        result.setOriginToolResult(originResult);
        return result;
    }

    public static ToolCallResult toolCallNotFound(String toolName, Map<String, String> params) {
        ToolCallResult toolCallResult = new ToolCallResult();
        toolCallResult.setToolName(toolName);
        toolCallResult.setToolParams(params);
        toolCallResult.setSuccess(false);
        String resultType = MessageBlockTypeEnum.TOOL_CALL_NOT_FOUND.name();
        toolCallResult.setToolCallResultType(resultType);
        toolCallResult.setOriginToolResult("Tool not found: " + toolName);
        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString("Tool not found: " + toolName);
        } catch (Exception e) {
            jsonResult = "\"Serialization error\"";
            log.error("JSON serialization failed", e);
        }
        toolCallResult.setJsonToolResult(jsonResult);
        toolCallResult.setFormatToolResult(ToolResultFormatter.formatToolCallResult(resultType,
                ToolUseXmlParseRule.ToolUseData.builder().toolName(toolName).toolParams(params).build(), jsonResult));
        return toolCallResult;
    }

    public static ToolCallResult success(String toolName, Map<String, String> params, Object result) {
        ToolCallResult toolCallResult = new ToolCallResult();
        toolCallResult.setToolName(toolName);
        toolCallResult.setToolParams(params);
        toolCallResult.setSuccess(true);
        String resultType = MessageBlockTypeEnum.TOOL_CALL_RESULT.name();
        toolCallResult.setToolCallResultType(resultType);
        toolCallResult.setOriginToolResult(result);
        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            jsonResult = "\"Serialization error\"";
            log.error("JSON serialization failed", e);
        }
        toolCallResult.setJsonToolResult(jsonResult);
        toolCallResult.setFormatToolResult(ToolResultFormatter.formatToolCallResult(resultType,
                ToolUseXmlParseRule.ToolUseData.builder().toolName(toolName).toolParams(params).build(), jsonResult));
        return toolCallResult;
    }

    public static ToolCallResult toolCallReject(String toolName, Map<String, String> params) {
        ToolCallResult toolCallResult = new ToolCallResult();
        toolCallResult.setToolName(toolName);
        toolCallResult.setToolParams(params);
        toolCallResult.setSuccess(false);
        String resultType = MessageBlockTypeEnum.TOOL_CALL_REJECT.name();
        toolCallResult.setToolCallResultType(resultType);
        toolCallResult.setOriginToolResult("User rejected using tool");
        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString("User rejected using tool");
        } catch (Exception e) {
            jsonResult = "\"Serialization error\"";
            log.error("JSON serialization failed", e);
        }
        toolCallResult.setJsonToolResult(jsonResult);
        toolCallResult.setFormatToolResult(ToolResultFormatter.formatToolCallResult(resultType,
                ToolUseXmlParseRule.ToolUseData.builder().toolName(toolName).toolParams(params).build(), jsonResult));
        return toolCallResult;
    }

    public static ToolCallResult toolCallLimitExceeded(String toolName, Map<String, String> params) {
        ToolCallResult toolCallResult = new ToolCallResult();
        toolCallResult.setToolName(toolName);
        toolCallResult.setToolParams(params);
        toolCallResult.setSuccess(false);
        String resultType = MessageBlockTypeEnum.TOOL_CALL_LIMIT_EXCEEDED.name();
        toolCallResult.setToolCallResultType(resultType);
        toolCallResult.setOriginToolResult("Tool call limit exceeded");
        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString("Tool call limit exceeded");
        } catch (Exception e) {
            jsonResult = "\"Serialization error\"";
            log.error("JSON serialization failed", e);
        }
        toolCallResult.setJsonToolResult(jsonResult);
        toolCallResult.setFormatToolResult(ToolResultFormatter.formatToolCallResult(resultType,
                ToolUseXmlParseRule.ToolUseData.builder().toolName(toolName).toolParams(params).build(), jsonResult));
        return toolCallResult;
    }

    public static ToolCallResult toolCallDuplicate(String toolName, Map<String, String> params) {
        ToolCallResult toolCallResult = new ToolCallResult();
        toolCallResult.setToolName(toolName);
        toolCallResult.setToolParams(params);
        toolCallResult.setSuccess(false);
        String resultType = MessageBlockTypeEnum.TOOL_CALL_DUPLICATE.name();
        toolCallResult.setToolCallResultType(resultType);
        toolCallResult.setOriginToolResult("Duplicate tool call detected");
        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString("Duplicate tool call detected");
        } catch (Exception e) {
            jsonResult = "\"Serialization error\"";
            log.error("JSON serialization failed", e);
        }
        toolCallResult.setJsonToolResult(jsonResult);
        toolCallResult.setFormatToolResult(ToolResultFormatter.formatToolCallResult(resultType,
                ToolUseXmlParseRule.ToolUseData.builder().toolName(toolName).toolParams(params).build(), jsonResult));
        return toolCallResult;
    }

    public static ToolCallResult toolCallExtraTextError(String toolName, Map<String, String> params) {
        ToolCallResult toolCallResult = new ToolCallResult();
        toolCallResult.setToolName(toolName);
        toolCallResult.setToolParams(params);
        toolCallResult.setSuccess(false);
        String resultType = MessageBlockTypeEnum.TOOL_CALL_EXTRA_TEXT_ERROR.name();
        toolCallResult.setToolCallResultType(resultType);
        toolCallResult.setOriginToolResult("Extra text output after tool call");
        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString("Extra text output after tool call");
        } catch (Exception e) {
            jsonResult = "\"Serialization error\"";
            log.error("JSON serialization failed", e);
        }
        toolCallResult.setJsonToolResult(jsonResult);
        toolCallResult.setFormatToolResult(ToolResultFormatter.formatToolCallResult(resultType,
                ToolUseXmlParseRule.ToolUseData.builder().toolName(toolName).toolParams(params).build(), jsonResult));
        return toolCallResult;
    }
}