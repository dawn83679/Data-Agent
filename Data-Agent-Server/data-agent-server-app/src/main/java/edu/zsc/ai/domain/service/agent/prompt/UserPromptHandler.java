package edu.zsc.ai.domain.service.agent.prompt;

public interface UserPromptHandler extends PromptHandler<
        PromptHandleRequest<UserPromptAssemblyContext, UserPromptSection>,
        PromptSectionResult<UserPromptSection>> {
}
