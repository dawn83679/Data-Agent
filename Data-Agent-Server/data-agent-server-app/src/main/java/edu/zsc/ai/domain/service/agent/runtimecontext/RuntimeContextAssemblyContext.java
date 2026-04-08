package edu.zsc.ai.domain.service.agent.runtimecontext;

import java.time.LocalDate;
import java.util.List;

import edu.zsc.ai.api.model.request.ChatUserMention;
<<<<<<< HEAD
=======
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.ConnectionSummary;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
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
<<<<<<< HEAD
=======
    private List<ConnectionSummary> availableConnections = List.of();

    @Builder.Default
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    private MemoryPromptContext memoryPromptContext = MemoryPromptContext.builder().build();

    @Builder.Default
    private List<ChatUserMention> userMentions = List.of();
}
