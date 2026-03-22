package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import edu.zsc.ai.domain.service.handler.Handler;

public interface MemoryRecallHandler extends Handler<MemoryRecallQuery, List<MemoryRecallItem>> {
}
