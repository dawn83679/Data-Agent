package edu.zsc.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoList {

    private Long id;

    private Long conversationId;

    @Builder.Default
    private List<Todo> todos = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
