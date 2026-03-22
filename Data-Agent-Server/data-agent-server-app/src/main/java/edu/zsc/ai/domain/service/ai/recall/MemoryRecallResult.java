package edu.zsc.ai.domain.service.ai.recall;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.zsc.ai.common.constant.MemoryRecallConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRecallResult {

    @Builder.Default
    private List<MemoryRecallItem> items = List.of();

    @Builder.Default
    private Map<String, Object> appliedFilters = Map.of();

    private String summary;

    public static MemoryRecallResult empty() {
        return MemoryRecallResult.builder()
                .items(List.of())
                .appliedFilters(new LinkedHashMap<>())
                .summary(MemoryRecallConstant.NO_MATCH_SUMMARY)
                .build();
    }
}
