package edu.zsc.ai.plugin.connection;

import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.MavenCoordinates;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MavenUrlBuilder.
 */
class MavenUrlBuilderTest {
    
    @Test
    void testBuildDownloadUrl_Success() throws PluginException {
        MavenCoordinates coordinates = new MavenCoordinates(
            "com.mysql",
            "mysql-connector-j",
            "8.0.33"
        );
        
        URL url = MavenUrlBuilder.buildDownloadUrl(coordinates);
        
        assertEquals("https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar",
            url.toString());
    }
    
    @Test
    void testBuildDownloadUrl_WithCustomBaseUrl() throws PluginException {
        MavenCoordinates coordinates = new MavenCoordinates(
            "com.mysql",
            "mysql-connector-j",
            "8.0.33"
        );
        String customBaseUrl = "https://custom.repo.com/maven2";
        
        URL url = MavenUrlBuilder.buildDownloadUrl(coordinates, customBaseUrl);
        
        assertEquals("https://custom.repo.com/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar",
            url.toString());
    }
    
    @Test
    void testBuildMetadataUrl_Success() throws PluginException {
        URL url = MavenUrlBuilder.buildMetadataUrl("com.mysql", "mysql-connector-j");
        
        assertEquals("https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/maven-metadata.xml",
            url.toString());
    }
    
    @Test
    void testBuildMetadataUrl_WithCustomBaseUrl() throws PluginException {
        String customBaseUrl = "https://custom.repo.com/maven2";
        
        URL url = MavenUrlBuilder.buildMetadataUrl("com.mysql", "mysql-connector-j", customBaseUrl);
        
        assertEquals("https://custom.repo.com/maven2/com/mysql/mysql-connector-j/maven-metadata.xml",
            url.toString());
    }
    
    @Test
    void testBuildMetadataUrl_EmptyGroupId() {
        assertThrows(PluginException.class, () -> {
            MavenUrlBuilder.buildMetadataUrl("", "mysql-connector-j");
        });
    }
    
    @Test
    void testBuildMetadataUrl_EmptyArtifactId() {
        assertThrows(PluginException.class, () -> {
            MavenUrlBuilder.buildMetadataUrl("com.mysql", "");
        });
    }
    
    @Test
    void testBuildDownloadUrl_WithSnapshotVersion() throws PluginException {
        MavenCoordinates coordinates = new MavenCoordinates(
            "com.mysql",
            "mysql-connector-j",
            "8.0.33-SNAPSHOT"
        );
        
        URL url = MavenUrlBuilder.buildDownloadUrl(coordinates);
        
        assertEquals("https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33-SNAPSHOT/mysql-connector-j-8.0.33-SNAPSHOT.jar",
            url.toString());
    }
    
    @Test
    void testBuildDownloadUrl_WithBetaVersion() throws PluginException {
        MavenCoordinates coordinates = new MavenCoordinates(
            "org.postgresql",
            "postgresql",
            "42.7.2-beta1"
        );
        
        URL url = MavenUrlBuilder.buildDownloadUrl(coordinates);
        
        assertEquals("https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2-beta1/postgresql-42.7.2-beta1.jar",
            url.toString());
    }
    
    @Test
    void testBuildDownloadUrl_WithRCVersion() throws PluginException {
        MavenCoordinates coordinates = new MavenCoordinates(
            "com.example",
            "driver",
            "2.1.0-RC1"
        );
        
        URL url = MavenUrlBuilder.buildDownloadUrl(coordinates);
        
        assertEquals("https://repo1.maven.org/maven2/com/example/driver/2.1.0-RC1/driver-2.1.0-RC1.jar",
            url.toString());
    }
    
    @Test
    void testBuildDownloadUrl_WithMultipleDotGroupId() throws PluginException {
        // Test groupId with multiple dots
        MavenCoordinates coordinates = new MavenCoordinates(
            "org.springframework.boot",
            "spring-boot-starter",
            "3.0.0"
        );
        
        URL url = MavenUrlBuilder.buildDownloadUrl(coordinates);
        
        assertEquals("https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter/3.0.0/spring-boot-starter-3.0.0.jar",
            url.toString());
    }
}

