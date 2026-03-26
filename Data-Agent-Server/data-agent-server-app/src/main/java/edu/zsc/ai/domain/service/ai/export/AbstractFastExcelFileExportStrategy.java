package edu.zsc.ai.domain.service.ai.export;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import edu.zsc.ai.common.enums.ai.FileExportFormatEnum;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;

import java.io.ByteArrayOutputStream;
import java.util.List;

abstract class AbstractFastExcelFileExportStrategy extends AbstractTabularFileExportStrategy {

    protected abstract FileExportFormatEnum exportFormat();

    protected abstract ExcelTypeEnum excelType();

    protected void configureWriter(ExcelWriterBuilder writerBuilder) {
        // Default no-op. Subclasses can register styles, charset, or other handlers.
    }

    @Override
    public String format() {
        return exportFormat().name();
    }

    @Override
    public FileExportArtifact export(FileExportRequest request) {
        List<String> headers = requireHeaders(request);
        List<List<Object>> rows = requireRows(request, headers.size());

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ExcelWriterBuilder writerBuilder = FastExcel.write(output)
                    .excelType(excelType())
                    .head(buildHead(headers));
            configureWriter(writerBuilder);
            writerBuilder.sheet("Export").doWrite(toStringRows(rows));

            return FileExportArtifact.builder()
                    .normalizedFormat(format())
                    .extension(exportFormat().getExtension())
                    .mimeType(exportFormat().getMimeType())
                    .content(output.toByteArray())
                    .rowCount(rows.size())
                    .columnCount(headers.size())
                    .preview(buildPreview(headers, rows))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate " + format() + " export", e);
        }
    }

    protected List<List<String>> buildHead(List<String> headers) {
        return headers.stream()
                .map(List::of)
                .toList();
    }

    protected List<List<String>> toStringRows(List<List<Object>> rows) {
        return rows.stream()
                .map(row -> row.stream()
                        .map(this::normalizeCell)
                        .toList())
                .toList();
    }
}
