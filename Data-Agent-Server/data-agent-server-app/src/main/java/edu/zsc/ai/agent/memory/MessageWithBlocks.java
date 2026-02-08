package edu.zsc.ai.agent.memory;

import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;

import java.util.List;


public record MessageWithBlocks(CustomAiMessage customAiMessage, List<AiMessageBlock> blocks) {
}
