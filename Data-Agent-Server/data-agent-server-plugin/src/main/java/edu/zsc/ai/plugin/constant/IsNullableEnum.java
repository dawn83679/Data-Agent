package edu.zsc.ai.plugin.constant;

/**
 * SQL standard information_schema.IS_NULLABLE column values.
 * Used by MySQL, PostgreSQL and other databases that follow SQL standard.
 */
public enum IsNullableEnum {
    YES("YES"),
    NO("NO");

    private final String value;

    IsNullableEnum(String value) {
        this.value = value;
    }

    public static boolean isNullable(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return YES.value.equalsIgnoreCase(value.trim());
    }
}
