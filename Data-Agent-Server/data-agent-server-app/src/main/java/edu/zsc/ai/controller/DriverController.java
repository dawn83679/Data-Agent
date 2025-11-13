package edu.zsc.ai.controller;

import edu.zsc.ai.model.dto.request.DownloadDriverRequest;
import edu.zsc.ai.model.dto.response.ApiResponse;
import edu.zsc.ai.model.dto.response.AvailableDriverResponse;
import edu.zsc.ai.model.dto.response.DownloadDriverResponse;
import edu.zsc.ai.model.dto.response.InstalledDriverResponse;
import edu.zsc.ai.service.DriverService;
import edu.zsc.ai.util.DriverFileUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

/**
 * 驱动控制器
 * 提供用于 JDBC 驱动管理的 REST API 接口。
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    
    private final DriverService driverService;
    
    /**
     * 列出可从 Maven Central 下载的所有可用驱动版本。
     * 会查询远程 Maven 仓库以展示可下载版本列表。
     *
     * @param databaseType 数据库类型（例如 "mysql"）
     * @return 来自 Maven Central 的可用驱动版本列表
     */
    @GetMapping("/available")
    public ApiResponse<List<AvailableDriverResponse>> listAvailableDrivers(
            @RequestParam String databaseType) {
        log.info("Listing available drivers from Maven Central, databaseType={}", databaseType);
        List<AvailableDriverResponse> drivers = driverService.listAvailableDrivers(databaseType);
        return ApiResponse.success(drivers);
    }
    
    /**
     * 列出本地已安装/已下载的驱动文件。
     * 会扫描本地驱动目录以展示当前磁盘上已有的内容。
     *
     * @param databaseType 数据库类型（例如 "mysql"）
     * @return 本地磁盘上已安装驱动的列表
     */
    @GetMapping("/installed")
    public ApiResponse<List<InstalledDriverResponse>> listInstalledDrivers(
            @RequestParam String databaseType) {
        log.info("Listing installed drivers from local disk, databaseType={}", databaseType);
        List<InstalledDriverResponse> drivers = driverService.listInstalledDrivers(databaseType);
        return ApiResponse.success(drivers);
    }
    
    /**
     * 从 Maven Central 下载驱动。
     *
     * @param request 下载请求（databaseType，可选的 version）
     * @return 包含驱动路径的下载结果
     */
    @PostMapping("/download")
    public ApiResponse<DownloadDriverResponse> downloadDriver(
            @Valid @RequestBody DownloadDriverRequest request) {
        log.info("Downloading driver: databaseType={}, version={}", request.getDatabaseType(), request.getVersion());
        
        Path driverPath = driverService.downloadDriver(request.getDatabaseType(), request.getVersion());
        
        // 从路径中提取信息
        String fileName = driverPath.getFileName().toString();
        String databaseType = driverPath.getParent().getFileName().toString();
        
        // 使用工具从文件名中提取版本号
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
     * 删除本地已安装的驱动。
     *
     * @param databaseType 数据库类型（例如 "MySQL"）
     * @param version 驱动版本
     * @return 成功响应
     */
    @DeleteMapping("/{databaseType}/{version}")
    public ApiResponse<Void> deleteDriver(
            @PathVariable String databaseType,
            @PathVariable String version) {
        log.info("Deleting driver: databaseType={}, version={}", databaseType, version);
        
        driverService.deleteDriver(databaseType, version);
        return ApiResponse.success();
    }
}

