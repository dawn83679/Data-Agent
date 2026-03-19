package edu.zsc.ai.domain.service.agent.prompt;

public record PromptHandleRequest<C, S>(C context, S section) {
}
