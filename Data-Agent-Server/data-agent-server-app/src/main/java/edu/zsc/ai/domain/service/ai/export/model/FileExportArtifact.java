package edu.zsc.ai.domain.service.ai.export.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileExportArtifact {

    private String normalizedFormat;
    private String extension;
    private String mimeType;
    private byte[] content;
    private Integer rowCount;
    private Integer columnCount;
    private Object preview;
}
