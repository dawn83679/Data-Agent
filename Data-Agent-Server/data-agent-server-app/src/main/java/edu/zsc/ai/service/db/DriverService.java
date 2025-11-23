package edu.zsc.ai.service.db;

import edu.zsc.ai.model.dto.response.db.AvailableDriverResponse;
import edu.zsc.ai.model.dto.response.db.InstalledDriverResponse;

import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for managing JDBC driver downloads and installations.
 */
public interface DriverService {
    
    /**
     * Download a driver from Maven Central.
     *
     * @param databaseType database type (e.g., "MySQL")
     * @param version driver version (optional, uses latest if null)
     * @return path to downloaded driver file
     */
    Path downloadDriver(String databaseType, String version);
    
    /**
     * List all available driver versions from Maven Central.
     * Queries the remote Maven repository to show all downloadable versions.
     *
     * @param databaseType database type (required)
     * @return list of available driver versions from Maven Central with installation status
     */
    List<AvailableDriverResponse> listAvailableDrivers(String databaseType);
    
    /**
     * List locally installed/downloaded drivers.
     * Scans the local drivers directory.
     *
     * @param databaseType database type (required)
     * @return list of installed drivers on local disk
     */
    List<InstalledDriverResponse> listInstalledDrivers(String databaseType);
    
    /**
     * Delete a locally installed driver.
     *
     * @param databaseType database type (e.g., "MySQL")
     * @param version driver version
     */
    void deleteDriver(String databaseType, String version);
}

