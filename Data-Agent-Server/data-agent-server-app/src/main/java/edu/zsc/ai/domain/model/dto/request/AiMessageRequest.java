package edu.zsc.ai.domain.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageRequest {

    private Long conversationId;

    private Short status;

    private Short priority;

    private Long startId;

    private Long endId;
}
