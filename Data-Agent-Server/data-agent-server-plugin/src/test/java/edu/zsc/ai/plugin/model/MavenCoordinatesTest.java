package edu.zsc.ai.plugin.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MavenCoordinates.
 */
class MavenCoordinatesTest {
    
    @Test
    void testToCoordinateString() {
        MavenCoordinates coordinates = new MavenCoordinates(
            "com.mysql",
            "mysql-connector-j",
            "8.0.33"
        );
        
        assertEquals("com.mysql:mysql-connector-j:8.0.33", coordinates.toCoordinateString());
    }
}

