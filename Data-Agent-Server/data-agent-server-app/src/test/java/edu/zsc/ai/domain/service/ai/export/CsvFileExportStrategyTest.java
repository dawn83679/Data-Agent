package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.domain.service.ai.export.model.CsvPreviewData;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvFileExportStrategyTest {

    private final CsvFileExportStrategy strategy = new CsvFileExportStrategy();

    @Test
    void exportProducesUtf8BomAndEscapedCsvCells() {
        FileExportArtifact artifact = strategy.export(FileExportRequest.builder()
                .format("CSV")
                .headers(List.of("name", "note"))
                .rows(List.of(
                        List.<Object>of("Alice", "hello,world"),
                        List.<Object>of("Bob", "say \"hi\""),
                        List.<Object>of("Chen", "line1\nline2")
                ))
                .build());

        String content = new String(artifact.getContent(), StandardCharsets.UTF_8);

        assertTrue(content.startsWith("\uFEFFname,note\r\n"));
        assertTrue(content.contains("Alice,\"hello,world\""));
        assertTrue(content.contains("Bob,\"say \"\"hi\"\"\""));
        assertTrue(content.contains("Chen,\"line1\nline2\""));
        assertEquals("csv", artifact.getExtension());
        assertEquals("text/csv;charset=utf-8", artifact.getMimeType());
    }

    @Test
    void exportBuildsPreviewAndMarksTruncation() {
        List<List<Object>> rows = IntStream.range(0, 11)
                .mapToObj(i -> List.<Object>of("row-" + i, i))
                .toList();

        FileExportArtifact artifact = strategy.export(FileExportRequest.builder()
                .format("CSV")
                .headers(List.of("name", "index"))
                .rows(rows)
                .build());

        CsvPreviewData preview = (CsvPreviewData) artifact.getPreview();

        assertEquals(10, preview.getRows().size());
        assertTrue(preview.isTruncated());
        assertEquals(11, preview.getTotalRowCount());
        assertEquals(2, preview.getTotalColumnCount());
    }
}
