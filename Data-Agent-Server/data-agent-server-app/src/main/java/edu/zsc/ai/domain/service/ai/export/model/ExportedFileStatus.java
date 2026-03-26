package edu.zsc.ai.domain.service.ai.export.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportedFileStatus {

    private String fileId;
    private boolean exists;
    private boolean available;
    private long sizeBytes;
}
