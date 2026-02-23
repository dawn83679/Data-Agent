package edu.zsc.ai.plugin.capability;

public class MysqlIdentifierEscaper implements SqlIdentifierEscaper {

    private static final MysqlIdentifierEscaper INSTANCE = new MysqlIdentifierEscaper();

    public static MysqlIdentifierEscaper getInstance() {
        return INSTANCE;
    }

    private MysqlIdentifierEscaper() {
    }
}
