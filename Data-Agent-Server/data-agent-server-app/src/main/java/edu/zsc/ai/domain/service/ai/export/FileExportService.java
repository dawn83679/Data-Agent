package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFileDownload;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFilePayload;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFileStatus;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import edu.zsc.ai.domain.service.ai.export.model.StoredExportFile;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileExportService {

    private final FileExportStrategyResolver strategyResolver;
    private final ExportFileStorageService storageService;

    public ExportedFilePayload export(FileExportRequest request) {
        validateRequest(request);

        FileExportStrategy strategy = strategyResolver.resolve(request.getFormat());
        FileExportArtifact artifact = strategy.export(request);
        StoredExportFile stored = storageService.store(request.getUserId(), request.getConversationId(), artifact);
        return toPayload(stored);
    }

    public ExportedFileDownload getDownload(String fileId, Long currentUserId) {
        return storageService.resolveDownload(fileId, currentUserId);
    }

    public ExportedFileStatus getStatus(String fileId, Long currentUserId) {
        return storageService.resolveStatus(fileId, currentUserId);
    }

    private ExportedFilePayload toPayload(StoredExportFile stored) {
        return ExportedFilePayload.builder()
                .fileId(stored.getFileId())
                .filename(stored.getFilename())
                .format(stored.getFormat())
                .mimeType(stored.getMimeType())
                .sizeBytes(stored.getSizeBytes())
                .downloadPath("/api/ai/files/" + stored.getFileId())
                .createdAt(stored.getCreatedAt())
                .rowCount(stored.getRowCount())
                .columnCount(stored.getColumnCount())
                .preview(stored.getPreview())
                .build();
    }

    private void validateRequest(FileExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("export request is required");
        }
        if (request.getFormat() == null || request.getFormat().isBlank()) {
            throw new IllegalArgumentException("format is required");
        }
        if (request.getUserId() == null) {
            throw BusinessException.unauthorized();
        }
        if (CollectionUtils.isEmpty(request.getHeaders())) {
            throw new IllegalArgumentException("headers is required and must not be empty");
        }

        List<List<Object>> rows = request.getRows();
        if (rows == null) {
            return;
        }
        int expectedSize = request.getHeaders().size();
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row == null) {
                throw new IllegalArgumentException("rows[" + i + "] must not be null");
            }
            if (row.size() != expectedSize) {
                throw new IllegalArgumentException(
                        "rows[" + i + "] column count " + row.size() + " does not match headers size " + expectedSize
                );
            }
        }
    }
}
