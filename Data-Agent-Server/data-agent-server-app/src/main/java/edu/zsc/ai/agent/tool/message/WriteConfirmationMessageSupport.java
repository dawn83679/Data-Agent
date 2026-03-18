package edu.zsc.ai.agent.tool.message;

/**
 * Shared message helpers for the write-confirmation flow.
 */
public final class WriteConfirmationMessageSupport {

    private WriteConfirmationMessageSupport() {
    }

    public static String noTokenForConversation() {
        return ToolMessageSupport.sentence(
                "No confirmation token exists for this conversation.",
                "You must call askUserConfirm first."
        );
    }

    public static String tokenPending() {
        return ToolMessageSupport.sentence(
                "A confirmation token exists, but the user has not confirmed it yet.",
                "Wait for user confirmation before executing write SQL."
        );
    }

    public static String tokenAlreadyConsumed() {
        return ToolMessageSupport.sentence(
                "The confirmation token was already used.",
                "Call askUserConfirm again for a new confirmation."
        );
    }

    public static String connectionMismatch(Long expected, Long actual) {
        return ToolMessageSupport.sentence(
                "The confirmed token expects connectionId=" + expected + ", but executeNonSelectSql received connectionId=" + actual + ".",
                "Use the same connectionId."
        );
    }

    public static String catalogMismatch(String expected, String actual) {
        return ToolMessageSupport.sentence(
                "The confirmed token expects catalog='" + expected + "', but executeNonSelectSql received catalog='" + actual + "'.",
                "Use the same catalog."
        );
    }

    public static String schemaMismatch(String expected, String actual) {
        return ToolMessageSupport.sentence(
                "The confirmed token expects schema='" + expected + "', but executeNonSelectSql received schema='" + actual + "'.",
                "Use the same schema."
        );
    }

    public static String sqlMismatch(String confirmedSql, String receivedSql) {
        return ToolMessageSupport.sentence(
                "SQL content differs from the confirmed SQL.",
                "Confirmed: '" + confirmedSql + "'.",
                "Received: '" + receivedSql + "'.",
                "You must call askUserConfirm again with the new SQL."
        );
    }

    public static String writeConfirmationFailed(String message) {
        return ToolMessageSupport.sentence(
                message,
                "Do not execute write SQL until the confirmation flow succeeds."
        );
    }
}
