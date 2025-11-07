package edu.zsc.ai.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
/**
 * Maven coordinates for a JDBC driver.
 * Contains groupId, artifactId, and version information.
 */
@AllArgsConstructor
@Getter
public class MavenCoordinates {
    
    /**
     * Maven group ID (e.g., "com.mysql")
     */
    private String groupId;
    
    /**
     * Maven artifact ID (e.g., "mysql-connector-j")
     */
    private String artifactId;
    
    /**
     * Maven version (e.g., "8.0.33")
     */
    private String version;
    
    /**
     * Get full Maven coordinate string.
     *
     * @return coordinate string in format "groupId:artifactId:version"
     */
    public String toCoordinateString() {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }
}

