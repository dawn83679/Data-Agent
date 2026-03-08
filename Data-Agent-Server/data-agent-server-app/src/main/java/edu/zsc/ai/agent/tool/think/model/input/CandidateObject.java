package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class CandidateObject {

    @JsonPropertyDescription("Connection ID this object belongs to.")
    private Long connectionId;

    @JsonPropertyDescription("Database (catalog) name.")
    private String databaseName;

    @JsonPropertyDescription("Schema name, if applicable.")
    private String schemaName;

    @JsonPropertyDescription("Object type: TABLE, VIEW, etc.")
    private String type;

    @JsonPropertyDescription("Object name, e.g. table name.")
    private String objectName;
}
