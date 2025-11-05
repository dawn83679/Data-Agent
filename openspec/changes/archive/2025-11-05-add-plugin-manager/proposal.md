# Add Plugin Manager

## Why

Currently, plugins are loaded directly using `ServiceLoader.load(Plugin.class)` in test code, but there is no centralized component to manage plugin lifecycle, provide plugin lookup, and cache loaded plugins. A PluginManager is needed to provide a unified interface for plugin management across the application.

## What Changes

- Add `PluginManager` interface in `data-agent-server-plugin` module (framework-agnostic)
- Add `DefaultPluginManager` implementation using Java SPI
- Support plugin loading, initialization, and lifecycle management
- Provide plugin lookup by ID, database type, and capabilities
- Cache loaded plugins for performance
- PluginManager is standalone (not Spring-managed), can be used in any environment
- Application layer optionally wraps PluginManager as Spring Bean
- Add comprehensive tests for PluginManager functionality

## Impact

- **Affected specs**: `plugin-management` (new capability)
- **Affected code**:
  - New: `edu.zsc.ai.plugin.manager.PluginManager` interface
  - New: `edu.zsc.ai.plugin.manager.DefaultPluginManager` implementation
  - New: `edu.zsc.ai.plugin.manager.PluginRegistry` (internal)
  - Modified: `data-agent-server-app` to use PluginManager (optional Spring Bean wrapper)
  - Modified: Test code to use PluginManager instead of direct ServiceLoader
- **Dependencies**: None (uses existing Plugin API, no Spring dependency in plugin module)
- **Breaking changes**: None (additive only)

