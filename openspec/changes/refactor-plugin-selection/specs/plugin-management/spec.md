## MODIFIED Requirements

### Requirement: Plugin Lookup by Database Type

The system SHALL provide plugin lookup by database type to support multiple versions of the same database.

#### Scenario: Get all MySQL plugins
- **WHEN** PluginManager.getPluginsByDbType(DbType.MYSQL) is called
- **THEN** PluginManager SHALL return a list containing both Mysql57Plugin and Mysql8Plugin
- **AND** plugins SHALL be ordered by version (newest first)

#### Scenario: Get plugins for database type with no plugins
- **WHEN** PluginManager.getPluginsByDbType(DbType.POSTGRESQL) is called and no PostgreSQL plugins exist
- **THEN** PluginManager SHALL return an empty list
- **AND** no exception SHALL be thrown

## ADDED Requirements

### Requirement: Select Single Plugin for Database Type

The system SHALL provide a method to select the most appropriate single plugin for a database type, eliminating the need for application layer to handle List selection logic.

#### Scenario: Select plugin without database version
- **WHEN** PluginManager.selectPluginForDbType("mysql") is called without database version
- **THEN** PluginManager SHALL return the newest version plugin (Mysql8Plugin)
- **AND** the returned plugin SHALL be a single Plugin instance, not a List
- **AND** plugins SHALL be selected based on version ordering (newest first)

#### Scenario: Select plugin with database version
- **WHEN** PluginManager.selectPluginForDbType("mysql", "8.0.33") is called with database version
- **THEN** PluginManager SHALL return the plugin that best matches the database version
- **AND** if a plugin explicitly supports the version range, that plugin SHALL be returned
- **AND** if no plugin explicitly supports the version, the newest version plugin SHALL be returned as fallback
- **AND** the returned plugin SHALL be a single Plugin instance, not a List

#### Scenario: Select plugin for unknown database type
- **WHEN** PluginManager.selectPluginForDbType("unknown") is called for a database type with no plugins
- **THEN** PluginManager SHALL throw PluginException with error code PLUGIN_NOT_FOUND
- **AND** error message SHALL indicate "No plugin available for database type: unknown"

#### Scenario: Select plugin with empty database type code
- **WHEN** PluginManager.selectPluginForDbType("") or selectPluginForDbType(null) is called
- **THEN** PluginManager SHALL throw PluginException with error code PLUGIN_NOT_FOUND
- **AND** error message SHALL indicate that database type code cannot be empty

#### Scenario: Select plugin reuses existing version matching logic
- **WHEN** PluginManager.selectPluginForDbType("mysql", "8.0.33") is called
- **THEN** PluginManager SHALL internally use the same version compatibility checking logic as selectPluginByDbVersion
- **AND** version comparison SHALL use semantic versioning (e.g., "5.7.0" vs "8.0.0")
- **AND** plugins with matching version ranges SHALL be preferred over fallback selection

