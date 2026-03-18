package edu.zsc.ai.observability;

import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

@Slf4j
public class ConversationLogIterator implements AgentLogCursor {

    private final BufferedReader reader;
    private AgentLogEvent next;

    public ConversationLogIterator(Path path) throws IOException {
        this.reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        advance();
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public AgentLogEvent next() {
        if (next == null) {
            throw new NoSuchElementException("No more log events");
        }
        AgentLogEvent current = next;
        advance();
        return current;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private void advance() {
        next = null;
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                try {
                    next = JsonUtil.json2Object(line, AgentLogEvent.class);
                    return;
                } catch (RuntimeException ex) {
                    log.warn("Skip malformed agent log line: {}", StringUtils.abbreviate(line, 160));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read agent runtime log", ex);
        }
    }
}
