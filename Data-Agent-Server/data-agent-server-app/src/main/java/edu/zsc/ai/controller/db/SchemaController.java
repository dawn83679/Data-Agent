package edu.zsc.ai.controller.db;

import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.SchemaService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Schema Controller
 * Provides REST API for listing schemas on an active connection.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaService schemaService;

    /**
     * List schemas in the given catalog for the given connection.
     *
     * @param connectionId unique connection identifier (required)
     * @param catalog      catalog/database name (optional; null means current catalog)
     * @return list of schema names
     */
    @GetMapping
    public ApiResponse<List<String>> listSchemas(
            @RequestParam @NotBlank(message = "connectionId is required") String connectionId,
            @RequestParam(required = false) String catalog) {
        log.info("Listing schemas: connectionId={}, catalog={}", connectionId, catalog);
        List<String> schemas = schemaService.listSchemas(connectionId, catalog);
        return ApiResponse.success(schemas);
    }
}
