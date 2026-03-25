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
public class FileExportRequest {

    private String format;
    private String filename;
    private List<String> headers;
    private List<List<Object>> rows;
    private Long userId;
    private Long conversationId;
}

