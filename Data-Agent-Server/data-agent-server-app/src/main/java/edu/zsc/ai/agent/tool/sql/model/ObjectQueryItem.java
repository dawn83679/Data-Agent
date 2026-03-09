package edu.zsc.ai.agent.tool.sql.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectQueryItem {

    @Description("Object type: TABLE, VIEW, FUNCTION, PROCEDURE, TRIGGER")
    private String objectType;

    @Description("Exact object name")
    private String objectName;

    @Description("Connection id")
    private Long connectionId;

    @Description("Database (catalog) name")
    private String databaseName;

    @Description("Schema name; omit if not used")
    private String schemaName;
}
