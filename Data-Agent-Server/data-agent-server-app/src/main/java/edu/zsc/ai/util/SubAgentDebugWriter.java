package edu.zsc.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public final class SubAgentDebugWriter {

    private static final Path LOG_FILE = Paths.get("logs", "subagent-debug.txt");
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int MAX_VALUE_LENGTH = 800;

    private SubAgentDebugWriter() {
    }

    public static synchronized void append(String module, String event, Map<String, ?> fields) {
        try {
            Path parent = LOG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    LOG_FILE,
                    buildLine(module, event, fields) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.warn("Failed to append sub-agent debug log: {}", e.getMessage());
        }
    }

    public static Map<String, Object> fields(Object... keyValues) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        if (keyValues == null) {
            return fields;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            fields.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        if (keyValues.length % 2 != 0) {
            fields.put("_fieldsWarning", "odd number of field arguments");
        }
        return fields;
    }

    private static String buildLine(String module, String event, Map<String, ?> fields) {
        StringBuilder line = new StringBuilder();
        line.append("[")
                .append(LocalDateTime.now().format(TS_FORMAT))
                .append("] ")
                .append(nullSafe(module))
                .append(" ")
                .append(nullSafe(event));
        if (fields != null) {
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                line.append(" | ")
                        .append(entry.getKey())
                        .append("=")
                        .append(sanitize(entry.getValue()));
            }
        }
        return line.toString();
    }

    private static String sanitize(Object value) {
        String text = nullSafe(value);
        text = text.replace("\r", " ").replace("\n", " ").trim();
        if (text.length() > MAX_VALUE_LENGTH) {
            return text.substring(0, MAX_VALUE_LENGTH - 3) + "...";
        }
        return text;
    }

    private static String nullSafe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
