package edu.zsc.ai.domain.service.agent.runtimecontext;

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
public class RuntimeContextAssemblyContext {

    private String language;

    private LocalDate currentDate;

    private String timezone;

    @Builder.Default
    private MemoryPromptContext memoryPromptContext = MemoryPromptContext.builder().build();

    @Builder.Default
    private List<ChatUserMention> userMentions = List.of();
}
