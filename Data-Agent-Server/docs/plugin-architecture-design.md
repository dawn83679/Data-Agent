# Plugin Architecture Design

## Overview

Data-Agent uses a **Java SPI-based pluggable architecture** to support multiple database types (SQL and NoSQL). This design allows easy extension for new database support without modifying the core application.

## Architecture Principles

### 1. Core Design Principles

- **Extensibility**: Add new database support by implementing plugin interfaces
- **Isolation**: Each plugin is independent and self-contained
- **Dynamic Loading**: Plugins are discovered and loaded at runtime via Java SPI
- **Capability-Based**: Plugins declare their capabilities through interfaces
- **Annotation-Driven**: Plugin metadata is declared via annotations for simplicity

### 2. Module Structure

```
data-agent-server (parent)
├── data-agent-server-plugin (Plugin API module)
│   ├── Plugin interfaces
│   ├── Capability interfaces
│   ├── Base classes
│   ├── Annotations
│   └── Utility classes
├── data-agent-server-plugins (Plugin implementations aggregation)
│   ├── mysql-plugin
│   ├── postgresql-plugin (future)
│   └── redis-plugin (future)
└── data-agent-server-app (Spring Boot application)
    └── Depends on data-agent-server-plugins
```

## Plugin API Design

### 1. Plugin Hierarchy

```java
Plugin (Top-level interface)
├── SqlPlugin (SQL database marker interface)
│   └── AbstractDatabasePlugin (Base implementation)
│       └── AbstractMysqlPlugin
│           ├── Mysql57Plugin
│           └── Mysql8Plugin
└── NoSqlPlugin (NoSQL database marker interface)
    └── AbstractDatabasePlugin (Base implementation)
        └── RedisPlugin (future)
```

### 2. Core Interfaces

#### 2.1 Plugin Interface

The top-level interface that all plugins must implement:

```java
public interface Plugin {
    // Plugin Identification
    String getPluginId();
    String getDisplayName();
    String getVersion();
    DbType getDbType();
    PluginType getPluginType();
    String getDescription();
    String getVendor();
    String getWebsite();
    
    // Database Version Support
    String getSupportMinVersion();
    String getSupportMaxVersion();
    
    // Capability Discovery
    Set<String> getSupportedCapabilities();
}
```

#### 2.2 SqlPlugin / NoSqlPlugin

Marker interfaces to distinguish plugin types:

```java
public interface SqlPlugin extends Plugin {}
public interface NoSqlPlugin extends Plugin {}
```

### 3. Annotation-Driven Metadata

#### 3.1 @PluginInfo

Declares plugin metadata on the plugin class:

```java
@PluginInfo(
    id = "mysql-8",
    name = "MySQL 8.0+",
    version = "0.0.1",
    dbType = DbType.MYSQL,
    description = "MySQL 8.0+ database plugin",
    supportMinVersion = "8.0.0"
)
public class Mysql8Plugin extends AbstractMysqlPlugin {
    // ...
}
```

#### 3.2 @CapabilityMarker

Marks capability interfaces for automatic discovery:

```java
@CapabilityMarker(CapabilityEnum.CONNECTION)
public interface ConnectionProvider {
    Connection connect(ConnectionConfig config) throws PluginException;
    boolean testConnection(ConnectionConfig config);
    void closeConnection(Connection connection) throws PluginException;
}
```

### 4. Base Classes

#### 4.1 AbstractDatabasePlugin

Provides default implementation for all database plugins:

**Features**:
- Automatically reads `@PluginInfo` annotation to implement metadata methods
- Automatically collects capabilities from implemented interfaces marked with `@CapabilityMarker`
- Lightweight and stateless design

**Usage**:
```java
@PluginInfo(/* metadata */)
public class Mysql8Plugin extends AbstractDatabasePlugin implements ConnectionProvider {
    // Only need to implement ConnectionProvider methods
    // Metadata methods are automatically handled by AbstractDatabasePlugin
}
```

