# Refactor Plugin Selection API

## Why

当前PluginManager返回List<Plugin>的结构导致应用层需要处理复杂的版本选择逻辑。例如，在ConnectionServiceImpl中，应用层需要：
1. 先调用`getConnectionProvider(dbType)`获取第一个可用的provider
2. 建立连接获取数据库版本
3. 再调用`selectPluginByDbVersion()`选择正确的plugin
4. 如果plugin不匹配，需要关闭连接并重新连接

这个过程繁琐且违反了分层原则：应用层不应该了解plugin版本选择的细节。用户希望plugin层能够自动处理版本选择，比如MySQL8直接返回Mysql8Plugin，而不需要应用层处理List和选择逻辑。

## What Changes

- **MODIFIED**: PluginManager添加新的便捷方法，返回单个Plugin而不是List：
  - `selectPluginForDbType(String dbTypeCode)` - 返回最适合的单个Plugin（最新版本）
  - `selectPluginForDbType(String dbTypeCode, String databaseVersion)` - 根据数据库版本返回最匹配的Plugin
- **MODIFIED**: 简化应用层代码，使用新的selectPluginForDbType方法替代复杂的List处理逻辑
- **KEEP**: 保留现有的`getPluginsByDbType()`方法以保持向后兼容
- **KEEP**: 保留现有的`selectPluginByDbVersion()`方法，但内部会被新方法使用

## Impact

- **Affected specs**: `plugin-management` (MODIFIED)
- **Affected code**:
  - Modified: `data-agent-server-plugin/src/main/java/edu/zsc/ai/plugin/manager/PluginManager.java` (添加selectPluginForDbType方法)
  - Modified: `data-agent-server-app/src/main/java/edu/zsc/ai/service/impl/ConnectionServiceImpl.java` (简化plugin选择逻辑)
  - Modified: `data-agent-server-app/src/main/java/edu/zsc/ai/service/impl/DriverServiceImpl.java` (简化plugin选择逻辑)

