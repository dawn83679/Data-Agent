package edu.zsc.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Aggregate for one conversation's todo list. Stored as one row (JSON in content) in persistence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoList {

    private Long conversationId;

    @Builder.Default
    private List<Todo> todos = new ArrayList<>();

    private LocalDateTime updatedAt;
}
