# Message Management

This document outlines the message management system for the AI database assistant, focusing on context compression and message querying strategies.

## Message Querying

### Frontend Message Display

For frontend message list display, only active messages should be shown to users. Compressed or invalid messages are filtered out to provide a clean user experience.

```sql
-- Get messages for frontend display
SELECT
    m.id,
    m.role,
    m.token_count,
    m.created_at,
    m.status,
    m.priority,
    GROUP_CONCAT(mb.content SEPARATOR '') as content_summary
FROM ai_message m
LEFT JOIN ai_message_block mb ON m.id = mb.message_id
WHERE m.conversation_id = ?
  AND m.status = 0  -- Only show active messages
GROUP BY m.id
ORDER BY m.created_at ASC;
```

**Key Points:**
- Only messages with `status = 0` (active) are displayed
- Messages are ordered chronologically by creation time
- Content blocks are aggregated for complete message text
- Compressed messages (`status = 2`) are hidden from users
- Invalid messages (`status = 1`) are completely excluded

### AI Context Retrieval

For AI model inference, a complete context including compressed messages is required. The ordering prioritizes summary messages while maintaining chronological flow.

```sql
-- Get messages for AI context
SELECT
    m.id,
    m.role,
    m.priority,
    m.status,
    mb.block_type,
    mb.content,
    mb.extension_data
FROM ai_message m
JOIN ai_message_block mb ON m.id = mb.message_id
WHERE m.conversation_id = ?
  AND m.status != 1  -- Exclude invalid messages only
ORDER BY m.priority DESC, m.created_at ASC;
```

**Key Points:**
- Both active messages (`status = 0`) and compressed messages (`status = 2`) are included
- Invalid messages (`status = 1`) are excluded from AI context
- Summary messages (`priority = 1`) appear first due to `DESC` ordering
- Regular messages (`priority = 0`) follow in chronological order
- Complete message blocks are retrieved for full context

## Message Status System

### Status Values

- **0 (Active)**: Normal messages visible to users and available for compression
- **1 (Invalid)**: Messages manually deleted by users or rolled back from compression
- **2 (Compressed)**: Messages compressed by the system, hidden from users but included in AI context

### Priority Values

- **0 (Regular)**: Normal messages with standard priority
- **1 (Summary)**: Generated summary messages from compression operations

## Query Performance Considerations

### Index Usage

The following indexes support efficient message querying:

- `idx_ai_message_conversation_id`: Fast filtering by conversation
- `idx_ai_message_conversation_created`: Chronological ordering within conversations
- `idx_ai_message_status`: Quick status-based filtering
- `idx_ai_message_priority`: Priority-based sorting
- `idx_ai_message_block_message_id`: Efficient joins to message blocks

### Cursor-Based Pagination

For large conversations, implement cursor-based pagination using message ID for better performance:

```sql
-- Cursor-based pagination for frontend (forward)
SELECT
    m.id,
    m.role,
    m.token_count,
    m.created_at,
    m.status,
    m.priority
FROM ai_message m
WHERE m.conversation_id = ?
  AND m.status = 0
  AND m.id > ?  -- Cursor position
ORDER BY m.id ASC
LIMIT ?;

-- Cursor-based pagination for frontend (backward)
SELECT
    m.id,
    m.role,
    m.token_count,
    m.created_at,
    m.status,
    m.priority
FROM ai_message m
WHERE m.conversation_id = ?
  AND m.status = 0
  AND m.id < ?  -- Cursor position
ORDER BY m.id DESC
LIMIT ?;
```

**Key Points:**
- Use `m.id` as cursor for stable ordering
- Forward pagination: `m.id > cursor_id` with `ASC` order
- Backward pagination: `m.id < cursor_id` with `DESC` order
- More efficient than `LIMIT/OFFSET` for large datasets
- Prevents duplicate/skipped records during pagination

### AI Context Cursor Pagination

```sql
-- Cursor-based pagination for AI context
SELECT
    m.id,
    m.role,
    m.priority,
    m.status,
    mb.block_type,
    mb.content,
    mb.extension_data
FROM ai_message m
JOIN ai_message_block mb ON m.id = mb.message_id
WHERE m.conversation_id = ?
  AND m.status != 1
  AND m.id > ?  -- Cursor position
ORDER BY m.priority DESC, m.id ASC
LIMIT ?;
```

## Message Compression

### Identifying Messages for Compression

To determine if compression is needed and identify eligible messages:

