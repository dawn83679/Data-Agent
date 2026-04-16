package edu.zsc.ai.domain.service.db;

/**
 * Connection visibility and write eligibility for the current request workspace.
 */
public interface ConnectionAccessService {

    boolean isOwner(long connectionId, long userId);

    /**
     * Whether the current user may use this connection for read or AI read tools in the current workspace.
     */
    boolean canRead(long connectionId);

    void assertReadable(long connectionId);

    /**
     * Blocks organization COMMON members from any database write (AI or otherwise).
     */
    void assertWritableForCurrentWorkspace(long connectionId);

    /**
     * REST workbench and connection management: forbidden for ORGANIZATION + COMMON (AI tools may still read authorized data).
     */
    void assertWorkbenchApiAllowed();
}
