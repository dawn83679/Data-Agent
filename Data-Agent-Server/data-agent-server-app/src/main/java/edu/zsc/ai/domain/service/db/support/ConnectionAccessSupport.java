package edu.zsc.ai.domain.service.db.support;

import edu.zsc.ai.domain.service.db.ConnectionAccessService;

import java.util.Optional;

/**
 * Holds the Spring {@link ConnectionAccessService} for static helpers (e.g. {@code ActiveConnectionRegistry})
 * after application context startup. When absent (e.g. some unit tests), callers may fall back to local checks.
 */
public final class ConnectionAccessSupport {

    private static volatile ConnectionAccessService delegate;

    private ConnectionAccessSupport() {
    }

    public static void setDelegate(ConnectionAccessService service) {
        delegate = service;
    }

    public static Optional<ConnectionAccessService> tryGet() {
        return Optional.ofNullable(delegate);
    }
}
