package edu.zsc.ai.agent.tool.sql.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectSearchQuery {

    @Description("SQL wildcard pattern, e.g. '%order%' or 'user_%'")
    private String objectNamePattern;

    @Description("Object type: TABLE, VIEW, FUNCTION, PROCEDURE, TRIGGER. Omit to search TABLE + VIEW.")
    private String objectType;

    @Description("Filter to a specific connection. Omit to search all connections.")
    private Long connectionId;

    @Description("Filter to a specific database/catalog. Requires connectionId.")
    private String databaseName;

    @Description("Filter to a specific schema. Requires connectionId + databaseName.")
    private String schemaName;
}