## Capability System

### 1. Capability Definition

Capabilities are defined as enums:

```java
public enum CapabilityEnum {
    CONNECTION("CONNECTION", "Ability to establish and manage database connections");
    // More capabilities: QUERY, DDL, DML, METADATA, etc.
}
```

### 2. Capability Declaration

Plugins declare capabilities by implementing capability interfaces:

```java
// Capability interface
@CapabilityMarker(CapabilityEnum.CONNECTION)
public interface ConnectionProvider {
    // Methods...
}

// Plugin implementation
public class Mysql8Plugin extends AbstractMysqlPlugin implements ConnectionProvider {
    // Automatically has CONNECTION capability
}
```

### 3. Automatic Capability Discovery

`AbstractDatabasePlugin.getSupportedCapabilities()` automatically:
1. Scans all implemented interfaces
2. Collects interfaces annotated with `@CapabilityMarker`
3. Returns capability codes as a Set
4. Uses caching to avoid repeated reflection

## MySQL Plugin Implementation

### 1. Version-Specific Plugins

**Mysql57Plugin** (MySQL 5.7.x):
- Plugin ID: `mysql-5.7`
- Driver: `com.mysql.jdbc.Driver`
- Min Version: `5.7.0`
- Max Version: `7.9.99`

**Mysql8Plugin** (MySQL 8.0+):
- Plugin ID: `mysql-8`
- Driver: `com.mysql.cj.jdbc.Driver`
- Min Version: `8.0.0`
- Max Version: (empty - supports all future versions)

### 2. Shared Functionality

`AbstractMysqlPlugin` provides common functionality:
- JDBC URL template: `jdbc:mysql://%s:%d/%s`
- Default port: `3306`
- ConnectionProvider implementation (connect, test, close)

### 3. Connection Components

**Component-Based Design**:

1. **DriverLoader** (Utility class)
   - Dynamically loads JDBC drivers from external JAR files
   - Uses URLClassLoader with caching mechanism
   - Handles driver registration with DriverManager

2. **JdbcConnectionBuilder** (Interface)
   - `buildUrl()`: Constructs JDBC URL from configuration
   - `buildProperties()`: Builds connection properties (username, password, timeout, etc.)

3. **MysqlJdbcConnectionBuilder** (MySQL implementation)
   - Builds base URL: `jdbc:mysql://host:port/database`
   - Builds Properties with all connection parameters
   - Uses Apache Commons utilities (StringUtils, MapUtils)

### 4. Connection Configuration

**ConnectionConfig** with Bean Validation:

```java
@Data
@Builder
public class ConnectionConfig {
    @NotBlank(message = "Host cannot be null or empty")
    private String host;
    
    @Min(value = 1, message = "Port must be a positive integer")
    private Integer port;
    
    private String database;
    private String username;
    private String password;
    
    @NotBlank(message = "Driver JAR path cannot be null or empty")
    private String driverJarPath;  // Required: external JDBC driver JAR
    
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();  // Custom properties
    
    @Builder.Default
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeout = 30;
}
```

## Plugin Manager

### 1. Overview

PluginManager provides centralized plugin management with the following features:
- **Auto Loading**: Plugins are automatically loaded via SPI in static initializer
- **Plugin Lookup**: By database type or capability
- **Thread Safety**: Concurrent access with ConcurrentHashMap
- **Framework Independence**: Can be used in Spring or standalone applications
- **Lightweight**: Stateless design with minimal overhead

### 2. Usage Examples

#### Basic Usage

```java
// Create PluginManager (plugins auto-loaded)
PluginManager manager = new DefaultPluginManager();

// Query plugins by database type
List<Plugin> mysqlPlugins = manager.getPluginsByDbType(DbType.MYSQL);
// Returns: [Mysql8Plugin, Mysql57Plugin] (sorted by version, newest first)

// Query plugins by capability
List<Plugin> connectionPlugins = manager.getPluginsByCapability("CONNECTION");

// Get all plugins
List<Plugin> allPlugins = manager.getAllPlugins();
```

