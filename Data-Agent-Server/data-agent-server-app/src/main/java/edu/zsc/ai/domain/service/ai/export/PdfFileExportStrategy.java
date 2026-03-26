package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.common.enums.ai.FileExportFormatEnum;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfFileExportStrategy extends AbstractTabularFileExportStrategy {
    private static final float MARGIN = 36F;
    private static final float FONT_SIZE = 9F;
    private static final float HEADER_FONT_SIZE = 9.5F;
    private static final float CELL_PADDING = 3F;
    private static final float LINE_HEIGHT = 12F;
    private static final PDRectangle LANDSCAPE_A4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final List<String> FONT_CANDIDATES = List.of(
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/System/Library/Fonts/Hiragino Sans GB.ttc",
            "/System/Library/Fonts/STHeiti Medium.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/System/Library/Fonts/Supplemental/Songti.ttc"
    );

    @Override
    public String format() {
        return FileExportFormatEnum.PDF.name();
    }

    @Override
    public FileExportArtifact export(FileExportRequest request) {
        List<String> headers = requireHeaders(request);
        List<List<Object>> rows = requireRows(request, headers.size());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont regularFont = loadFont(document);
            PDFont headerFont = regularFont;

            PDPage page = new PDPage(LANDSCAPE_A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);

            float tableWidth = page.getMediaBox().getWidth() - MARGIN * 2;
            float columnWidth = tableWidth / headers.size();
            float y = page.getMediaBox().getHeight() - MARGIN;
            float bottom = MARGIN;

            List<List<String>> allRows = new ArrayList<>();
            allRows.add(headers);
            for (List<Object> row : rows) {
                List<String> normalized = new ArrayList<>(row.size());
                for (Object cell : row) {
                    normalized.add(normalizeCell(cell));
                }
                allRows.add(normalized);
            }

            List<String> headerValues = allRows.get(0);
            float headerHeight = drawRow(content, page, headerValues, y, columnWidth, headerFont, HEADER_FONT_SIZE, true);
            y -= headerHeight;

            for (int i = 1; i < allRows.size(); i++) {
                List<String> row = allRows.get(i);
                float rowHeight = estimateRowHeight(row, columnWidth, regularFont, FONT_SIZE);
                if (y - rowHeight < bottom) {
                    content.close();
                    page = new PDPage(LANDSCAPE_A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - MARGIN;
                    headerHeight = drawRow(content, page, headerValues, y, columnWidth, headerFont, HEADER_FONT_SIZE, true);
                    y -= headerHeight;
                }
                rowHeight = drawRow(content, page, row, y, columnWidth, regularFont, FONT_SIZE, false);
                y -= rowHeight;
            }

            content.close();
            document.save(output);
            return FileExportArtifact.builder()
                    .normalizedFormat(format())
                    .extension(FileExportFormatEnum.PDF.getExtension())
                    .mimeType(FileExportFormatEnum.PDF.getMimeType())
                    .content(output.toByteArray())
                    .rowCount(rows.size())
                    .columnCount(headers.size())
                    .preview(buildPreview(headers, rows))
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF export", e);
        }
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        for (String candidate : FONT_CANDIDATES) {
            File file = new File(candidate);
            if (file.exists() && file.isFile()) {
                try {
                    return PDType0Font.load(document, file);
                } catch (IOException ignored) {
                    // Try the next font candidate.
                }
            }
        }
        return PDType1Font.HELVETICA;
    }

    private float estimateRowHeight(List<String> values, float columnWidth, PDFont font, float fontSize) throws IOException {
        int maxLines = 1;
        for (String value : values) {
            int lineCount = wrapText(value, font, fontSize, columnWidth - CELL_PADDING * 2).size();
            maxLines = Math.max(maxLines, lineCount);
        }
        return maxLines * LINE_HEIGHT + CELL_PADDING * 2;
    }

    private float drawRow(PDPageContentStream content,
                          PDPage page,
                          List<String> values,
                          float y,
                          float columnWidth,
                          PDFont font,
                          float fontSize,
                          boolean header) throws IOException {
        float rowHeight = estimateRowHeight(values, columnWidth, font, fontSize);
        float x = MARGIN;

        for (String value : values) {
            content.addRect(x, y - rowHeight, columnWidth, rowHeight);
            if (header) {
                content.setNonStrokingColor(235, 238, 242);
                content.fillAndStroke();
                content.setNonStrokingColor(0, 0, 0);
            } else {
                content.stroke();
            }

            List<String> wrapped = wrapText(value, font, fontSize, columnWidth - CELL_PADDING * 2);
            float textY = y - CELL_PADDING - fontSize;
            for (String line : wrapped) {
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(x + CELL_PADDING, textY);
                content.showText(line);
                content.endText();
                textY -= LINE_HEIGHT;
            }
            x += columnWidth;
        }
        return rowHeight;
    }

    private List<String> wrapText(String value, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String normalized = drawableText(value == null ? "" : value, font);
        if (normalized.isEmpty()) {
            lines.add("");
            return lines;
        }
        for (String paragraph : normalized.split("\\R", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char ch = paragraph.charAt(i);
                String candidate = current.toString() + ch;
                float width = font.getStringWidth(candidate) / 1000F * fontSize;
                if (width > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(ch);
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private String drawableText(String value, PDFont font) {
        if (!(font instanceof PDType1Font) || value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder sanitized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            sanitized.append(ch <= 0x00FF ? ch : '?');
        }
        return sanitized.toString();
    }
}
