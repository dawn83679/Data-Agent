package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.common.enums.ai.FileExportFormatEnum;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class DocxFileExportStrategy extends AbstractTabularFileExportStrategy {

    @Override
    public String format() {
        return FileExportFormatEnum.DOCX.name();
    }

    @Override
    public FileExportArtifact export(FileExportRequest request) {
        List<String> headers = requireHeaders(request);
        List<List<Object>> rows = requireRows(request, headers.size());

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setText("Export Table");

            XWPFTable table = document.createTable(rows.size() + 1, headers.size());
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < headers.size(); i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.removeParagraph(0);
                XWPFParagraph paragraph = cell.addParagraph();
                XWPFRun run = paragraph.createRun();
                run.setBold(true);
                run.setText(headers.get(i));
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                XWPFTableRow tableRow = table.getRow(rowIndex + 1);
                List<Object> row = rows.get(rowIndex);
                for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                    XWPFTableCell cell = tableRow.getCell(colIndex);
                    cell.removeParagraph(0);
                    XWPFParagraph paragraph = cell.addParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(normalizeCell(row.get(colIndex)));
                }
            }

            document.write(output);
            return FileExportArtifact.builder()
                    .normalizedFormat(format())
                    .extension(FileExportFormatEnum.DOCX.getExtension())
                    .mimeType(FileExportFormatEnum.DOCX.getMimeType())
                    .content(output.toByteArray())
                    .rowCount(rows.size())
                    .columnCount(headers.size())
                    .preview(buildPreview(headers, rows))
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate DOCX export", e);
        }
    }
}
