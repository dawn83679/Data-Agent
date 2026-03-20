package edu.zsc.ai.api.controller.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import edu.zsc.ai.common.enums.ai.MemoryWorkspaceLevelEnum;
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
        assertEquals(Arrays.stream(MemoryWorkspaceLevelEnum.values()).map(MemoryWorkspaceLevelEnum::getCode).toList(), body.getWorkspaceLevels());
        assertEquals(Arrays.stream(MemorySourceTypeEnum.values()).map(MemorySourceTypeEnum::getCode).toList(), body.getSourceTypes());

        Map<String, MemoryMetadataResponse.MemoryTypeMetadata> metadataByType = body.getMemoryTypes().stream()
                .collect(Collectors.toMap(MemoryMetadataResponse.MemoryTypeMetadata::getCode, Function.identity()));
        assertEquals(MemoryTypeEnum.values().length, metadataByType.size());
        for (MemoryTypeEnum type : MemoryTypeEnum.values()) {
            MemoryMetadataResponse.MemoryTypeMetadata typeMetadata = metadataByType.get(type.getCode());
            assertNotNull(typeMetadata);
            assertEquals(MemorySubTypeEnum.validCodesFor(type), typeMetadata.getSubTypes());
        }
    }
}
