package edu.zsc.ai.agent.tool.message;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public final class ToolMessageSupport {

    public static final String DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES =
            "用户回复前不要继续对象发现。";

    public static final String DO_NOT_PROCEED_WITH_DISCOVERY_PLANNING_OR_EXECUTION_UNTIL_CONNECTION_IS_AVAILABLE =
            "连接可用前不要继续发现、规划或执行。";

    public static final String IF_THE_TARGET_IS_STILL_UNCLEAR_ASK_THE_USER_BEFORE_PROCEEDING =
            "如果目标仍不清楚，继续前先询问用户。";

    private ToolMessageSupport() {
    }

    public static String askUserWhether(String action) {
        return "询问用户是否要" + action + "。";
    }

    public static String continueOnlyWith(String subject) {
        return "只基于" + subject + "继续。";
    }

    public static String waitForUserReply(String action) {
        return action + "前等待用户回复。";
    }

    public static String sentence(String... parts) {
        return Arrays.stream(parts)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
