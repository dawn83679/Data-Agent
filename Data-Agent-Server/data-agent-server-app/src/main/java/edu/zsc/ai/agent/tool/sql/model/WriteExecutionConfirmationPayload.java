package edu.zsc.ai.agent.tool.sql.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WriteExecutionConfirmationPayload {

    private Long conversationId;
    private Long connectionId;
    private String databaseName;
    private String schemaName;
    private String sql;
    private String sqlPreview;
    private List<WriteExecutionGrantOption> availableGrantOptions;
}
