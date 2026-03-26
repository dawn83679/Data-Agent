package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.domain.service.ai.export.model.CsvPreviewData;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractTabularFileExportStrategy implements FileExportStrategy {

    protected static final int PREVIEW_ROW_LIMIT = 10;

    protected List<String> requireHeaders(FileExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("export request is required");
        }
        if (CollectionUtils.isEmpty(request.getHeaders())) {
            throw new IllegalArgumentException("headers must contain at least one column");
        }
        for (int i = 0; i < request.getHeaders().size(); i++) {
            if (StringUtils.isBlank(request.getHeaders().get(i))) {
                throw new IllegalArgumentException("headers[" + i + "] must not be blank");
            }
        }
        return request.getHeaders();
    }

    protected List<List<Object>> requireRows(FileExportRequest request, int expectedSize) {
        List<List<Object>> rows = request.getRows() == null ? List.of() : request.getRows();
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row == null) {
                throw new IllegalArgumentException("rows[" + i + "] must not be null");
            }
            if (row.size() != expectedSize) {
                throw new IllegalArgumentException("rows[" + i + "] must contain exactly " + expectedSize + " cells");
            }
        }
        return rows;
    }

    protected String normalizeCell(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    protected CsvPreviewData buildPreview(List<String> headers, List<List<Object>> rows) {
        List<List<String>> previewRows = new ArrayList<>();
        int previewSize = Math.min(rows.size(), PREVIEW_ROW_LIMIT);
        for (int i = 0; i < previewSize; i++) {
            List<Object> row = rows.get(i);
            List<String> previewRow = new ArrayList<>(headers.size());
            for (Object value : row) {
                previewRow.add(normalizeCell(value));
            }
            previewRows.add(previewRow);
        }
        return CsvPreviewData.builder()
                .columns(List.copyOf(headers))
                .rows(previewRows)
                .truncated(rows.size() > PREVIEW_ROW_LIMIT)
                .totalRowCount(rows.size())
                .totalColumnCount(headers.size())
                .build();
    }
}
