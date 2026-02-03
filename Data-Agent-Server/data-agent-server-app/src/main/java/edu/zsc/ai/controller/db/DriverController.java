package edu.zsc.ai.controller.db;

import edu.zsc.ai.domain.model.dto.request.db.DownloadDriverRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.AvailableDriverResponse;
import edu.zsc.ai.domain.model.dto.response.db.DownloadDriverResponse;
import edu.zsc.ai.domain.model.dto.response.db.InstalledDriverResponse;
import edu.zsc.ai.domain.service.db.DriverService;
import edu.zsc.ai.util.DriverFileUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Driver Controller
 * Provides REST API endpoints for JDBC driver management.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    
    private final DriverService driverService;
    
    /**
     * List all available driver versions from Maven Central that can be downloaded.
     * This queries the remote Maven repository to show all downloadable versions.
     *
     * @param databaseType database type (e.g., "mysql")
     * @return list of available driver versions from Maven Central
     */
    @GetMapping("/available")
    public ApiResponse<List<AvailableDriverResponse>> listAvailableDrivers(
            @RequestParam @NotBlank(message = "databaseType is required") String databaseType) {
        log.info("Listing available drivers from Maven Central, databaseType={}", databaseType);
        List<AvailableDriverResponse> drivers = driverService.listAvailableDrivers(databaseType);
        return ApiResponse.success(drivers);
    }
    
    /**
     * List locally installed/downloaded driver files.
     * This scans the local drivers directory to show what's already on disk.
     *
     * @param databaseType database type (e.g., "mysql")
     * @return list of installed drivers on local disk
     */
    @GetMapping("/installed")
    public ApiResponse<List<InstalledDriverResponse>> listInstalledDrivers(
            @RequestParam @NotBlank(message = "databaseType is required") String databaseType) {
        log.info("Listing installed drivers from local disk, databaseType={}", databaseType);
        List<InstalledDriverResponse> drivers = driverService.listInstalledDrivers(databaseType);
        return ApiResponse.success(drivers);
    }
    
    /**
     * Download a driver from Maven Central.
     *
     * @param request download request (databaseType, optional version)
     * @return download response with driver path
     */
    @PostMapping("/download")
    public ApiResponse<DownloadDriverResponse> downloadDriver(
            @Valid @RequestBody DownloadDriverRequest request) {
        log.info("Downloading driver: databaseType={}, version={}", request.getDatabaseType(), request.getVersion());
        
        Path driverPath = driverService.downloadDriver(request.getDatabaseType(), request.getVersion());
        
        // Extract information from path
        String fileName = driverPath.getFileName().toString();
        String databaseType = driverPath.getParent().getFileName().toString();
        
        // Extract version from filename using utility
        String version = DriverFileUtil.extractVersionFromFileName(fileName);
        
        DownloadDriverResponse response = DownloadDriverResponse.builder()
            .driverPath(driverPath.toAbsolutePath().toString())
            .databaseType(databaseType)
            .fileName(fileName)
            .version(version)
            .build();
        
        return ApiResponse.success(response);
    }
    
    /**
     * Delete a locally installed driver.
     *
     * @param databaseType database type (e.g., "MySQL")
     * @param version driver version
     * @return success response
     */
    @DeleteMapping("/{databaseType}/{version}")
    public ApiResponse<Void> deleteDriver(
            @PathVariable @NotBlank(message = "databaseType is required") String databaseType,
            @PathVariable @NotBlank(message = "version is required") String version) {
        log.info("Deleting driver: databaseType={}, version={}", databaseType, version);
        
        driverService.deleteDriver(databaseType, version);
        return ApiResponse.success();
    }
}

