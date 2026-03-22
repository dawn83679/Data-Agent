package edu.zsc.ai.domain.service.db.support;

import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.connection.ConnectionConfig;

import java.util.List;
import java.util.function.BiFunction;

/**
 * A first-success handler chain for connection providers.
 */
public final class ConnectionProviderChain<R> {

    private final List<ConnectionProviderHandler<R>> handlers;

    private ConnectionProviderChain(List<ConnectionProviderHandler<R>> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public static <R> ConnectionProviderChain<R> fromProviders(List<ConnectionProvider> providers,
                                                               BiFunction<ConnectionProvider, ConnectionConfig, R> action) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("Connection providers cannot be null or empty");
        }
        List<ConnectionProviderHandler<R>> handlers = providers.stream()
                .<ConnectionProviderHandler<R>>map(provider -> new DefaultConnectionProviderHandler<>(provider, action))
                .toList();
        return new ConnectionProviderChain<>(handlers);
    }

    public ConnectionProviderHandleResult<R> handle(ConnectionConfig config) {
        for (ConnectionProviderHandler<R> handler : handlers) {
            try {
                return handler.handle(config);
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    public record ConnectionProviderHandleResult<R>(ConnectionProvider provider, R result) {
    }

    private interface ConnectionProviderHandler<R> {

        ConnectionProviderHandleResult<R> handle(ConnectionConfig config);
    }

    private record DefaultConnectionProviderHandler<R>(
            ConnectionProvider provider,
            BiFunction<ConnectionProvider, ConnectionConfig, R> action) implements ConnectionProviderHandler<R> {

        @Override
        public ConnectionProviderHandleResult<R> handle(ConnectionConfig config) {
            return new ConnectionProviderHandleResult<>(provider, action.apply(provider, config));
        }
    }
}