#### Spring Integration (Optional)

```java
@Configuration
public class PluginConfig {
    
    @Bean
    public PluginManager pluginManager() {
        return new DefaultPluginManager();
    }
}
```

#### Find Specific Plugin

```java
// Find MySQL 8 plugin
List<Plugin> mysqlPlugins = manager.getPluginsByDbType(DbType.MYSQL);
Plugin mysql8 = mysqlPlugins.stream()
    .filter(p -> "mysql-8".equals(p.getPluginId()))
    .findFirst()
    .orElse(null);
```

## SPI Service Discovery

### 1. Service Registration

Register plugins in `META-INF/services/edu.zsc.ai.plugin.Plugin`:

```
edu.zsc.ai.plugin.mysql.Mysql57Plugin
edu.zsc.ai.plugin.mysql.Mysql8Plugin
```

### 2. Plugin Loading and Management

Use PluginManager for centralized plugin management:

```java
// Create PluginManager (plugins auto-loaded in static initializer)
PluginManager pluginManager = new DefaultPluginManager();

// Query plugins by database type
List<Plugin> mysqlPlugins = pluginManager.getPluginsByDbType(DbType.MYSQL);

// Query plugins by capability
List<Plugin> connectionPlugins = pluginManager.getPluginsByCapability("CONNECTION");

// Get all plugins
List<Plugin> allPlugins = pluginManager.getAllPlugins();
```

### 3. Application Integration

The `data-agent-server-app` module depends on `data-agent-server-plugins`:

```xml
<dependency>
    <groupId>edu.zsc.ai</groupId>
    <artifactId>data-agent-server-plugins</artifactId>
    <version>${project.version}</version>
    <type>pom</type>
</dependency>
```

This automatically includes all plugin implementations.

## Dependency Management

### 1. Parent POM

Centralized version management and common dependencies:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Jackson, Apache Commons, Validation API, etc. -->
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- All submodules automatically inherit: -->
    <!-- Lombok, Commons Lang3, Commons Collections4, Bean Validation -->
</dependencies>
```

### 2. Plugin Module Dependencies

Plugin implementations only need to declare:
- Dependency on `data-agent-server-plugin`
- Test dependencies

All utility libraries are inherited from parent.

## Adding New Database Support

### Step 1: Create Plugin Module

Create a new module under `data-agent-server-plugins/`:

```
data-agent-server-plugins/
└── postgresql-plugin/
    ├── pom.xml
    └── src/main/java/edu/zsc/ai/plugin/postgresql/
```

### Step 2: Add to Parent POM

Update `data-agent-server-plugins/pom.xml`:

```xml
<modules>
    <module>mysql-plugin</module>
    <module>postgresql-plugin</module>  <!-- New -->
</modules>

<dependencies>
    <dependency>
        <groupId>edu.zsc.ai</groupId>
        <artifactId>postgresql-plugin</artifactId>  <!-- New -->
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

### Step 3: Implement Plugin Class

```java
@PluginInfo(
    id = "postgresql",
    name = "PostgreSQL",
    version = "0.0.1",
    dbType = DbType.POSTGRESQL,
    description = "PostgreSQL database plugin",
    supportMinVersion = "12.0"
)
public class PostgresqlPlugin extends AbstractDatabasePlugin 
        implements SqlPlugin, ConnectionProvider {
    
    // Implement ConnectionProvider methods
}
```

### Step 4: Register via SPI

Create `META-INF/services/edu.zsc.ai.plugin.Plugin`:

```
edu.zsc.ai.plugin.postgresql.PostgresqlPlugin
```

### Step 5: Implement Capabilities

Implement required capability interfaces (e.g., `ConnectionProvider`).

The plugin is now automatically available to the application!

## Component Reusability

### Reusable Components