```sql
-- Step 1: Check if conversation exceeds token limit
SELECT id, token_count
FROM ai_conversation
WHERE id = ?
  AND token_count > ?;  -- Token limit threshold

-- Step 2: Get oldest user messages for compression
SELECT id, token_count, created_at
FROM ai_message
WHERE conversation_id = ?
  AND role = 'USER'
  AND status = 0
  AND priority = 0
ORDER BY created_at ASC
LIMIT ?;  -- Number of user messages to compress

-- Step 3: For each user message, find the corresponding AI reply
SELECT id, token_count
FROM ai_message
WHERE conversation_id = ?
  AND role = 'ASSISTANT'
  AND id > ?  -- User message ID
  AND status = 0
  AND priority = 0
ORDER BY id ASC
LIMIT 1;
```

### Identifying Complete Conversation Segments

After executing the simple queries, the application layer needs to identify complete conversation segments for compression:

```java
// Application logic example
List<ConversationSegment> identifyCompressionSegments(Long conversationId, int segmentCount) {
    List<ConversationSegment> segments = new ArrayList<>();

    // Step 1: Get oldest user messages
    List<Message> userMessages = findOldestUserMessages(conversationId, segmentCount);

    for (Message userMsg : userMessages) {
        // Step 2: Find corresponding AI reply
        Message aiMsg = findNextAIMessage(conversationId, userMsg.getId());

        if (aiMsg != null) {
            // Step 3: Create complete Q&A segment
            ConversationSegment segment = new ConversationSegment();
            segment.setUserMessage(userMsg);
            segment.setAiMessage(aiMsg);
            segment.setTotalTokens(userMsg.getTokenCount() + aiMsg.getTokenCount());
            segment.setStartTime(userMsg.getCreatedAt());
            segment.setEndTime(aiMsg.getCreatedAt());

            segments.add(segment);
        }
    }

    return segments;
}

// Determine compression range from segments
CompressionRange calculateCompressionRange(List<ConversationSegment> segments) {
    if (segments.isEmpty()) return null;

    Long startId = segments.get(0).getUserMessage().getId();
    Long endId = segments.get(segments.size() - 1).getAiMessage().getId();
    Integer totalTokens = segments.stream()
        .mapToInt(ConversationSegment::getTotalTokens)
        .sum();

    return new CompressionRange(startId, endId, totalTokens);
}
```

### Compression Range Validation

Before compressing, validate that the message range is eligible:

```sql
-- Check if message range can be compressed
SELECT COUNT(*) as compressible_count
FROM ai_message
WHERE conversation_id = ?
  AND id BETWEEN ? AND ?  -- Compression range
  AND status != 1;  -- Exclude invalid messages only
```

### Executing Message Compression

Complete compression operation involves multiple steps:

```sql
-- Step 1: Create compression record
INSERT INTO ai_compression_record (
    conversation_id,
    start_message_id,
    end_message_id,
    compression_strategy,
    token_before,
    status
) VALUES (?, ?, ?, 'SUMMARY', ?, 0);

-- Step 2: Mark messages as compressed
UPDATE ai_message
SET status = 2  -- Compressed status
WHERE id BETWEEN ? AND ?
  AND conversation_id = ?;

-- Step 3: Create summary message (priority = 1)
INSERT INTO ai_message (
    conversation_id,
    role,
    token_count,
    status,
    priority
) VALUES (?, 'ASSISTANT', ?, 0, 1);

-- Step 4: Add summary message content blocks
INSERT INTO ai_message_block (
    message_id,
    block_type,
    content,
    extension_data
) VALUES (?, 'TEXT', ?, ?);

-- Step 5: Update compression record with summary info
UPDATE ai_compression_record
SET summary_message_id = ?,
    token_after = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?;
```

### Conversation Rollback

When a user wants to rollback from a specific message, all subsequent messages should be marked as compressed:

```sql
-- Step 1: Mark all messages after the rollback point as compressed
UPDATE ai_message
SET status = 2  -- Compressed status
WHERE conversation_id = ?
  AND id > ?  -- Rollback from this message ID
  AND status = 0;  -- Only affect active messages

-- Step 2: Update conversation token count after rollback
UPDATE ai_conversation
SET token_count = (
    SELECT COALESCE(SUM(token_count), 0)
    FROM ai_message
    WHERE conversation_id = ?
      AND status != 1  -- Exclude invalid messages
)
WHERE id = ?;
```

**Rollback Logic:**
- User selects a specific message ID (e.g., 157) as the rollback point
- All messages with ID > 157 are marked as `status = 2` (compressed)
- Update conversation token count to reflect the new total
- These messages are hidden from frontend but still available for reference
- The conversation can continue from the rollback point with new messages

### Compression Statistics

Track compression effectiveness:

```sql
-- Get compression statistics for a conversation
SELECT
    COUNT(*) as total_compressions,
    SUM(token_before) as total_tokens_before,
    SUM(token_after) as total_tokens_after,
    SUM(token_before - token_after) as total_saved_tokens,
    AVG(token_before - token_after) as avg_saved_tokens
FROM ai_compression_record
WHERE conversation_id = ?
  AND status = 0;  -- Only active compressions
```