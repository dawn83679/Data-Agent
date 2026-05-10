package edu.zsc.ai.agent.tool.model;

import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolResult {

    private static final String DEFAULT_SUCCESS_MESSAGE =
            "工具已成功完成。下一步使用这个结果。";

    private static final String DEFAULT_EMPTY_MESSAGE =
            "没有找到匹配结果。继续前调整范围，或询问用户明确目标。";

    private static final String DEFAULT_FAIL_MESSAGE =
            "工具执行失败。重试前检查当前输入和上下文。";

    private boolean success;

    private String message;

    private Object result;

    private Long elapsedMs;

    public static AgentToolResult success(Object result) {
        return success(result, DEFAULT_SUCCESS_MESSAGE);
    }

    public static AgentToolResult success(Object result, String message) {
        return AgentToolResult.builder()
                .success(true)
                .message(defaultMessage(message, DEFAULT_SUCCESS_MESSAGE))
                .result(result)
                .build();
    }

    public static AgentToolResult empty() {
        return empty(DEFAULT_EMPTY_MESSAGE);
    }

    public static AgentToolResult empty(String message) {
        return AgentToolResult.builder()
                .success(true)
                .message(defaultMessage(message, DEFAULT_EMPTY_MESSAGE))
                .build();
    }

    public static AgentToolResult noContext() {
        return AgentToolResult.builder()
                .success(false)
                .message(ToolMessageSupport.sentence(
                        "内部错误：用户会话上下文不可用。",
                        "这是系统问题，不要重试。",
                        "向用户说明该问题。"
                ))
                .build();
    }

    public static AgentToolResult fail(String error) {
        return AgentToolResult.builder()
                .success(false)
                .message(defaultMessage(error, DEFAULT_FAIL_MESSAGE))
                .build();
    }

    public static AgentToolResult fail(String customMessage, String errorDetail) {
        return AgentToolResult.builder()
                .success(false)
                .message(defaultMessage(customMessage, DEFAULT_FAIL_MESSAGE)
                        + " 错误：" + defaultMessage(errorDetail, "未知错误"))
                .build();
    }

    public static AgentToolResult fail(Throwable e) {
        return fail(e != null ? e.getMessage() : null);
    }

    private static String defaultMessage(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }
}
