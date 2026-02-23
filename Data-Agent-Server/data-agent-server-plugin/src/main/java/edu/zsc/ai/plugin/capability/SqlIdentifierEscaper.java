package edu.zsc.ai.plugin.capability;

import org.apache.commons.lang3.StringUtils;

public interface SqlIdentifierEscaper {

    String BACKTICK = "`";
    String DOUBLE_BACKTICK = "``";
    String SINGLE_QUOTE = "'";
    String DOUBLE_SINGLE_QUOTE = "''";
    String BACKSLASH = "\\";
    String PERCENT = "%";
    String UNDERSCORE = "_";
    String ESCAPED_BACKSLASH = "\\\\";
    String ESCAPED_PERCENT = "\\%";
    String ESCAPED_UNDERSCORE = "\\_";
    char UNDERSCORE_CHAR = '_';

    default String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        if (StringUtils.contains(identifier, BACKTICK)) {
            return StringUtils.replace(identifier, BACKTICK, DOUBLE_BACKTICK);
        }
        return identifier;
    }

    default boolean needsQuoting(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return false;
        }

        if (Character.isDigit(identifier.charAt(0))) {
            return true;
        }

        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != UNDERSCORE_CHAR) {
                return true;
            }
        }

        return false;
    }

    default String quoteIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return identifier;
        }

        String escaped = escapeIdentifier(identifier);

        if (needsQuoting(identifier)) {
            return BACKTICK + escaped + BACKTICK;
        }

        return escaped;
    }

    default String escapeStringLiteral(String value) {
        if (value == null) {
            return null;
        }

        boolean needsEscape = StringUtils.containsAny(value, BACKSLASH, SINGLE_QUOTE);
        if (!needsEscape) {
            return value;
        }

        return StringUtils.replaceEach(value,
                new String[]{BACKSLASH, SINGLE_QUOTE},
                new String[]{ESCAPED_BACKSLASH, DOUBLE_SINGLE_QUOTE});
    }

    default String escapeLikePattern(String pattern) {
        if (pattern == null) {
            return null;
        }

        boolean needsEscape = StringUtils.containsAny(pattern, BACKSLASH, PERCENT, UNDERSCORE);
        if (!needsEscape) {
            return pattern;
        }

        return StringUtils.replaceEach(pattern,
                new String[]{BACKSLASH, PERCENT, UNDERSCORE},
                new String[]{ESCAPED_BACKSLASH, ESCAPED_PERCENT, ESCAPED_UNDERSCORE});
    }
}
