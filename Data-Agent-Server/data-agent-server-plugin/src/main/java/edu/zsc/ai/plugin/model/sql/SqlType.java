package edu.zsc.ai.plugin.model.sql;

public enum SqlType {
    SELECT, INSERT, UPDATE, DELETE, MERGE,
    CREATE, ALTER, DROP, TRUNCATE,
    GRANT, REVOKE,
    SHOW, EXPLAIN, DESCRIBE, USE, SET,
    BEGIN, COMMIT, ROLLBACK,
    UNKNOWN;

    public boolean isReadOnly() {
        return switch (this) {
            case SELECT, SHOW, EXPLAIN, DESCRIBE -> true;
            default -> false;
        };
    }

    public boolean isDml() {
        return switch (this) {
            case SELECT, INSERT, UPDATE, DELETE, MERGE -> true;
            default -> false;
        };
    }

    public boolean isDdl() {
        return switch (this) {
            case CREATE, ALTER, DROP, TRUNCATE -> true;
            default -> false;
        };
    }
}
