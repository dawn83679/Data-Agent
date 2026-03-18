package edu.zsc.ai.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultAgentLogService implements AgentLogService {

    private final List<AgentLogHandler> handlers;

    @Override
    public void record(AgentLogEvent event) {
        if (event == null) {
            return;
        }
        for (AgentLogHandler handler : handlers) {
            handler.handle(event);
        }
    }
}
