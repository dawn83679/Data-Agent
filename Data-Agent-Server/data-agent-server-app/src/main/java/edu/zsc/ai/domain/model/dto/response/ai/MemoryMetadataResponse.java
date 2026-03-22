package edu.zsc.ai.domain.model.dto.response.ai;

import java.util.Arrays;
import java.util.List;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySourceTypeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryWorkspaceLevelEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMetadataResponse {

    private List<String> scopes;

    private List<String> workspaceLevels;

    private List<String> sourceTypes;

    private List<MemoryTypeMetadata> memoryTypes;

    public static MemoryMetadataResponse fromEnums() {
        return MemoryMetadataResponse.builder()
                .scopes(Arrays.stream(MemoryScopeEnum.values())
                        .map(MemoryScopeEnum::getCode)
                        .toList())
                .workspaceLevels(Arrays.stream(MemoryWorkspaceLevelEnum.values())
                        .map(MemoryWorkspaceLevelEnum::getCode)
                        .toList())
                .sourceTypes(Arrays.stream(MemorySourceTypeEnum.values())
                        .map(MemorySourceTypeEnum::getCode)
                        .toList())
                .memoryTypes(Arrays.stream(MemoryTypeEnum.values())
                        .map(type -> MemoryTypeMetadata.builder()
                                .code(type.getCode())
                                .subTypes(MemorySubTypeEnum.validCodesFor(type))
                                .build())
                        .toList())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryTypeMetadata {
        private String code;
        private List<String> subTypes;
    }
}