1. **DriverLoader** - Works for all JDBC-based databases
2. **ConnectionConfig** - Generic connection configuration
3. **AbstractDatabasePlugin** - Base class for all plugins
4. **PluginManager** - Centralized plugin management

### Database-Specific Components

Each database plugin needs to implement:
- `JdbcConnectionBuilder` - Database-specific URL and Properties construction
- Capability interfaces (e.g., `ConnectionProvider`)

## Testing

### 1. SPI Loading Test

Verify plugin discovery and capability collection:

```java
ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
for (Plugin plugin : loader) {
    assertNotNull(plugin);
    assertTrue(plugin.getSupportedCapabilities().contains("CONNECTION"));
}
```

### 2. Connection Test

Verify connection functionality:

```java
ConnectionConfig config = ConnectionConfig.builder()
    .host("localhost")
    .port(3306)
    .username("root")
    .password("password")
    .driverJarPath("/path/to/mysql-connector.jar")
    .build();

Connection conn = plugin.connect(config);
assertNotNull(conn);
assertFalse(conn.isClosed());
```

## Error Handling

### 1. Unified Exception Handling

All plugin errors use `PluginException` with error codes:

```java
public class PluginErrorCode {
    public static final String PLUGIN_ERROR = "PLUGIN_0001";
    public static final String CONNECTION_FAILED = "PLUGIN_0008";
    // More error codes...
}
```

### 2. Usage Example

```java
try {
    connection = plugin.connect(config);
} catch (PluginException e) {
    System.err.println("Error Code: " + e.getErrorCode());
    System.err.println("Message: " + e.getMessage());
}
```

## Future Enhancements

### Planned Capabilities

- **QueryExecutor**: Execute SQL queries
- **DdlExecutor**: Execute DDL statements (CREATE, ALTER, DROP)
- **DmlExecutor**: Execute DML statements (INSERT, UPDATE, DELETE)
- **MetadataProvider**: Retrieve database metadata (tables, columns, indexes)
- **SchemaManager**: Manage database schemas
- **ConnectionPoolProvider**: Connection pool management

### Planned Plugins

- PostgreSQL Plugin
- Oracle Plugin
- SQL Server Plugin
- Redis Plugin
- MongoDB Plugin
- Elasticsearch Plugin

## Design Guidelines

### 1. Single Responsibility

Each component has a single, well-defined responsibility:
- `DriverLoader` - Load drivers only
- `JdbcConnectionBuilder` - Build connection parameters only
- `ConnectionProvider` - Manage connections only

### 2. Interface Segregation

Capabilities are defined as fine-grained interfaces:
- Plugins implement only the capabilities they support
- Applications can query capabilities before usage

### 3. Dependency Inversion

- Application depends on plugin interfaces (abstraction)
- Plugin implementations depend on interfaces
- No direct dependency on concrete plugin classes

### 4. Open/Closed Principle

- Open for extension: Easy to add new plugins
- Closed for modification: No need to modify existing code

## Best Practices

### 1. Plugin Development

- Always extend `AbstractDatabasePlugin` for automatic metadata handling
- Use `@PluginInfo` annotation for metadata declaration
- Implement only the capabilities your database supports
- Use component-based design for connection logic
- Follow Java design guidelines (no magic values, use constants)

### 2. Capability Implementation

- Mark capability interfaces with `@CapabilityMarker`
- Keep capability interfaces focused and cohesive
- Provide clear documentation for each method
- Handle errors with specific error codes

### 3. Testing

- Test SPI loading and capability discovery
- Test each capability independently
- Test with real database connections
- Test error handling scenarios

### 4. Documentation

- All comments and documentation in English
- Provide clear Javadoc for public APIs
- Document configuration requirements
- Include usage examples

## References

- Java SPI Documentation: https://docs.oracle.com/javase/tutorial/ext/basics/spi.html
- Bean Validation Specification: https://beanvalidation.org/
- JDBC API Documentation: https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/

