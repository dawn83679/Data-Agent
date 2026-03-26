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
public class StoredExportFile {

    private String fileId;
    private Long ownerUserId;
    private Long conversationId;
    private String uuid;
    private String format;
    private String mimeType;
    private String filename;
    private Path path;
    private long sizeBytes;
    private long createdAt;
    private Integer rowCount;
    private Integer columnCount;
    private Object preview;
}
