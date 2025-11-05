# Plugin Manager Design

## Context

Currently, the plugin system uses Java SPI for discovery, but there is no centralized management component. Each part of the application that needs to use plugins must directly call `ServiceLoader.load(Plugin.class)`, which:
- Creates new plugin instances each time
- No lifecycle management (plugins are not initialized/started)
- No centralized plugin registry for lookup
- No caching mechanism
- Difficult to test and mock

A PluginManager is needed to address these issues.

## Goals / Non-Goals

### Goals
- Centralized plugin loading and management
- Plugin lifecycle management (initialize → start → stop → destroy)
- Efficient plugin lookup by ID, database type, or capability
- Thread-safe plugin registry with caching
- Integration with Spring Boot application lifecycle
- Support for lazy plugin initialization

### Non-Goals
- Plugin hot-reloading (not in this change)
- Plugin versioning/dependency resolution (not in this change)
- Plugin marketplace or remote loading (not in this change)
- Plugin sandboxing or security isolation (not in this change)

## Decisions

### Decision 1: PluginManager as Interface + Default Implementation

**Choice**: Define `PluginManager` interface with `DefaultPluginManager` implementation

**Rationale**:
- Allows for alternative implementations (e.g., with hot-reloading)
- Easier to mock for testing
- Follows dependency inversion principle

**Alternatives considered**:
- Single concrete class: Less flexible for future enhancements
- Abstract class: Harder to test with mocking

### Decision 2: Plugin Lifecycle States

**States**:
1. `DISCOVERED` - Plugin found by SPI but not loaded
2. `LOADED` - Plugin instance created
3. `INITIALIZED` - Plugin.initialize() called
4. `STARTED` - Plugin.start() called
5. `STOPPED` - Plugin.stop() called
6. `DESTROYED` - Plugin.destroy() called (removed from registry)
7. `FAILED` - Plugin failed during lifecycle transition

**Rationale**:
- Clear state transitions
- Easy to debug plugin issues
- Supports lazy initialization

### Decision 3: Plugin Lookup Strategy

**Supported lookup methods**:
- `getPlugin(String pluginId)` - Get by unique ID
- `getPluginsByDbType(DbType dbType)` - Get all plugins for a database type
- `getPluginsByCapability(String capability)` - Get plugins with specific capability
- `getAllPlugins()` - Get all loaded plugins

**Rationale**:
- Covers common use cases
- Efficient with indexed caching (by ID, by DbType, by Capability)

### Decision 4: Thread Safety

**Choice**: Use `ConcurrentHashMap` for plugin registry

**Rationale**:
- PluginManager will be singleton in Spring context
- Multiple threads may access plugins concurrently
- Read-heavy workload (few writes after initialization)

### Decision 5: PluginManager Independence from Spring

**Choice**: 
- PluginManager is a standalone component, NOT a Spring Bean
- Application creates and manages PluginManager instance
- PluginManager can be used in non-Spring environments

**Rationale**:
- Plugin system is framework-agnostic
- Can be used in standalone applications, tests, or other frameworks
- Simpler design without Spring dependencies
- Application decides when to initialize/shutdown plugins

**Spring Integration (Application Layer)**:
- Application creates PluginManager instance and wraps it as a Bean if needed
- Application controls plugin lifecycle via PluginManager methods
- PluginContext implementation can use Spring resources

### Decision 6: Error Handling

**Strategy**:
- Plugin loading failures are logged but don't stop application startup
- Failed plugins are marked with `FAILED` state
- `getPlugin()` returns null for failed plugins
- `PluginLoadException` for critical failures

**Rationale**:
- Resilient to individual plugin failures
- Application can start even if some plugins fail
- Clear visibility of plugin health

## Component Design

### PluginManager Interface

```java
public interface PluginManager {
    // Lifecycle
    void loadPlugins();
    void initializeAll();
    void startAll();
    void stopAll();
    void destroyAll();
    
    // Lookup
    Plugin getPlugin(String pluginId);
    List<Plugin> getPluginsByDbType(DbType dbType);
    List<Plugin> getPluginsByCapability(String capability);
    List<Plugin> getAllPlugins();
    
    // State
    PluginState getPluginState(String pluginId);
    Map<String, PluginState> getAllPluginStates();
}
```

### DefaultPluginManager Implementation

**Key responsibilities**:
1. Use `ServiceLoader<Plugin>` for discovery
2. Maintain `ConcurrentHashMap<String, Plugin>` for registry
3. Maintain `ConcurrentHashMap<String, PluginState>` for states
4. Maintain indexed caches for efficient lookup
5. Coordinate lifecycle calls to all plugins
6. Handle and log plugin errors

### PluginRegistry (Internal Helper)

**Purpose**: Encapsulate plugin storage and indexing logic

**Features**:
- Primary index: `Map<String, Plugin>` (by plugin ID)
- Secondary index: `Map<DbType, List<Plugin>>` (by database type)
- Capability index: `Map<String, List<Plugin>>` (by capability)
- Thread-safe operations

## Risks / Trade-offs

### Risk 1: Plugin Loading Performance
- **Risk**: Loading many plugins may take time
- **Mitigation**: 
  - Application decides when to load (startup vs lazy)
  - Use lazy initialization (load on first access)
  - Parallel plugin loading (future enhancement)
  - Configuration to disable unwanted plugins

### Risk 2: Plugin Failure Impact
- **Risk**: One plugin failure might affect others
- **Mitigation**:
  - Isolate plugin lifecycle calls with try-catch
  - Continue loading other plugins even if one fails
  - Provide clear error messages and state tracking

### Risk 3: Memory Usage
- **Risk**: Caching all plugins in memory
- **Mitigation**:
  - Plugins are lightweight (mostly stateless)
  - Clear lifecycle management to release resources
  - Future: Support for plugin unloading if needed

## Migration Plan

### Phase 1: Add PluginManager (This Change)
1. Implement PluginManager interface and DefaultPluginManager
2. Update test code to use PluginManager
3. Provide example of Spring integration (optional)

### Phase 2: Application Integration (Future)
1. Use PluginManager in connection service
2. Use PluginManager in query service
3. Expose plugin information via REST API

### Rollback
- Remove PluginManager usage
- Revert to direct ServiceLoader usage
- No data migration needed (stateless)

## Open Questions

1. Should plugins be initialized eagerly (at startup) or lazily (on first use)?
   - **Recommendation**: Eager for CONNECTION capability (fail fast), lazy for others
   
2. Should PluginManager support plugin unloading?
   - **Recommendation**: Not in this change (YAGNI), add when needed

3. Should PluginManager be a singleton or allow multiple instances?
   - **Recommendation**: Application decides (typically singleton), but design allows multiple instances for testing or multi-tenant scenarios

