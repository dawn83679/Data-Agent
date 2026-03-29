package edu.zsc.ai.agent.tool.sql.model;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonAlias("databaseName")
    @Description("Filter databases/catalogs by SQL wildcard pattern, e.g. 'app%' or '%prod%'. Requires connectionId.")
    private String databaseNamePattern;

    @JsonAlias("schemaName")
    @Description("Filter schemas by SQL wildcard pattern, e.g. 'public' or '%core%'. Requires connectionId + databaseNamePattern.")
    private String schemaNamePattern;
}
