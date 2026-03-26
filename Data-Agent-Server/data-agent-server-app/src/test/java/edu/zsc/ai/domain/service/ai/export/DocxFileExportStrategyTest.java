package edu.zsc.ai.domain.service.ai.export;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocxFileExportStrategyTest {

    private final DocxFileExportStrategy strategy = new DocxFileExportStrategy();

    @Test
    void exportProducesReadableWordDocument() throws Exception {
        FileExportArtifact artifact = strategy.export(FileExportRequest.builder()
                .format("DOCX")
                .headers(List.of("name", "city"))
                .rows(List.of(List.<Object>of("Alice", "Shanghai")))
                .build());

        assertEquals("docx", artifact.getExtension());
        assertEquals("DOCX", artifact.getNormalizedFormat());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", artifact.getMimeType());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(artifact.getContent()))) {
            assertEquals("name", document.getTables().get(0).getRow(0).getCell(0).getText());
            assertEquals("Alice", document.getTables().get(0).getRow(1).getCell(0).getText());
            assertEquals("Shanghai", document.getTables().get(0).getRow(1).getCell(1).getText());
        }
    }
}
