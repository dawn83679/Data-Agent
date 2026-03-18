package edu.zsc.ai.agent.tool.message;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Shared guidance-oriented message fragments for tool results.
 */
public final class ToolMessageSupport {

    public static final String DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES =
            "Do not continue object discovery until the user replies.";

    public static final String DO_NOT_PROCEED_WITH_DISCOVERY_PLANNING_OR_EXECUTION_UNTIL_CONNECTION_IS_AVAILABLE =
            "Do not proceed with discovery, planning, or execution until a connection is available.";

    public static final String IF_THE_TARGET_IS_STILL_UNCLEAR_ASK_THE_USER_BEFORE_PROCEEDING =
            "If the target is still unclear, ask the user before proceeding.";

    private ToolMessageSupport() {
    }

    public static String askUserWhether(String action) {
        return "Ask the user whether to " + action + ".";
    }

    public static String continueOnlyWith(String subject) {
        return "Continue only with " + subject + ".";
    }

    public static String waitForUserReply(String action) {
        return "Wait for the user's reply before " + action + ".";
    }

    public static String sentence(String... parts) {
        return Arrays.stream(parts)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
