package edu.zsc.ai.api.controller.ai;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.service.ai.export.FileExportService;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFileDownload;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFileStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/ai/files")
@RequiredArgsConstructor
public class ExportFileController {

    private final FileExportService exportFileService;

    @GetMapping("/{fileId}/status")
    public ExportedFileStatus status(@PathVariable @NotBlank(message = "fileId is required") String fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return exportFileService.getStatus(fileId, userId);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable @NotBlank(message = "fileId is required") String fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExportedFileDownload download = exportFileService.getDownload(fileId, userId);

        Resource resource = new FileSystemResource(download.getPath());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.getFilename(), StandardCharsets.UTF_8)
                .build();

        log.info("Downloading export file: fileId={}, userId={}, filename={}", fileId, userId, download.getFilename());
        return ResponseEntity.ok()
                .contentLength(download.getSizeBytes())
                .contentType(MediaType.parseMediaType(download.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
