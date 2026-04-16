package edu.zsc.ai.domain.service.org;

import edu.zsc.ai.context.RequestContextInfo;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds {@link RequestContextInfo} workspace fields from HTTP headers after authentication.
 */
public interface WorkspaceRequestContextService {

    /**
     * Populates user id and workspace/org fields. Conversation and connection fields are left null.
     */
    RequestContextInfo buildBaseContext(long userId, HttpServletRequest request);
}
