package edu.zsc.ai.service.ai.parse.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.zsc.ai.enums.ai.message.MessageBlockTypeEnum;
import edu.zsc.ai.model.dto.response.ai.ParseResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool use XML parsing rule
 * Specialized for parsing <tool_use> structured XML
 */
@Slf4j
public class ToolUseXmlParseRule extends AbstractParseRule {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Pattern toolUsePattern = Pattern.compile(
            "<tool_use>(.*?)</tool_use>",
            Pattern.DOTALL
    );

    // Match tool_name tag
    private final Pattern toolNamePattern = Pattern.compile(
            "<tool_name>(.*?)</tool_name>",
            Pattern.DOTALL
    );

    // Match tool_params tag
    private final Pattern toolParamsPattern = Pattern.compile(
            "<tool_params>(.*?)</tool_params>",
            Pattern.DOTALL
    );

    @Override
    public String getName() {
        return MessageBlockTypeEnum.TOOL_CALL.name();
    }

    @Override
    protected List<ParseResult> doParse(String buffer) {
        List<ParseResult> results = new ArrayList<>();

        Matcher toolUseMatcher = toolUsePattern.matcher(buffer);

        while (toolUseMatcher.find()) {
            String toolUseContent = toolUseMatcher.group(1);
            String fullXmlContent = toolUseMatcher.group(0); // Complete XML including tags
            int startPos = toolUseMatcher.start();
            int endPos = toolUseMatcher.end();

            ToolUseData toolUseData = parseToolUseContent(toolUseContent);

            // Create parse result
            // originalContent: Save original XML format for database
            // displayContent: Save JSON format for tool execution and frontend display
            String displayContent;
            try {
                displayContent = objectMapper.writeValueAsString(toolUseData);
            } catch (Exception e) {
                log.error("JSON serialization failed", e);
                displayContent = "{\"error\":\"Serialization failed\"}";
            }
            ParseResult result = new ParseResult(getName(), fullXmlContent, displayContent, startPos, endPos);
            results.add(result);

            log.debug("ToolUseXmlParseRule matched, tool: [{}], param count: [{}], range: [{}-{}], content: [{}]",
                    toolUseData.getToolName(), toolUseData.getToolParams().size(), startPos, endPos, fullXmlContent);
        }

        return results;
    }

    /**
     * Parse tool use content
     */
    private ToolUseData parseToolUseContent(String content) {
        ToolUseData.ToolUseDataBuilder builder = ToolUseData.builder();

        // Parse tool name
        Matcher toolNameMatcher = toolNamePattern.matcher(content);
        if (toolNameMatcher.find()) {
            builder.toolName(toolNameMatcher.group(1).trim());
        } else {
            throw new IllegalArgumentException("Missing tool name");
        }

        // Parse tool parameters
        Matcher toolParamsMatcher = toolParamsPattern.matcher(content);
        if (toolParamsMatcher.find()) {
            String paramsContent = toolParamsMatcher.group(1);
            Map<String, Object> params = parseToolParams(paramsContent);
            builder.toolParams(params);
        } else {
            builder.toolParams(new HashMap<>());
        }

        return builder.build();
    }

    /**
     * Parse tool parameters
     */
    private Map<String, Object> parseToolParams(String paramsContent) {
        Map<String, Object> params = new HashMap<>();

        // Use regex to match all parameter tags
        Pattern paramPattern = Pattern.compile("<([^>]+)>(.*?)</\\1>", Pattern.DOTALL);
        Matcher paramMatcher = paramPattern.matcher(paramsContent);

        while (paramMatcher.find()) {
            String paramName = paramMatcher.group(1).trim();
            Object paramValue = paramMatcher.group(2).trim();
            params.put(paramName, paramValue);
        }

        return params;
    }

    /**
     * Tool use data class
     */
    @Data
    @Builder
    public static class ToolUseData {
        private String toolName;
        private Map<String, Object> toolParams;
    }
}