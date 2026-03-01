package edu.zsc.ai.agent.confirm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An in-memory record tracking a single write-confirmation token.
 * Status transitions: PENDING → CONFIRMED → CONSUMED.
 * TTL is managed by Caffeine; no createdAt/expiresAt fields needed here.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WriteConfirmationEntry {

    private String token;
    private Long userId;
    private Long conversationId;
    private String sql;
    private String databaseName;
    private String schemaName;
    private WriteConfirmationStatus status;
}
