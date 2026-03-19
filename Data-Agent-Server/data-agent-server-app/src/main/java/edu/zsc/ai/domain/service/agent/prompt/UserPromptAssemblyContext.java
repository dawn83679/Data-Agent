package edu.zsc.ai.domain.service.agent.prompt;

import java.time.LocalDate;
import java.util.List;

import edu.zsc.ai.api.model.request.ChatUserMention;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserPromptAssemblyContext {

    private String userMessage;

    private String language;

    private String agentMode;

    private String modelName;

    private LocalDate currentDate;

    private String timezone;

    @Builder.Default
    private MemoryPromptContext memoryPromptContext = MemoryPromptContext.builder().build();

    @Builder.Default
    private List<ChatUserMention> userMentions = List.of();
}
