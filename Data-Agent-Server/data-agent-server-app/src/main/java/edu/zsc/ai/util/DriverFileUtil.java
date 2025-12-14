package edu.zsc.ai.util;

import edu.zsc.ai.domain.model.dto.response.db.InstalledDriverResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Driver file utility class.
 * Provides helper methods for driver file operations.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
public final class DriverFileUtil {
    
    /**
     * Pattern to extract version from filename.
     * Supports versions with suffixes (with or without hyphen):
     * - With hyphen: 8.0.33-beta, 1.0.0-SNAPSHOT, 2.1.0-RC1
     * - Without hyphen: 2.1.0RC1, 1.0.0SNAPSHOT
     * Extracts only the numeric part (8.0.33) and ignores suffixes
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile(".*-([\\d.]+)(?:-?[A-Za-z0-9]+)?\\.jar$");
    
    private DriverFileUtil() {
        // Utility class
    }
    
    /**
     * Extract version from driver filename.
     *
     * @param fileName file name (e.g., "mysql-connector-j-8.0.33.jar")
     * @return version string (e.g., "8.0.33"), or "unknown" if not found
     */
    public static String extractVersionFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }
        
        Matcher matcher = VERSION_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }
    
    /**
     * Scan a directory for installed driver files.
     *
     * @param directory directory to scan
     * @param databaseType database type name
     * @return list of installed driver responses
     */
    public static List<InstalledDriverResponse> scanDriverDirectory(Path directory, String databaseType) {
        List<InstalledDriverResponse> installedDrivers = new ArrayList<>();

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .forEach(jarFile -> {
                        try {
                            String fileName = jarFile.getFileName().toString();
                            String version = extractVersionFromFileName(fileName);
                            FileTime lastModifiedTime = Files.getLastModifiedTime(jarFile);
                            Instant instant = lastModifiedTime.toInstant();

                            InstalledDriverResponse response = InstalledDriverResponse.builder()
                                    .databaseType(databaseType)
                                    .fileName(fileName)
                                    .version(version)
                                    .filePath(jarFile.toAbsolutePath().toString())
                                    .fileSize(Files.size(jarFile))
                                    .lastModified(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                                    .build();
                            installedDrivers.add(response);
                        } catch (IOException e) {
                            log.warn("Failed to read driver file info: {}, {}", jarFile, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to scan directory: {}, {}", directory, e.getMessage());
        }

        return installedDrivers;
    }
}

