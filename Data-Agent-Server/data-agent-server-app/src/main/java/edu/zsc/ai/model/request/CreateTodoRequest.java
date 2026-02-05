package edu.zsc.ai.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoRequest {

    private String title;
    

    private String description;
    

    private String priority;
}
