package edu.zsc.ai.plugin.connection;

import edu.zsc.ai.plugin.exception.PluginErrorCode;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.MavenCoordinates;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class for building Maven Central download URLs.
 */
public final class MavenUrlBuilder {
    
    private MavenUrlBuilder() {
        // Utility class
    }
    
    /**
     * Build Maven Central download URL for a JAR file.
     *
     * @param coordinates Maven coordinates (groupId, artifactId, version)
     * @param baseUrl Maven repository base URL (default: Maven Central)
     * @return download URL
     * @throws PluginException if URL construction fails
     */
    public static URL buildDownloadUrl(MavenCoordinates coordinates, String baseUrl) throws PluginException {
        String urlString = String.format("%s/%s/%s/%s/%s-%s.jar",
            baseUrl != null ? baseUrl : DriverConstants.MAVEN_CENTRAL_URL,
            coordinates.getGroupId().replace('.', '/'),
            coordinates.getArtifactId(),
            coordinates.getVersion(),
            coordinates.getArtifactId(),
            coordinates.getVersion());
        
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED,
                "Failed to build Maven download URL: " + urlString, e);
        }
    }
    
    /**
     * Build Maven Central download URL using default base URL.
     *
     * @param coordinates Maven coordinates
     * @return download URL
     * @throws PluginException if URL construction fails
     */
    public static URL buildDownloadUrl(MavenCoordinates coordinates) throws PluginException {
        return buildDownloadUrl(coordinates, DriverConstants.MAVEN_CENTRAL_URL);
    }
    
    /**
     * Build Maven Central metadata URL for querying available versions.
     *
     * @param groupId Maven group ID
     * @param artifactId Maven artifact ID
     * @param baseUrl Maven repository base URL (default: Maven Central)
     * @return metadata URL
     * @throws PluginException if URL construction fails
     */
    public static URL buildMetadataUrl(String groupId, String artifactId, String baseUrl) throws PluginException {
        if (groupId == null || groupId.isEmpty() || artifactId == null || artifactId.isEmpty()) {
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED,
                "groupId and artifactId are required for metadata URL");
        }
        
        String urlString = String.format("%s/%s/%s/maven-metadata.xml",
            baseUrl != null ? baseUrl : DriverConstants.MAVEN_CENTRAL_URL,
            groupId.replace('.', '/'),
            artifactId);
        
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED,
                "Failed to build Maven metadata URL: " + urlString, e);
        }
    }
    
    /**
     * Build Maven Central metadata URL using default base URL.
     *
     * @param groupId Maven group ID
     * @param artifactId Maven artifact ID
     * @return metadata URL
     * @throws PluginException if URL construction fails
     */
    public static URL buildMetadataUrl(String groupId, String artifactId) throws PluginException {
        return buildMetadataUrl(groupId, artifactId, DriverConstants.MAVEN_CENTRAL_URL);
    }
}

