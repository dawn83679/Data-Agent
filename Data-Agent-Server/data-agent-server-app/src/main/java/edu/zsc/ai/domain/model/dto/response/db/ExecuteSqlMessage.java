package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlMessage {
    private ExecuteSqlMessageLevel level;
    private String code;
    private String sqlState;
    private String message;
    private String detail;
}
