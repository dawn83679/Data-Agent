package edu.zsc.ai.domain.service.ai.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMaintenanceReport {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    private Integer activeMemoryCount;

    private Integer archivedMemoryCount;

    private Integer hiddenMemoryCount;

    private Integer expiredActiveMemoryCount;

    private Integer duplicateActiveMemoryCount;

    private Integer processedArchivedCount;

    private Integer processedHiddenCount;
}
