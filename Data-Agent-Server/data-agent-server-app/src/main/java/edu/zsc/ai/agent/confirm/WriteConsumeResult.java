package edu.zsc.ai.agent.confirm;

public record WriteConsumeResult(
    boolean success,
    String reason,
    String detail
) {
    public static WriteConsumeResult ok() {
        return new WriteConsumeResult(true, null, null);
    }

    public static WriteConsumeResult fail(String reason, String detail) {
        return new WriteConsumeResult(false, reason, detail);
    }
}
