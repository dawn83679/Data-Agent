package edu.zsc.ai.plugin.value;

import java.sql.SQLException;

/**
 * Value processor interface for handling data source-specific type conversions.
 * This interface separates type handling logic from data access logic.
 *
 * <p>This interface supports both JDBC and non-JDBC data sources through
 * a generic ValueContext abstraction.
 *
 * <p>Each data source type (JDBC, MongoDB, Redis, etc.) should provide
 * its own implementation to handle special types and conversions.
 *
 * @author hhz
 */
public interface ValueProcessor {



    /**
     * @param context the JDBC context containing ResultSet and column information
     * @return the extracted and converted value, may be null
     */
    Object getJdbcValue(JdbcValueContext context) throws SQLException;


}
