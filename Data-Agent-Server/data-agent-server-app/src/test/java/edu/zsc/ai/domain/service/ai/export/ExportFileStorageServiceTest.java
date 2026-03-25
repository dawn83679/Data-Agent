package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFileDownload;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.StoredExportFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private ExportFileStorageService storageService;

    @AfterEach
    void tearDown() {
        if (storageService != null) {
            storageService.shutdown();
        }
    }

    @Test
    void store_generatesExpectedFilenameAndSupportsOwnerDownload() {
        storageService = new ExportFileStorageService(tempDir);
        storageService.initialize();

        FileExportArtifact artifact = FileExportArtifact.builder()
                .normalizedFormat("CSV")
                .extension("csv")
                .mimeType("text/csv;charset=utf-8")
                .content("a,b\r\n1,2".getBytes(StandardCharsets.UTF_8))
                .rowCount(1)
                .columnCount(2)
                .build();

        StoredExportFile stored = storageService.store(9L, 77L, artifact);

        assertNotNull(stored.getFileId());
        assertTrue(stored.getFilename().matches("\\d{17}-9-77-[0-9a-f\\-]{36}\\.csv"));
        assertTrue(Files.exists(stored.getPath()));

        ExportedFileDownload download = storageService.resolveDownload(stored.getFileId(), 9L);
        assertEquals(stored.getFilename(), download.getFilename());
    }

    @Test
    void resolveDownload_rejectsNonOwner() {
        storageService = new ExportFileStorageService(tempDir);
        storageService.initialize();

        FileExportArtifact artifact = FileExportArtifact.builder()
                .normalizedFormat("CSV")
                .extension("csv")
                .mimeType("text/csv;charset=utf-8")
                .content("x".getBytes(StandardCharsets.UTF_8))
                .build();
        StoredExportFile stored = storageService.store(3L, 4L, artifact);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> storageService.resolveDownload(stored.getFileId(), 99L)
        );

        assertEquals(403, exception.getCode());
    }

    @Test
    void shutdown_cleansDirectory() {
        storageService = new ExportFileStorageService(tempDir);
        storageService.initialize();

        FileExportArtifact artifact = FileExportArtifact.builder()
                .normalizedFormat("CSV")
                .extension("csv")
                .mimeType("text/csv;charset=utf-8")
                .content("x".getBytes(StandardCharsets.UTF_8))
                .build();
        storageService.store(1L, null, artifact);

        storageService.shutdown();
        assertFalse(Files.exists(tempDir));
    }
}

