package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlSubResult {
    private ExecuteSqlResponseType type;
    private ExecuteSqlResultSet resultSet;
    private List<String> headers;
    private List<List<Object>> rows;
    private int affectedRows;
    private List<ExecuteSqlMessage> messages;
}
