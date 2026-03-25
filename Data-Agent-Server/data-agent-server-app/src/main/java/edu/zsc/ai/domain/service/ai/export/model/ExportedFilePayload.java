package edu.zsc.ai.domain.service.ai.export.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportedFilePayload {

    private String fileId;
    private String filename;
    private String format;
    private String mimeType;
    private long sizeBytes;
    private String downloadPath;
    private long createdAt;
    private Integer rowCount;
    private Integer columnCount;
    private Object preview;
}

