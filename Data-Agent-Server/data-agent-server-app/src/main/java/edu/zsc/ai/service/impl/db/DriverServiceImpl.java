package edu.zsc.ai.service.impl.db;

import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.response.db.AvailableDriverResponse;
import edu.zsc.ai.model.dto.response.db.InstalledDriverResponse;
import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.driver.DriverStorageManager;
import edu.zsc.ai.plugin.driver.MavenDriverDownloader;
import edu.zsc.ai.plugin.driver.MavenMetadataClient;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.manager.PluginManager;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import edu.zsc.ai.service.db.DriverService;
import edu.zsc.ai.util.DriverFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Driver Service Implementation
 * Handles driver download, listing, and management operations.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {


    @Override
    public Path downloadDriver(String databaseType, String version) {
        // Step 1: Select the first plugin for the database type
        Plugin plugin = PluginManager.selectFirstPluginByDbType(databaseType.toLowerCase());

        // Step 2: Get Maven coordinates from selected plugin
        MavenCoordinates downloadCoordinates = plugin.getDriverMavenCoordinates(version);

        // Step 3: Download driver
        DbType dbType = plugin.getDbType();
        Path driverPath = MavenDriverDownloader.downloadDriver(
                downloadCoordinates,
                dbType,
                null,  // Use default storage directory
                null   // Use default Maven Central URL
        );

        log.info("Successfully downloaded driver for database type {}: {}", databaseType, driverPath);
        return driverPath;
    }

    @Override
    public List<AvailableDriverResponse> listAvailableDrivers(String databaseType) {
        // Select the first plugin for the database type
        Plugin plugin = PluginManager.selectFirstPluginByDbType(databaseType.toLowerCase());

        // Get default driver coordinates from selected plugin
        MavenCoordinates coordinates = plugin.getDriverMavenCoordinates(null);

        // Query Maven Central for all available versions
        List<String> versions = MavenMetadataClient.queryVersions(
                coordinates.getGroupId(),
                coordinates.getArtifactId(),
                null  // Use default Maven Central URL
        );

        // Check which versions are installed locally
        DbType dbType = plugin.getDbType();
        Set<String> installedVersions = getInstalledVersionStrings(dbType, coordinates.getArtifactId());

        // Build response list

        return versions.stream()
                .map(version -> AvailableDriverResponse.builder()
                        .databaseType(dbType.getDisplayName())
                        .version(version)
                        .installed(installedVersions.contains(version))
                        .groupId(coordinates.getGroupId())
                        .artifactId(coordinates.getArtifactId())
                        .mavenCoordinates(coordinates.getGroupId() + ":" + coordinates.getArtifactId() + ":" + version)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<InstalledDriverResponse> listInstalledDrivers(String databaseType) {
        List<InstalledDriverResponse> installedDrivers = new ArrayList<>();
        Path baseDir = DriverStorageManager.getStorageDirectory(null, DbType.MYSQL).getParent();

        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            return installedDrivers;
        }

        // Scan specified database type directory
        Path dbTypeDir = baseDir.resolve(databaseType);
        if (Files.exists(dbTypeDir) && Files.isDirectory(dbTypeDir)) {
            List<InstalledDriverResponse> drivers = DriverFileUtil.scanDriverDirectory(dbTypeDir, databaseType);
            installedDrivers.addAll(drivers);
        }

        return installedDrivers;
    }

    /**
     * Get set of installed versions for a database type and artifact.
     *
     * @param dbType     database type
     * @param artifactId artifact ID
     * @return set of installed version strings
     */
    private Set<String> getInstalledVersionStrings(DbType dbType, String artifactId) {
        // Reuse listInstalledDrivers to get all installed drivers
        List<InstalledDriverResponse> installedDrivers = listInstalledDrivers(dbType.getCode());

        // Filter by artifactId and extract versions
        return installedDrivers.stream()
                .filter(driver -> driver.getFileName().contains(artifactId))
                .map(InstalledDriverResponse::getVersion)
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteDriver(String databaseType, String version) {
        // Step 1: Get all installed drivers for this database type
        List<InstalledDriverResponse> installedDrivers = listInstalledDrivers(databaseType);

        // Step 2: Find driver with matching version
        InstalledDriverResponse targetDriver = installedDrivers.stream()
                .filter(driver -> version.equals(driver.getVersion()))
                .findFirst()
                .orElse(null);

        if (targetDriver == null) {
            throw new BusinessException(404,
                    "Driver not found: " + databaseType + "/" + version);
        }

        // Step 3: Delete file
        Path driverFilePath = Path.of(targetDriver.getFilePath());
        DriverStorageManager.deleteDriver(driverFilePath);
        log.info("Successfully deleted driver: {}", driverFilePath);
    }
}

