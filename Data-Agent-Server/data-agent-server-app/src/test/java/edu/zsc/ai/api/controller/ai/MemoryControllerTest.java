package edu.zsc.ai.api.controller.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySourceTypeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryMetadataResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.ai.MemoryService;

class MemoryControllerTest {

    private final MemoryService memoryService = mock(MemoryService.class);
    private final MemoryController controller = new MemoryController(memoryService);

    @Test
    void metadata_returnsEnumDrivenResponseShape() {
        ApiResponse<MemoryMetadataResponse> response = controller.metadata();

        assertNotNull(response);
        assertNotNull(response.getData());
        MemoryMetadataResponse body = response.getData();

        assertEquals(Arrays.stream(MemoryScopeEnum.values()).map(MemoryScopeEnum::getCode).toList(), body.getScopes());
        assertEquals(Arrays.stream(MemorySourceTypeEnum.values()).map(MemorySourceTypeEnum::getCode).toList(), body.getSourceTypes());

        Map<String, MemoryMetadataResponse.MemoryTypeMetadata> metadataByType = body.getMemoryTypes().stream()
                .collect(Collectors.toMap(MemoryMetadataResponse.MemoryTypeMetadata::getCode, Function.identity()));
        assertEquals(MemoryTypeEnum.values().length, metadataByType.size());
        for (MemoryTypeEnum type : MemoryTypeEnum.values()) {
            MemoryMetadataResponse.MemoryTypeMetadata typeMetadata = metadataByType.get(type.getCode());
            assertNotNull(typeMetadata);
            assertEquals(MemorySubTypeEnum.validCodesFor(type), typeMetadata.getSubTypes());
        }
        assertFalse(metadataByType.get(MemoryTypeEnum.WORKFLOW_CONSTRAINT.getCode()).getSubTypes()
                        .contains(MemorySubTypeEnum.CONVERSATION_WORKING_MEMORY.getCode()),
                "Internal conversation working memory subtype should stay out of external metadata");
    }

    @Test
    void maintenance_controller_service_and_report_areRemoved() {
        assertThrows(NoSuchMethodException.class, () -> MemoryController.class.getDeclaredMethod("maintenanceSummary"));
        assertThrows(NoSuchMethodException.class, () -> MemoryController.class.getDeclaredMethod("runMaintenance"));
        assertThrows(NoSuchMethodException.class, () -> MemoryService.class.getMethod("inspectCurrentUserMaintenance"));
        assertThrows(NoSuchMethodException.class, () -> MemoryService.class.getMethod("runCurrentUserMaintenance"));
        assertThrows(NoSuchMethodException.class, () -> MemoryService.class.getMethod("runGlobalMaintenance"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("edu.zsc.ai.domain.service.ai.model.MemoryMaintenanceReport"));
    }
}
