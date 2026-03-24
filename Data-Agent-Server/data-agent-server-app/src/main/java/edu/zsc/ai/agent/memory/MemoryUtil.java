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

    private static final Pattern TASK_PATTERN =
            Pattern.compile("(?ms)^\\s*<task(?:\\s+[^>]*)?>\\s*(.*?)\\s*^\\s*</task>");
    private static final Pattern USER_QUESTION_PATTERN =
            Pattern.compile("(?ms)^\\s*<user_question(?:\\s+[^>]*)?>\\s*(.*?)\\s*^\\s*</user_question>");
    private static final Pattern MODERN_WRAPPER_PREFIX =
            Pattern.compile("^\\s*<(system_context|task|response_preferences|scope_hints|durable_facts|explicit_references)(?:\\s+[^>]*)?>");
    private static final Pattern LEGACY_WRAPPER_PREFIX =
            Pattern.compile("^\\s*<(user_memory|user_mention|system_context)(?:\\s+[^>]*)?>");

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
        Matcher taskMatcher = TASK_PATTERN.matcher(trimmed);
        if (taskMatcher.find()) {
            return normalizeTaskContent(taskMatcher.group(1));
        }
        Matcher matcher = USER_QUESTION_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return content;
        }
        return StringUtils.trimToEmpty(matcher.group(1));
    }

    private static boolean looksLikeInjectedWrapper(String trimmed) {
        boolean hasTaskWrapper = TASK_PATTERN.matcher(trimmed).find();
        boolean hasLegacyQuestionWrapper = USER_QUESTION_PATTERN.matcher(trimmed).find();
        if (!hasTaskWrapper && !hasLegacyQuestionWrapper) {
            return false;
        }
        return MODERN_WRAPPER_PREFIX.matcher(trimmed).find()
                || LEGACY_WRAPPER_PREFIX.matcher(trimmed).find();
    }

    private static String normalizeTaskContent(String taskContent) {
        String normalized = StringUtils.trimToEmpty(taskContent);
        normalized = normalized.replaceFirst("(?m)^\\s*(Current task:|当前任务：)\\s*\\R?", "");
        normalized = normalized.replaceFirst("(?m)^\\s*-\\s*", "");
        return StringUtils.trimToEmpty(normalized);
    }
}
