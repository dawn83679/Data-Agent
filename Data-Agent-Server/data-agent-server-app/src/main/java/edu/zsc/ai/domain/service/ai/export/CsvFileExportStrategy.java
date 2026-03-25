package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.domain.service.ai.export.model.CsvPreviewData;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvFileExportStrategy implements FileExportStrategy {

    private static final String FORMAT = "CSV";
    private static final byte[] UTF8_BOM = new byte[]{
            (byte) 0xEF,
            (byte) 0xBB,
            (byte) 0xBF
    };
    private static final int PREVIEW_ROW_LIMIT = 10;

    @Override
    public String format() {
        return FORMAT;
    }

    @Override
    public FileExportArtifact export(FileExportRequest request) {
        validate(request);

        List<String> headers = request.getHeaders();
        List<List<Object>> rows = request.getRows() == null ? List.of() : request.getRows();
        int columnCount = headers.size();

        List<String> lines = new ArrayList<>(rows.size() + 1);
        lines.add(joinCsvRow(headers.stream().map(this::normalizeCell).toList()));
        for (List<Object> row : rows) {
            List<String> normalizedRow = new ArrayList<>(columnCount);
            for (Object value : row) {
                normalizedRow.add(normalizeCell(value));
            }
            lines.add(joinCsvRow(normalizedRow));
        }

        byte[] csvBytes = String.join("\r\n", lines).getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[UTF8_BOM.length + csvBytes.length];
        System.arraycopy(UTF8_BOM, 0, content, 0, UTF8_BOM.length);
        System.arraycopy(csvBytes, 0, content, UTF8_BOM.length, csvBytes.length);

        List<List<String>> previewRows = new ArrayList<>();
        int previewSize = Math.min(rows.size(), PREVIEW_ROW_LIMIT);
        for (int i = 0; i < previewSize; i++) {
            List<Object> row = rows.get(i);
            List<String> previewRow = new ArrayList<>(columnCount);
            for (Object value : row) {
                previewRow.add(normalizeCell(value));
            }
            previewRows.add(previewRow);
        }

        CsvPreviewData preview = CsvPreviewData.builder()
                .columns(List.copyOf(headers))
                .rows(previewRows)
                .truncated(rows.size() > PREVIEW_ROW_LIMIT)
                .totalRowCount(rows.size())
                .totalColumnCount(columnCount)
                .build();

        return FileExportArtifact.builder()
                .normalizedFormat(FORMAT)
                .extension("csv")
                .mimeType("text/csv;charset=utf-8")
                .content(content)
                .rowCount(rows.size())
                .columnCount(columnCount)
                .preview(preview)
                .build();
    }

    private void validate(FileExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("export request is required");
        }
        if (CollectionUtils.isEmpty(request.getHeaders())) {
            throw new IllegalArgumentException("headers must contain at least one column");
        }
        for (int i = 0; i < request.getHeaders().size(); i++) {
            String header = request.getHeaders().get(i);
            if (StringUtils.isBlank(header)) {
                throw new IllegalArgumentException("headers[" + i + "] must not be blank");
            }
        }

        List<List<Object>> rows = request.getRows();
        if (rows == null) {
            return;
        }

        int expectedSize = request.getHeaders().size();
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row == null) {
                throw new IllegalArgumentException("rows[" + i + "] must not be null");
            }
            if (row.size() != expectedSize) {
                throw new IllegalArgumentException("rows[" + i + "] must contain exactly " + expectedSize + " cells");
            }
        }
    }

    private String joinCsvRow(List<String> values) {
        return values.stream()
                .map(this::escapeCsvCell)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String normalizeCell(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeCsvCell(String value) {
        String normalized = value == null ? "" : value;
        boolean needsQuotes = normalized.contains(",")
                || normalized.contains("\"")
                || normalized.contains("\r")
                || normalized.contains("\n");
        if (!needsQuotes) {
            return normalized;
        }
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }
}
