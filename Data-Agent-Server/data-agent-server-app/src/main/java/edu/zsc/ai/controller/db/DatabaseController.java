package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.DatabaseService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/databases")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseService databaseService;

    @GetMapping
    public ApiResponse<List<String>> listDatabases(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId) {
        log.info("Listing databases: connectionId={}", connectionId);
        List<String> databases = databaseService.listDatabases(connectionId);
        return ApiResponse.success(databases);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteDatabase(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "databaseName is required") String databaseName) {
        log.info("Deleting database: connectionId={}, databaseName={}", connectionId, databaseName);
        long userId = StpUtil.getLoginIdAsLong();
        databaseService.deleteDatabase(connectionId, databaseName, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/charsets")
    public ApiResponse<List<String>> getCharacterSets(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId) {
        log.info("Getting character sets: connectionId={}", connectionId);
        List<String> charsets = databaseService.getCharacterSets(connectionId);
        return ApiResponse.success(charsets);
    }

    @GetMapping("/collations")
    public ApiResponse<List<String>> getCollations(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "charset is required") String charset) {
        log.info("Getting collations: connectionId={}, charset={}", connectionId, charset);
        List<String> collations = databaseService.getCollations(connectionId, charset);
        return ApiResponse.success(collations);
    }

    @PostMapping("/create")
    public ApiResponse<Void> createDatabase(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "databaseName is required") String databaseName,
            @RequestParam @NotNull(message = "charset is required") String charset,
            @RequestParam String collation) {
        log.info("Creating database: connectionId={}, databaseName={}, charset={}, collation={}",
                connectionId, databaseName, charset, collation);
        long userId = StpUtil.getLoginIdAsLong();
        databaseService.createDatabase(connectionId, databaseName, charset, collation, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/exists")
    public ApiResponse<Boolean> databaseExists(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "databaseName is required") String databaseName) {
        log.info("Checking database existence: connectionId={}, databaseName={}", connectionId, databaseName);
        long userId = StpUtil.getLoginIdAsLong();
        boolean exists = databaseService.databaseExists(connectionId, databaseName, userId);
        return ApiResponse.success(exists);
    }
}
