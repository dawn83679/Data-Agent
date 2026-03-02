package edu.zsc.ai.config.ai;

import edu.zsc.ai.agent.tool.annotation.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class AgentToolConfig {

    @Bean("agentTools")
    public List<Object> agentTools(ApplicationContext applicationContext) {
        Map<String, Object> tools = applicationContext.getBeansWithAnnotation(AgentTool.class);
        log.info("Discovered {} local agent tool bean(s)", tools.size());
        return List.copyOf(tools.values());
    }
}
