package edu.zsc.ai.observability;

import java.io.Closeable;
import java.util.Iterator;

public interface AgentLogCursor extends Iterator<AgentLogEvent>, Closeable {
}
