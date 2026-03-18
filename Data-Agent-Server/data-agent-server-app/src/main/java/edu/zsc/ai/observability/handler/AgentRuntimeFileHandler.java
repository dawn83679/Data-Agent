package edu.zsc.ai.observability.handler;

import edu.zsc.ai.observability.AgentLogCategory;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogHandler;
import edu.zsc.ai.observability.AgentLogType;
import edu.zsc.ai.observability.config.AgentObservabilityConfigProvider;
import edu.zsc.ai.observability.config.AgentObservabilitySettings;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
public class AgentRuntimeFileHandler implements AgentLogHandler {

    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AgentObservabilityConfigProvider configProvider;
    private final Map<Long, ConversationFile> activeFiles = new ConcurrentHashMap<>();

    @Override
    public void handle(AgentLogEvent event) {
        if (!shouldWrite(event)) {
            return;
        }
        Long conversationId = event.getConversationId();
        ConversationFile file = activeFiles.computeIfAbsent(conversationId, this::createConversationFile);
        synchronized (file.lock()) {
            try {
                Files.writeString(
                        file.path(),
                        JsonUtil.object2json(event) + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException ex) {
                log.warn("Failed to write agent runtime log for conversation {}: {}", conversationId, ex.getMessage());
            }
        }
        if (event.getType() == AgentLogType.CONVERSATION_COMPLETE || event.getType() == AgentLogType.CONVERSATION_ERROR) {
            activeFiles.remove(conversationId);
        }
    }

    private boolean shouldWrite(AgentLogEvent event) {
        AgentObservabilitySettings settings = configProvider.current();
        if (!settings.isEnabled() || !settings.isRuntimeLogEnabled() || event == null || event.getConversationId() == null) {
            return false;
        }
        AgentLogCategory category = event.getType() != null ? event.getType().getCategory() : AgentLogCategory.GENERAL;
        if (category == AgentLogCategory.SSE && !settings.isSseEventLogEnabled()) {
            return false;
        }
        if (category == AgentLogCategory.TOKEN && !settings.isIncludeTokenStream()) {
            return false;
        }
        if (category == AgentLogCategory.TOOL && !settings.isToolEventLogEnabled()) {
            return false;
        }
        return true;
    }

    private ConversationFile createConversationFile(Long conversationId) {
        Path directory = resolveRuntimeLogDir();
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create agent runtime log directory: " + directory, ex);
        }
        String fileName = FILE_TIME_FORMAT.format(LocalDateTime.now()) + "-" + conversationId + ".log";
        return new ConversationFile(directory.resolve(fileName), new Object());
    }

    private Path resolveRuntimeLogDir() {
        String configuredPath = configProvider.current().getRuntimeLogDir();
        if (configuredPath == null || configuredPath.isBlank()) {
            return Paths.get(System.getProperty("user.home"), ".data-agent", "logs", "agent", "runtime");
        }
        if (configuredPath.startsWith("~/")) {
            return Paths.get(System.getProperty("user.home"), configuredPath.substring(2));
        }
        if ("~".equals(configuredPath)) {
            return Paths.get(System.getProperty("user.home"));
        }
        return Paths.get(configuredPath);
    }

    private record ConversationFile(Path path, Object lock) {
    }
}
