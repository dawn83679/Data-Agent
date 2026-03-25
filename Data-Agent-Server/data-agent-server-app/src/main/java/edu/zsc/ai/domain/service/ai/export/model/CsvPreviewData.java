package edu.zsc.ai.domain.service.ai.export.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvPreviewData {

    private List<String> columns;
    private List<List<String>> rows;
    private boolean truncated;
    private int totalRowCount;
    private int totalColumnCount;
}

