package edu.zsc.ai.plugin.connection;

import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.exception.PluginErrorCode;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.MavenCoordinates;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Utility class for downloading JDBC drivers from Maven Central.
 * Combines multiple components to complete the download process:
 * - MavenUrlBuilder: Builds download URLs
 * - HttpDownloader: Downloads files via HTTP
 * - JarFileValidator: Validates downloaded JAR files
 * - DriverStorageManager: Manages storage directories and files
 */
public final class MavenDriverDownloader {
    
    private static final Logger logger = Logger.getLogger(MavenDriverDownloader.class.getName());
    
    private MavenDriverDownloader() {
        // Utility class
    }
    
    /**
     * Download a driver from Maven Central.
     *
     * @param coordinates Maven coordinates (groupId, artifactId, version)
     * @param dbType database type (for directory organization)
     * @param baseStorageDir base storage directory (default: ./drivers)
     * @param mavenRepositoryUrl Maven repository URL (default: Maven Central)
     * @return path to downloaded driver file
     * @throws PluginException if download fails
     */
    public static Path downloadDriver(
            MavenCoordinates coordinates,
            DbType dbType,
            String baseStorageDir,
            String mavenRepositoryUrl) throws PluginException {
        
        // Step 1: Determine storage directory and file path
        Path storageDir = DriverStorageManager.getStorageDirectory(baseStorageDir, dbType);
        Path driverFilePath = DriverStorageManager.getDriverFilePath(baseStorageDir, dbType, coordinates);
        
        // Step 2: Check if driver already exists (cache check)
        if (DriverStorageManager.driverExists(driverFilePath)) {
            logger.info("Driver already exists, skipping download: " + driverFilePath);
            return driverFilePath;
        }
        
        // Step 3: Build download URL
        String repoUrl = mavenRepositoryUrl != null && !mavenRepositoryUrl.isEmpty()
            ? mavenRepositoryUrl
            : DriverConstants.MAVEN_CENTRAL_URL;
        
        java.net.URL downloadUrl = MavenUrlBuilder.buildDownloadUrl(coordinates, repoUrl);
        logger.info("Downloading driver from: " + downloadUrl);
        
        // Step 4: Ensure storage directory exists
        DriverStorageManager.ensureDirectoryExists(storageDir);
        
        // Step 5: Download file via HTTP
        try {
            HttpDownloader.download(downloadUrl, driverFilePath);
        } catch (PluginException e) {
            // Clean up partial file if download failed
            try {
                if (java.nio.file.Files.exists(driverFilePath)) {
                    java.nio.file.Files.delete(driverFilePath);
                }
            } catch (java.io.IOException deleteException) {
                logger.warning("Failed to delete partial file: " + deleteException.getMessage());
            }
            throw e;
        }
        
        // Step 6: Validate downloaded JAR file
        try {
            JarFileValidator.validate(driverFilePath);
        } catch (PluginException e) {
            // Clean up invalid file
            try {
                java.nio.file.Files.delete(driverFilePath);
            } catch (java.io.IOException deleteException) {
                logger.warning("Failed to delete invalid file: " + deleteException.getMessage());
            }
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED,
                "Downloaded file is invalid or corrupted: " + e.getMessage(), e);
        }
        
        logger.info("Successfully downloaded and validated driver: " + driverFilePath);
        return driverFilePath;
    }
    
    /**
     * Download a driver using default settings.
     *
     * @param coordinates Maven coordinates
     * @param dbType database type
     * @return path to downloaded driver file
     * @throws PluginException if download fails
     */
    public static Path downloadDriver(MavenCoordinates coordinates, DbType dbType) throws PluginException {
        return downloadDriver(coordinates, dbType, null, null);
    }
}

