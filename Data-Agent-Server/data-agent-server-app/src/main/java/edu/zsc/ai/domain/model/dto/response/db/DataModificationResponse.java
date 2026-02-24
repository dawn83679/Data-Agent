package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for data modification operations (INSERT, UPDATE, DELETE)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataModificationResponse {

    /**
     * Number of affected rows
     */
    private int affectedRows;

    /**
     * Generated keys (for INSERT operations)
     */
    private Long generatedId;
}
