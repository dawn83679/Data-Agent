package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.util.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.response.db.AvailableDriverResponse;
import edu.zsc.ai.domain.model.dto.response.db.InstalledDriverResponse;
import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.driver.DriverStorageManager;
import edu.zsc.ai.plugin.driver.MavenDriverDownloader;
import edu.zsc.ai.plugin.driver.MavenMetadataClient;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import edu.zsc.ai.domain.service.db.DriverService;
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
        List<Plugin> plugins = DefaultPluginManager.getInstance().getPluginsByDbType(databaseType.toLowerCase());

        MavenCoordinates downloadCoordinates = null;
        Plugin plugin = null;
        for (Plugin p : plugins) {
            try {
                downloadCoordinates = p.getDriverMavenCoordinates(version);
                plugin = p;
                break;
            } catch (RuntimeException e) {
                // Try next plugin
            }
        }
        if (plugin == null || downloadCoordinates == null) {
            throw new BusinessException(400, "No plugin supports driver version " + version + " for database type " + databaseType);
        }

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
        List<Plugin> plugins = DefaultPluginManager.getInstance().getPluginsByDbType(databaseType.toLowerCase());

        MavenCoordinates coordinates = null;
        Plugin plugin = null;
        for (Plugin p : plugins) {
            try {
                coordinates = p.getDriverMavenCoordinates(null);
                plugin = p;
                break;
            } catch (RuntimeException e) {
                // Try next plugin
            }
        }
        if (plugin == null || coordinates == null) {
            throw new BusinessException(400, "No plugin provides default driver coordinates for database type " + databaseType);
        }

        final MavenCoordinates coords = coordinates;
        List<String> versions = MavenMetadataClient.queryVersions(
                coords.getGroupId(),
                coords.getArtifactId(),
                null  // Use default Maven Central URL
        );

        DbType dbType = plugin.getDbType();
        Set<String> installedVersions = getInstalledVersionStrings(dbType, coords.getArtifactId());

        return versions.stream()
                .map(version -> AvailableDriverResponse.builder()
                        .databaseType(dbType.getDisplayName())
                        .version(version)
                        .installed(installedVersions.contains(version))
                        .groupId(coords.getGroupId())
                        .artifactId(coords.getArtifactId())
                        .mavenCoordinates(coords.getGroupId() + ":" + coords.getArtifactId() + ":" + version)
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

