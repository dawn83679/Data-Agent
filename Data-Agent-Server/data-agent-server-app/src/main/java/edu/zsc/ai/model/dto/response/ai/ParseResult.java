package edu.zsc.ai.model.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parse result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    /**
     * Parse type (corresponds to MessageBlockTypeEnum)
     */
    private String type;

    /**
     * Original content (original format saved to database)
     */
    private String originalContent;

    /**
     * Display content (format used for tool execution and frontend display)
     */
    private String displayContent;

    /**
     * Content start position
     */
    private int startPos;

    /**
     * Content end position
     */
    private int endPos;

    /**
     * Simplified constructor for cases with only original content
     */
    public ParseResult(String type, String originalContent, int startPos, int endPos) {
        this.type = type;
        this.originalContent = originalContent;
        this.displayContent = originalContent;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    /**
     * Get content length
     */
    public int getLength() {
        return endPos - startPos;
    }
}