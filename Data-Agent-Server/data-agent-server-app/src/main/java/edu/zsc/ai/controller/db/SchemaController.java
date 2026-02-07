package edu.zsc.ai.controller.db;

import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.SchemaService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaService schemaService;

    @GetMapping
    public ApiResponse<List<String>> listSchemas(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam(required = false) String catalog) {
        log.info("Listing schemas: connectionId={}, catalog={}", connectionId, catalog);
        List<String> schemas = schemaService.listSchemas(connectionId, catalog);
        return ApiResponse.success(schemas);
    }
}
