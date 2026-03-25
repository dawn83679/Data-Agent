package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFileDownload;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFilePayload;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileExportServiceTest {

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
    void export_returnsStructuredPayloadAndDownloadForOwner() {
        FileExportService service = createService();

        ExportedFilePayload payload = service.export(FileExportRequest.builder()
                .format("CSV")
                .headers(List.of("name", "age"))
                .rows(List.of(List.<Object>of("Alice", 18)))
                .userId(42L)
                .conversationId(7L)
                .build());

        assertNotNull(payload.getFileId());
        assertEquals("CSV", payload.getFormat());
        assertEquals("/api/ai/files/" + payload.getFileId(), payload.getDownloadPath());
        assertTrue(payload.getFilename().matches("\\d{17}-42-7-[0-9a-f\\-]{36}\\.csv"));

        ExportedFileDownload download = service.getDownload(payload.getFileId(), 42L);
        assertEquals(payload.getFilename(), download.getFilename());
    }

    @Test
    void export_rejectsUnsupportedFormat() {
        FileExportService service = createService();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.export(FileExportRequest.builder()
                        .format("PDF")
                        .headers(List.of("name"))
                        .rows(List.of(List.<Object>of("Alice")))
                        .userId(1L)
                        .conversationId(2L)
                        .build())
        );

        assertTrue(exception.getMessage().contains("Unsupported format"));
    }

    @Test
    void getDownload_rejectsNonOwner() {
        FileExportService service = createService();
        ExportedFilePayload payload = service.export(FileExportRequest.builder()
                .format("CSV")
                .headers(List.of("name"))
                .rows(List.of(List.<Object>of("Alice")))
                .userId(2L)
                .conversationId(3L)
                .build());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getDownload(payload.getFileId(), 100L)
        );
        assertEquals(403, exception.getCode());
    }

    private FileExportService createService() {
        storageService = new ExportFileStorageService(tempDir.resolve("exports"));
        storageService.initialize();
        return new FileExportService(new FileExportStrategyResolver(List.of(new CsvFileExportStrategy())), storageService);
    }
}
