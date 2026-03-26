package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum FileExportFormatEnum {

    CSV("csv", "text/csv;charset=utf-8"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    PDF("pdf", "application/pdf");

    private final String extension;
    private final String mimeType;

    public static FileExportFormatEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("format is required");
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(item -> item.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported format '" + value + "'"));
    }
}
