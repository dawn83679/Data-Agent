package edu.zsc.ai.domain.service.ai.export;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XlsxFileExportStrategyTest {

    private final XlsxFileExportStrategy strategy = new XlsxFileExportStrategy();

    @Test
    void exportProducesReadableWorkbook() throws Exception {
        FileExportArtifact artifact = strategy.export(FileExportRequest.builder()
                .format("XLSX")
                .headers(List.of("name", "age"))
                .rows(List.of(List.<Object>of("Alice", 18)))
                .build());

        assertEquals("xlsx", artifact.getExtension());
        assertEquals("XLSX", artifact.getNormalizedFormat());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", artifact.getMimeType());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(artifact.getContent()))) {
            assertEquals("name", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Alice", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("18", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
    }
}
