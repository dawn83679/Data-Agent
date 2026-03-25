package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.enums.error.ErrorCodeEnum;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFileDownload;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.StoredExportFile;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ExportFileStorageService {

    static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final Path rootDirectory;
    private final Map<String, StoredExportFile> files = new ConcurrentHashMap<>();

    public ExportFileStorageService() {
        this(Path.of(System.getProperty("java.io.tmpdir"), "data-agent", "exports"));
    }

    public ExportFileStorageService(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @PostConstruct
    public void initialize() {
        recreateRootDirectory();
    }

    @PreDestroy
    public void shutdown() {
        files.clear();
        deleteRootDirectoryQuietly();
    }

    public Path resolvePath(String filename) {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new BusinessException(ResponseCode.SYSTEM_ERROR, ErrorCodeEnum.FILE_WRITE_ERROR.getMessage());
        }
        return rootDirectory.resolve(filename);
    }

    public StoredExportFile store(StoredExportFile file, byte[] content) {
        if (file == null || file.getFileId() == null || file.getPath() == null) {
            throw new BusinessException(ResponseCode.SYSTEM_ERROR, ErrorCodeEnum.FILE_WRITE_ERROR.getMessage());
        }
        try {
            Files.createDirectories(rootDirectory);
            Files.write(file.getPath(), content == null ? new byte[0] : content);
        } catch (IOException e) {
            log.error("Failed to persist export file {}", file.getFilename(), e);
            throw new BusinessException(ResponseCode.SYSTEM_ERROR, ErrorCodeEnum.FILE_WRITE_ERROR.getMessage());
        }
        files.put(file.getFileId(), file);
        return file;
    }

    public StoredExportFile store(Long ownerUserId, Long conversationId, FileExportArtifact artifact) {
        if (ownerUserId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, ErrorCodeEnum.NOT_LOGIN_ERROR.getMessage());
        }
        if (artifact == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ErrorCodeEnum.PARAMS_ERROR.getMessage());
        }

        String fileId = UUID.randomUUID().toString();
        String filename = buildFilename(ownerUserId, conversationId, fileId, artifact.getExtension());
        StoredExportFile stored = StoredExportFile.builder()
                .fileId(fileId)
                .ownerUserId(ownerUserId)
                .conversationId(conversationId)
                .uuid(fileId)
                .format(artifact.getNormalizedFormat())
                .mimeType(artifact.getMimeType())
                .filename(filename)
                .path(resolvePath(filename))
                .sizeBytes(artifact.getContent() == null ? 0 : artifact.getContent().length)
                .createdAt(System.currentTimeMillis())
                .rowCount(artifact.getRowCount())
                .columnCount(artifact.getColumnCount())
                .preview(artifact.getPreview())
                .build();
        return store(stored, artifact.getContent());
    }

    public Optional<StoredExportFile> get(String fileId) {
        return Optional.ofNullable(files.get(fileId));
    }

    public ExportedFileDownload resolveDownload(String fileId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, ErrorCodeEnum.NOT_LOGIN_ERROR.getMessage());
        }

        StoredExportFile stored = get(fileId)
                .orElseThrow(() -> new BusinessException(ResponseCode.NOT_FOUND, ErrorCodeEnum.FILE_NOT_FOUND.getMessage()));
        if (!currentUserId.equals(stored.getOwnerUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, ErrorCodeEnum.NO_AUTH_ERROR.getMessage());
        }
        if (stored.getPath() == null || !Files.exists(stored.getPath())) {
            remove(fileId);
            throw new BusinessException(ResponseCode.NOT_FOUND, ErrorCodeEnum.FILE_NOT_FOUND.getMessage());
        }

        return ExportedFileDownload.builder()
                .fileId(stored.getFileId())
                .filename(stored.getFilename())
                .mimeType(stored.getMimeType())
                .path(stored.getPath())
                .sizeBytes(stored.getSizeBytes())
                .build();
    }

    public void remove(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        StoredExportFile removed = files.remove(fileId);
        if (removed == null || removed.getPath() == null) {
            return;
        }
        try {
            Files.deleteIfExists(removed.getPath());
        } catch (IOException e) {
            log.warn("Failed to delete export file {}", removed.getPath(), e);
        }
    }

    private void recreateRootDirectory() {
        deleteRootDirectoryQuietly();
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new BusinessException(ResponseCode.SYSTEM_ERROR, ErrorCodeEnum.FILE_WRITE_ERROR.getMessage());
        }
    }

    private void deleteRootDirectoryQuietly() {
        if (!Files.exists(rootDirectory)) {
            return;
        }
        try (var paths = Files.walk(rootDirectory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete export path {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to clean export root directory {}", rootDirectory, e);
        }
    }

    private String buildFilename(Long userId, Long conversationId, String fileId, String extension) {
        String timestamp = FILE_TIMESTAMP_FORMAT.format(LocalDateTime.now());
        long safeConversationId = conversationId == null ? 0L : conversationId;
        String safeFileId = StringUtils.defaultIfBlank(fileId, UUID.randomUUID().toString());
        String safeExtension = StringUtils.defaultIfBlank(extension, "bin");
        return "%s-%d-%d-%s.%s".formatted(timestamp, userId, safeConversationId, safeFileId, safeExtension);
    }
}
