package edu.zsc.ai.controller.db;

import edu.zsc.ai.domain.model.dto.request.db.DownloadDriverRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.AvailableDriverResponse;
import edu.zsc.ai.domain.model.dto.response.db.DownloadDriverResponse;
import edu.zsc.ai.domain.model.dto.response.db.InstalledDriverResponse;
import edu.zsc.ai.domain.service.db.DriverService;
import edu.zsc.ai.util.DriverFileUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Driver Controller
 * Provides REST API endpoints for JDBC driver management.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Tag(name = "Driver Management", description = "JDBC driver management APIs")
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    
    private final DriverService driverService;
    
    @Operation(summary = "List Available Drivers", description = "List all available driver versions from Maven Central")
    @GetMapping("/available")
    public ApiResponse<List<AvailableDriverResponse>> listAvailableDrivers(
            @RequestParam String databaseType) {
        log.info("Listing available drivers from Maven Central, databaseType={}", databaseType);
        List<AvailableDriverResponse> drivers = driverService.listAvailableDrivers(databaseType);
        return ApiResponse.success(drivers);
    }
    
    @Operation(summary = "List Installed Drivers", description = "List locally installed/downloaded driver files")
    @GetMapping("/installed")
    public ApiResponse<List<InstalledDriverResponse>> listInstalledDrivers(
            @RequestParam String databaseType) {
        log.info("Listing installed drivers from local disk, databaseType={}", databaseType);
        List<InstalledDriverResponse> drivers = driverService.listInstalledDrivers(databaseType);
        return ApiResponse.success(drivers);
    }
    
    @Operation(summary = "Download Driver", description = "Download a JDBC driver from Maven Central")
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
    
    @Operation(summary = "Delete Driver", description = "Delete a locally installed JDBC driver")
    @DeleteMapping("/{databaseType}/{version}")
    public ApiResponse<Void> deleteDriver(
            @PathVariable String databaseType,
            @PathVariable String version) {
        log.info("Deleting driver: databaseType={}, version={}", databaseType, version);
        
        driverService.deleteDriver(databaseType, version);
        return ApiResponse.success();
    }
}

