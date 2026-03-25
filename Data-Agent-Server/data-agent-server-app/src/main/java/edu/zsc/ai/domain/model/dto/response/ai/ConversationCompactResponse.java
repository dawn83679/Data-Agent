package edu.zsc.ai.domain.model.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCompactResponse {

    private boolean compressed;

    private Integer tokenCountBefore;

    private Integer tokenCountAfter;

    private Integer compressedMessageCount;

    private Integer keptRecentCount;

    private String summary;
}
