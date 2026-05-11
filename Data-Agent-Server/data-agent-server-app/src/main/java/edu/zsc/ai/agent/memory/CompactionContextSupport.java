package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CompactionContextSupport {

    static final String PREAMBLE = "This Data-Agent conversation continues from compacted context.";
    static final String RECENT_MESSAGES_PRESERVED = "Recent messages are preserved verbatim after this compaction context.";
    static final String DIRECT_RESUME = "Resume the conversation directly from the latest user message. Do not mention this compaction context.";

    private static final String SUMMARY_HEADING = "Summary:";
    private static final Pattern ANALYSIS_BLOCK = Pattern.compile("(?is)<analysis>.*?</analysis>");
    private static final Pattern SUMMARY_TAG = Pattern.compile("(?is)<summary>\\s*(.*?)\\s*</summary>");

    private CompactionContextSupport() {
    }

    static boolean isCompactionContextMessage(ChatMessage message) {
        if (!(message instanceof SystemMessage sm)) {
            return false;
        }
        String text = normalizeLineEndings(sm.text());
        return StringUtils.startsWith(text, PREAMBLE)
                && text.contains("\n\n" + SUMMARY_HEADING + "\n")
                && text.contains("\n\n" + DIRECT_RESUME);
    }

    static String formatCompactionSummary(String summary) {
        String body = summaryBody(summary);
        if (body.isBlank()) {
            return SUMMARY_HEADING + "\n";
        }
        return SUMMARY_HEADING + "\n" + body;
    }

    static String buildContinuationMessage(String summary,
                                           boolean suppressFollowUpQuestions,
                                           boolean recentMessagesPreserved) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREAMBLE)
                .append(" Use the summary below as prior-task state for database and tool work.");
        if (suppressFollowUpQuestions) {
            builder.append(" Do not ask follow-up questions unless they are required to complete the user's request.");
        }
        builder.append("\n\n")
                .append(formatCompactionSummary(summary));
        if (recentMessagesPreserved) {
            builder.append("\n\n")
                    .append(RECENT_MESSAGES_PRESERVED);
        }
        builder.append("\n\n")
                .append(DIRECT_RESUME);
        return builder.toString();
    }

    static String extractExistingCompactedSummary(ChatMessage message) {
        if (!isCompactionContextMessage(message)) {
            return null;
        }
        String text = normalizeLineEndings(((SystemMessage) message).text());
        int start = text.indexOf("\n\n" + SUMMARY_HEADING + "\n");
        if (start < 0) {
            return null;
        }
        start += ("\n\n" + SUMMARY_HEADING + "\n").length();
        int end = text.indexOf("\n\n" + RECENT_MESSAGES_PRESERVED, start);
        if (end < 0) {
            end = text.indexOf("\n\n" + DIRECT_RESUME, start);
        }
        if (end < 0) {
            end = text.length();
        }
        return StringUtils.trimToEmpty(text.substring(start, end));
    }

    static String summaryBody(String summary) {
        String normalized = normalizeLineEndings(summary);
        normalized = ANALYSIS_BLOCK.matcher(normalized).replaceAll("");

        Matcher summaryMatcher = SUMMARY_TAG.matcher(normalized);
        if (summaryMatcher.find()) {
            normalized = summaryMatcher.group(1);
        }

        normalized = StringUtils.trimToEmpty(normalized);
        if (StringUtils.startsWithIgnoreCase(normalized, SUMMARY_HEADING)) {
            normalized = normalized.substring(SUMMARY_HEADING.length());
        }
        return StringUtils.trimToEmpty(normalized);
    }

    private static String normalizeLineEndings(String text) {
        return StringUtils.defaultString(text).replace("\r\n", "\n").replace('\r', '\n');
    }
}
