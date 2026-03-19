package edu.zsc.ai.agent.tool.sql.approval;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.context.DbContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class WriteExecutionApprovalStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final Cache<String, Boolean> approvals = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(10_000)
            .build();

    public void approve(Long userId, Long conversationId, DbContext db, String sql) {
        approvals.put(key(userId, conversationId, db, sql), Boolean.TRUE);
    }

    public void approve(DbContext db, String sql) {
        approve(RequestContext.getUserId(), RequestContext.getConversationId(), db, sql);
    }

    public boolean consumeApproved(DbContext db, String sql) {
        return consumeApproved(RequestContext.getUserId(), RequestContext.getConversationId(), db, sql);
    }

    public boolean consumeApproved(Long userId, Long conversationId, DbContext db, String sql) {
        String key = key(userId, conversationId, db, sql);
        Boolean approved = approvals.getIfPresent(key);
        if (!Boolean.TRUE.equals(approved)) {
            return false;
        }
        approvals.invalidate(key);
        return true;
    }

    private String key(Long userId, Long conversationId, DbContext db, String sql) {
        return String.join("::",
                Objects.toString(userId, "null"),
                Objects.toString(conversationId, "null"),
                Objects.toString(db.connectionId(), "null"),
                Objects.toString(db.catalog(), "null"),
                Objects.toString(db.schema(), "null"),
                normalizeSql(sql));
    }

    public static String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return StringUtils.stripEnd(StringUtils.normalizeSpace(sql), ";");
    }
}
