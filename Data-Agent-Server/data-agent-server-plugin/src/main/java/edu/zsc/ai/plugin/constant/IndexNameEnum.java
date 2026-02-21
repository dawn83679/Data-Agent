package edu.zsc.ai.plugin.constant;

public enum IndexNameEnum {

    PRIMARY("PRIMARY");

    private final String value;

    IndexNameEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isPrimaryKeyIndex(String indexName) {
        return indexName != null && PRIMARY.value.equalsIgnoreCase(indexName);
    }
}
