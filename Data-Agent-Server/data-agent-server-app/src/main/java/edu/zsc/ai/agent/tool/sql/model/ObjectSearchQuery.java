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

    @Description("SQL 通配符模式，例如 '%order%' 或 'user_%'。")
    private String objectNamePattern;

    @Description("对象类型：TABLE、VIEW、FUNCTION、PROCEDURE、TRIGGER。省略时搜索 TABLE 和 VIEW。")
    private String objectType;

    @Description("限定到指定连接。省略时搜索所有连接。")
    private Long connectionId;

    @JsonAlias("databaseName")
    @Description("按 SQL 通配符模式过滤数据库或 catalog，例如 'app%' 或 '%prod%'。需要同时提供 connectionId。")
    private String databaseNamePattern;

    @JsonAlias("schemaName")
    @Description("按 SQL 通配符模式过滤 schema，例如 'public' 或 '%core%'。需要同时提供 connectionId 和 databaseNamePattern。")
    private String schemaNamePattern;
}
