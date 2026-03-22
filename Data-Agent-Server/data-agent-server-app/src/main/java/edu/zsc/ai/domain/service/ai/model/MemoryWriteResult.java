package edu.zsc.ai.domain.service.ai.model;

import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryWriteResult {

    private AiMemory memory;

    private MemoryToolActionEnum action;
}
