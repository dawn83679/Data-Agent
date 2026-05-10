package edu.zsc.ai.agent.tool.plan.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanStep {

    @Description("步骤序号，从 1 开始。")
    private int order;

    @Description("这一步要做什么。")
    private String description;

    @Description("这一步要执行的 SQL 语句。")
    private String sql;

    @Description("这一步涉及的表或对象名。")
    private String objectName;
}
