## 1. Implementation

- [x] 1.1 Add selectPluginForDbType(String dbTypeCode) method to PluginManager
  - [x] 1.1.1 Implement method that calls getPluginsByDbTypeCode internally
  - [x] 1.1.2 Return first plugin from sorted list (newest first)
  - [x] 1.1.3 Throw PluginException if no plugins found
  - [x] 1.1.4 Add Javadoc documentation

- [x] 1.2 Add selectPluginForDbType(String dbTypeCode, String databaseVersion) method to PluginManager
  - [x] 1.2.1 Implement method that calls selectPluginByDbVersion internally
  - [x] 1.2.2 Add Javadoc documentation
  - [x] 1.2.3 Ensure it reuses existing version matching logic

- [x] 1.3 Refactor ConnectionServiceImpl to use new methods
  - [x] 1.3.1 Update testConnection() method to use selectPluginForDbType()
  - [x] 1.3.2 Update openConnection() method to use selectPluginForDbType() with version
  - [x] 1.3.3 Simplify plugin selection logic (remove List handling)
  - [x] 1.3.4 Remove unnecessary connection reconnection logic if plugin matches

- [x] 1.4 Refactor DriverServiceImpl to use new methods
  - [x] 1.4.1 Update downloadDriver() method to use selectPluginForDbType()
  - [x] 1.4.2 Update listAvailableDrivers() method to use selectPluginForDbType()
  - [x] 1.4.3 Simplify plugin selection logic

## 2. Testing

- [ ] 2.1 Add unit tests for selectPluginForDbType(String dbTypeCode)
  - [ ] 2.1.1 Test with valid database type (should return newest plugin)
  - [ ] 2.1.2 Test with unknown database type (should throw exception)
  - [ ] 2.1.3 Test with empty/null database type code (should throw exception)
  - [ ] 2.1.4 Test that returned plugin is not null and is a single instance

- [ ] 2.2 Add unit tests for selectPluginForDbType(String dbTypeCode, String databaseVersion)
  - [ ] 2.2.1 Test with matching version (should return matching plugin)
  - [ ] 2.2.2 Test with non-matching version (should return fallback plugin)
  - [ ] 2.2.3 Test with null version (should return newest plugin)
  - [ ] 2.2.4 Test version compatibility logic matches selectPluginByDbVersion

- [ ] 2.3 Add integration tests for ConnectionServiceImpl
  - [ ] 2.3.1 Test testConnection() with new plugin selection
  - [ ] 2.3.2 Test openConnection() with MySQL 8.0 (should select Mysql8Plugin)
  - [ ] 2.3.3 Test openConnection() with MySQL 5.7 (should select Mysql57Plugin)
  - [ ] 2.3.4 Verify no unnecessary reconnections occur

- [ ] 2.4 Add integration tests for DriverServiceImpl
  - [ ] 2.4.1 Test downloadDriver() with new plugin selection
  - [ ] 2.4.2 Test listAvailableDrivers() with new plugin selection

## 3. Documentation

- [x] 3.1 Update PluginManager Javadoc
  - [x] 3.1.1 Document selectPluginForDbType methods
  - [x] 3.1.2 Explain when to use selectPluginForDbType vs getPluginsByDbType
  - [x] 3.1.3 Add usage examples

- [ ] 3.2 Update plugin-architecture-design.md if needed
  - [ ] 3.2.1 Document new plugin selection API
  - [ ] 3.2.2 Update usage examples

## 4. Validation

- [ ] 4.1 Run openspec validate refactor-plugin-selection --strict
- [ ] 4.2 Fix any validation errors
- [x] 4.3 Verify all existing tests still pass
- [x] 4.4 Verify backward compatibility (existing getPluginsByDbType still works)

