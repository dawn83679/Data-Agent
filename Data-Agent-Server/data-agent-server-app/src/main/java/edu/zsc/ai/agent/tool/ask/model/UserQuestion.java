package edu.zsc.ai.agent.tool.ask.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestion {

    @Description("要询问用户的问题。必须清楚、具体，让用户知道你需要什么信息。")
    private String question;

    @Description("供用户选择的选项列表，建议 2 到 3 个，最多 3 个。应基于已有数据提供具体选项，例如连接、数据库、表。")
    private List<String> options;

    @Description("可选的自由输入提示。提供后会作为自定义输入框的占位提示展示，用于引导用户输入。")
    private String freeTextHint;

    @Description("是否允许多选，默认 false。true 表示多选，false 表示单选。")
    private Boolean allowMultiSelect;
}
