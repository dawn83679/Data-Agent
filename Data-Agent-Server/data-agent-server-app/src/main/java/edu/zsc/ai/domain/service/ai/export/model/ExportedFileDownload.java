package edu.zsc.ai.domain.service.ai.export.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportedFileDownload {

    private String fileId;
    private String filename;
    private String mimeType;
    private Path path;
    private long sizeBytes;
}

