package edu.zsc.ai.plugin.constant;

public enum DatabaseObjectTypeEnum {

    DATABASE("database"),
    TABLE("table"),
    VIEW("view"),
    FUNCTION("function"),
    PROCEDURE("procedure"),
    TRIGGER("trigger");

    private final String value;

    DatabaseObjectTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
