package edu.zsc.ai.plugin.mysql.constant;

/**
 * Column names for MySQL information_schema.ROUTINES and PARAMETERS result sets.
 */
public final class MysqlRoutineConstants {

    // ROUTINES columns
    public static final String ROUTINE_SCHEMA = "ROUTINE_SCHEMA";
    public static final String ROUTINE_NAME = "ROUTINE_NAME";
    public static final String ROUTINE_TYPE = "ROUTINE_TYPE";
    public static final String DTD_IDENTIFIER = "DTD_IDENTIFIER";
    public static final String SPECIFIC_NAME = "SPECIFIC_NAME";

    // PARAMETERS columns
    public static final String SPECIFIC_SCHEMA = "SPECIFIC_SCHEMA";
    public static final String PARAMETER_NAME = "PARAMETER_NAME";
    public static final String ORDINAL_POSITION = "ORDINAL_POSITION";

    // Routine type values
    public static final String ROUTINE_TYPE_FUNCTION = "FUNCTION";
    public static final String ROUTINE_TYPE_PROCEDURE = "PROCEDURE";

    private MysqlRoutineConstants() {
    }
}
