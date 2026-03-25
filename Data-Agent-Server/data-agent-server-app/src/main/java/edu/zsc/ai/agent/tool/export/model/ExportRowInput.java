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

    @Description("Cell values for this row. The number of cells must exactly match headers.")
    private List<Object> cells;
}
