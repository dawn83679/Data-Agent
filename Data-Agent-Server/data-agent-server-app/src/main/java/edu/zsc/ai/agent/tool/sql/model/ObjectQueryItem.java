package edu.zsc.ai.agent.tool.sql.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectQueryItem {

    @Description("对象类型：TABLE、VIEW、FUNCTION、PROCEDURE、TRIGGER。")
    private String objectType;

    @Description("精确对象名。")
    private String objectName;

    @Description("连接 ID。")
    private Long connectionId;

    @Description("数据库或 catalog 名称。")
    private String databaseName;

    @Description("schema 名称；数据库类型不使用 schema 时省略。")
    private String schemaName;
}
