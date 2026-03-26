package edu.zsc.ai.domain.service.ai.export;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfFileExportStrategyTest {

    private final PdfFileExportStrategy strategy = new PdfFileExportStrategy();

    @Test
    void exportProducesReadablePdf() throws Exception {
        FileExportArtifact artifact = strategy.export(FileExportRequest.builder()
                .format("PDF")
                .headers(List.of("name", "city"))
                .rows(List.of(List.<Object>of("Alice", "Shanghai")))
                .build());

        assertEquals("pdf", artifact.getExtension());
        assertEquals("PDF", artifact.getNormalizedFormat());
        assertEquals("application/pdf", artifact.getMimeType());
        assertTrue(artifact.getContent().length > 0);

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(artifact.getContent()))) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("name"));
            assertTrue(text.contains("Alice"));
            assertTrue(text.contains("Shanghai"));
        }
    }
}
