package edu.zsc.ai.agent.memory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.common.constant.UserPromptTagConstant;

/**
 * Utility for normalizing runtime prompt wrappers in stored user messages.
 */
public final class MemoryUtil {

    public static final String TAG_USER_QUESTION_OPEN = UserPromptTagConstant.USER_QUESTION_OPEN;
    public static final String TAG_USER_QUESTION_CLOSE = UserPromptTagConstant.USER_QUESTION_CLOSE;
    public static final String TAG_SYSTEM_CONTEXT_OPEN = UserPromptTagConstant.SYSTEM_CONTEXT_OPEN;
    public static final String TAG_SYSTEM_REMIDER_OPEN = UserPromptTagConstant.SYSTEM_REMIDER_OPEN;
    public static final String TAG_USER_MEMORY_OPEN = UserPromptTagConstant.USER_MEMORY_OPEN;
    public static final String TAG_USER_MENTION_OPEN = UserPromptTagConstant.USER_MENTION_OPEN;

    private static final Pattern USER_QUESTION_PATTERN =
            Pattern.compile("(?ms)^\\s*" + TAG_USER_QUESTION_OPEN + "\\s*(.*?)\\s*^\\s*"
                    + TAG_USER_QUESTION_CLOSE + "\\s*$");

    private MemoryUtil() {
    }

    /**
     * Normalizes a ChatMessage by stripping injected runtime prompt wrappers from UserMessage.
     * Non-UserMessage types are returned unchanged.
     */
    public static ChatMessage normalizeUserMessage(ChatMessage message) {
        if (!(message instanceof UserMessage userMessage)) {
            return message;
        }
        if (CollectionUtils.isEmpty(userMessage.contents())
                || !userMessage.contents().stream().allMatch(messageContent -> messageContent instanceof TextContent)) {
            return message;
        }
        String content = userMessage.contents().stream()
                .filter(messageContent -> messageContent instanceof TextContent)
                .map(messageContent -> ((TextContent) messageContent).text())
                .collect(Collectors.joining("\n"));
        if (StringUtils.isBlank(content)) {
            return message;
        }
        String normalized = stripInjectedWrapper(content);
        if (normalized.equals(content)) {
            return message;
        }
        return UserMessage.from(normalized);
    }

    /**
     * Strips the runtime prompt wrapper and returns the original user question.
     */
    public static String stripInjectedWrapper(String content) {
        String trimmed = StringUtils.trimToEmpty(content);
        if (!looksLikeInjectedWrapper(trimmed)) {
            return content;
        }
        Matcher matcher = USER_QUESTION_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return content;
        }
        return StringUtils.trimToEmpty(matcher.group(1));
    }

    private static boolean looksLikeInjectedWrapper(String trimmed) {
        if (!trimmed.contains(TAG_USER_QUESTION_OPEN) || !trimmed.contains(TAG_USER_QUESTION_CLOSE)) {
            return false;
        }
        return trimmed.startsWith(TAG_SYSTEM_CONTEXT_OPEN)
                || trimmed.startsWith(TAG_SYSTEM_REMIDER_OPEN)
                || trimmed.startsWith(TAG_USER_MEMORY_OPEN)
                || trimmed.startsWith(TAG_USER_MENTION_OPEN);
    }
}
