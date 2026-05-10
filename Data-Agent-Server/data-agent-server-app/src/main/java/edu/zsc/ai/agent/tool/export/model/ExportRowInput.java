package edu.zsc.ai.agent.tool.export.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRowInput {

    @Description("本行单元格值。单元格数量必须和 headers 精确一致。")
    private List<Object> cells;
}
