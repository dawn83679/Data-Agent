package edu.zsc.ai.plugin.mysql.value.template;

import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL TEXT types (TEXT, TINYTEXT, MEDIUMTEXT, LONGTEXT).
 * 
 * <p>Optimized streaming strategy:
 * <ul>
 *   <li>Reads content in chunks without calling clob.length() to avoid full load</li>
 *   <li>Small TEXT (&lt; 1MB): Returns full content</li>
 *   <li>Large TEXT (≥ 1MB): Returns truncated preview with size info</li>
 * </ul>
 *
 * @author hhz
 * @date 2025-11-26
 */
public class MySQLTextProcessor extends DefaultValueProcessor {
    // Size threshold: 1MB (in characters, assuming ~1 byte per char for estimation)
    private static final int SIZE_THRESHOLD_CHARS = 1024 * 1024;
    // Preview size for large text: 1000 characters
    private static final int PREVIEW_LENGTH = 1000;
    // Chunk size for streaming read: 8KB
    private static final int CHUNK_SIZE = 8192;
    
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        
        // Get Clob without calling length() to avoid full load
        Clob clob = resultSet.getClob(columnIndex);
        if (clob == null) {
            return null;
        }
        
        try {
            return readTextWithStreaming(clob);
        } finally {
            // Safe cleanup: ignore exceptions during free()
            try {
                clob.free();
            } catch (SQLException ignored) {
                // Ignore cleanup errors
            }
        }
    }


    /**
     * Read text content using streaming approach.
     * Dynamically determines if content is large and should be truncated.
     */
    private String readTextWithStreaming(Clob clob) throws SQLException {
        try (Reader reader = clob.getCharacterStream()) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[CHUNK_SIZE];
            int totalCharsRead = 0;
            int charsRead;
            
            // Read in chunks until we hit threshold or end of stream
            while ((charsRead = reader.read(buffer)) != -1) {
                totalCharsRead += charsRead;
                
                // Check if we've exceeded the threshold
                if (totalCharsRead > SIZE_THRESHOLD_CHARS) {
                    // Large text detected - return preview with metadata
                    // Keep only the preview portion
                    if (result.length() < PREVIEW_LENGTH) {
                        result.append(buffer, 0, Math.min(charsRead, PREVIEW_LENGTH - result.length()));
                    }
                    
                    // Continue reading to get accurate total size
                    long remainingSize = totalCharsRead;
                    while ((charsRead = reader.read(buffer)) != -1) {
                        remainingSize += charsRead;
                    }
                    
                    return formatTextMetadata(result.toString(), remainingSize);
                }
                
                // Still under threshold, accumulate content
                result.append(buffer, 0, charsRead);
            }
            
            // Small text - return full content
            return result.toString();
            
        } catch (Exception e) {
            throw new SQLException("Failed to read TEXT content", e);
        }
    }
    
    /**
     * Format TEXT metadata for large text fields.
     * Format: [preview text]... [TEXT: {size}MB, truncated]
     */
    private String formatTextMetadata(String preview, long totalChars) {
        // Estimate size (assuming average 1.5 bytes per char for mixed content)
        double estimatedMB = (totalChars * 1.5) / (1024.0 * 1024.0);
        return String.format("%s... [TEXT: ~%.2fMB (%d chars), truncated]", 
                           preview, estimatedMB, totalChars);
    }
}
