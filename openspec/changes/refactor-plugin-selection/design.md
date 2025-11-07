# Plugin Selection API Refactoring Design

## Context

当前PluginManager提供了多个方法来查询plugins：
- `getPluginsByDbType(DbType)` - 返回List<Plugin>
- `getPluginsByDbTypeCode(String)` - 返回List<Plugin>
- `selectPluginByDbVersion(String, String)` - 返回单个Plugin，但需要先知道数据库版本

应用层在使用时面临的问题：
1. 需要处理List，即使大多数情况下只需要一个plugin
2. 需要先建立连接获取版本，才能选择正确的plugin
3. 选择逻辑分散在应用层，违反了分层原则

## Goals / Non-Goals

### Goals
- 提供简洁的API，让应用层直接获取最合适的单个Plugin
- 封装版本选择逻辑到PluginManager内部
- 保持向后兼容，不破坏现有代码

### Non-Goals
- 不改变Plugin接口
- 不改变SPI加载机制
- 不改变plugin的版本匹配逻辑（仍然使用现有的isVersionCompatible方法）

## Decisions

### Decision: 添加selectPluginForDbType方法返回单个Plugin

**What**: 在PluginManager中添加两个重载方法：
- `selectPluginForDbType(String dbTypeCode)` - 当不知道数据库版本时，返回最新版本的plugin
- `selectPluginForDbType(String dbTypeCode, String databaseVersion)` - 当知道数据库版本时，返回最匹配的plugin

**Why**: 
- 应用层大多数情况下只需要一个plugin，不需要处理List
- 封装版本选择逻辑，让应用层更简单
- 保持API一致性（都返回Plugin而不是List）

**Alternatives considered**:
1. **适配器模式** - 创建一个PluginAdapter类来封装选择逻辑
   - 拒绝原因：增加了不必要的抽象层，直接在PluginManager中添加方法更直接
2. **策略模式** - 创建不同的选择策略
   - 拒绝原因：当前选择逻辑简单，不需要策略模式
3. **工厂模式** - 创建PluginFactory来创建/选择plugin
   - 拒绝原因：plugin已经通过SPI加载，不需要工厂创建

### Decision: 保留现有方法以保持向后兼容

**What**: 保留`getPluginsByDbType()`和`selectPluginByDbVersion()`方法

**Why**: 
- 可能有其他代码依赖这些方法
- 某些场景可能需要获取所有plugins（如列出所有可用plugins）
- 渐进式迁移，不强制一次性修改所有代码

### Decision: 内部复用现有逻辑

**What**: `selectPluginForDbType()`内部调用现有的`getPluginsByDbTypeCode()`和`selectPluginByDbVersion()`

**Why**: 
- 避免重复代码
- 保持逻辑一致性
- 如果未来需要修改选择逻辑，只需要修改一处

## Implementation Details

### selectPluginForDbType(String dbTypeCode)

```java
public static Plugin selectPluginForDbType(String dbTypeCode) {
    List<Plugin> plugins = getPluginsByDbTypeCode(dbTypeCode);
    if (plugins.isEmpty()) {
        throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                "No plugin available for database type: " + dbTypeCode);
    }
    // 返回最新版本的plugin（列表已经按版本排序，newest first）
    return plugins.get(0);
}
```

### selectPluginForDbType(String dbTypeCode, String databaseVersion)

```java
public static Plugin selectPluginForDbType(String dbTypeCode, String databaseVersion) {
    // 复用现有的selectPluginByDbVersion逻辑
    return selectPluginByDbVersion(dbTypeCode, databaseVersion);
}
```

## Risks / Trade-offs

### Risk: 应用层可能仍然需要List
**Mitigation**: 保留`getPluginsByDbType()`方法，应用层可以选择使用哪个方法

### Risk: 性能影响
**Mitigation**: 新方法内部复用现有逻辑，没有额外的性能开销

### Risk: API混乱（既有返回List的方法，又有返回单个Plugin的方法）
**Mitigation**: 
- 方法命名清晰：`getPluginsByDbType`明确返回List，`selectPluginForDbType`明确返回单个Plugin
- 文档说明使用场景

## Migration Plan

1. **Phase 1**: 添加新方法到PluginManager
2. **Phase 2**: 修改ConnectionServiceImpl使用新方法
3. **Phase 3**: 修改DriverServiceImpl使用新方法
4. **Phase 4**: 验证所有功能正常
5. **Phase 5**: （可选）未来可以考虑废弃返回List的方法，但当前保持兼容

## Open Questions

- 是否需要添加`selectConnectionProviderForDbType()`方法，直接返回ConnectionProvider？
  - 当前方案：先selectPlugin，再cast到ConnectionProvider
  - 备选方案：添加专门的方法返回ConnectionProvider
  - 决定：暂时不添加，保持API简洁，如果未来有需求再添加

