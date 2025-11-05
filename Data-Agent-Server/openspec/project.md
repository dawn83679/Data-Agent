# Project Context

## Purpose

Data-Agent is a web-based database management tool designed to support a wide variety of SQL and NoSQL databases. The goal is to provide a unified interface for database operations, similar to tools like Navicat, DataGrip, and DBeaver, but with a modern web-based architecture and a highly extensible plugin system.

**Key Goals**:
- Support multiple database types through a pluggable architecture
- Provide comprehensive database management capabilities (CRUD, metadata, DDL, DML)
- Enable dynamic JDBC driver loading without bundling drivers
- Offer a beautiful and modern web UI with best UX practices
- Maintain high code quality and international standards

## Tech Stack

### Backend
- **Java 17** - Primary backend language
- **Spring Boot 3.5.6** - Application framework
- **Maven** - Build and dependency management
- **Java SPI** - Plugin loading mechanism
- **JDBC** - Database connectivity
- **Bean Validation (Jakarta Validation API)** - Parameter validation
- **Hibernate Validator** - Validation implementation

### Frontend
- **Vue 3** - Frontend framework
- **TypeScript** - Type-safe JavaScript
- **Vite** - Build tool and dev server

### Utilities
- **Lombok** - Reduce boilerplate code
- **Apache Commons Lang3** - String and object utilities
- **Apache Commons Collections4** - Collection utilities
- **Jackson** - JSON processing

## Project Conventions

### Code Style

#### Java
- **Package naming**: `edu.zsc.ai.[module].[layer]`
- **Class naming**: PascalCase (e.g., `AbstractDatabasePlugin`)
- **Method naming**: camelCase (e.g., `getDriverClassName`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `PLUGIN_ERROR_CODE`)
- **All comments in English** for internationalization

#### Mandatory Patterns
- **No magic values**: All constants must be defined in constant classes
- **No string literals**: Error messages, fixed strings must use constants
- **Use utility classes**: Prefer Apache Commons over manual null checks
  - `StringUtils.isBlank()` instead of `str == null || str.isEmpty()`
  - `MapUtils.isNotEmpty()` instead of `map != null && !map.isEmpty()`

#### Documentation
- All public APIs must have Javadoc comments
- Comments must include `@param`, `@return`, `@throws` where applicable
- Class-level comments must describe purpose and usage

### Architecture Patterns

#### Multi-Module Maven Structure
```
data-agent-server (parent)
├── data-agent-server-plugin (API definitions)
├── data-agent-server-plugins (implementation aggregator)
│   └── [specific-plugin] (e.g., mysql-plugin)
└── data-agent-server-app (Spring Boot application)
```

#### Plugin Architecture
- **Java SPI-based**: Plugins discovered via `ServiceLoader`
- **Annotation-driven metadata**: Use `@PluginInfo` for plugin information
- **Capability-based design**: Plugins implement capability interfaces (e.g., `ConnectionProvider`)
- **Automatic capability discovery**: Use `@CapabilityMarker` on capability interfaces
- **Version range support**: Plugins specify min/max database versions

#### Design Principles
- **Single Responsibility**: Each class has one clear purpose
- **Interface Segregation**: Fine-grained capability interfaces
- **Dependency Inversion**: Depend on abstractions, not concrete classes
- **Open/Closed**: Open for extension (new plugins), closed for modification

#### Component-Based Design
- Break complex operations into reusable components
- Use utility classes for common operations (validation, driver loading)
- Use interfaces for database-specific implementations (URL building, property building)

### Testing Strategy

#### Unit Tests
- Test each component independently
- Use JUnit 5 for all tests
- Test files located in `src/test/java/` mirroring main structure

#### Integration Tests
- Test SPI loading mechanism
- Test plugin lifecycle
- Test database connections with real databases (Docker)

#### Test Naming
- Test class: `[ClassName]Test` (e.g., `Mysql8PluginConnectionTest`)
- Test method: `test[Functionality]` (e.g., `testConnect`, `testConnectionWithProperties`)

#### Coverage Requirements
- All public APIs must be tested
- Critical paths must have positive and negative test cases
- Connection tests must verify success and failure scenarios

### Git Workflow

#### Branch Strategy
- `main` - Main development branch
- Feature branches: `feature/[feature-name]`
- Bugfix branches: `fix/[issue-description]`

#### Commit Conventions
- **Format**: `<type>: <description>`
- **Types**:
  - `feat`: New feature
  - `fix`: Bug fix
  - `refactor`: Code refactoring
  - `docs`: Documentation changes
  - `test`: Test additions/changes
  - `chore`: Maintenance tasks

#### Commit Message Guidelines
- First line: Concise summary (50 chars max)
- Body: Detailed explanation of changes
- List major changes with bullet points
- Reference related issues/PRs

## Domain Context

### Database Management Domain

**Core Concepts**:
- **Plugin**: A module that provides support for a specific database type
- **Capability**: A functional feature that a plugin can provide (e.g., CONNECTION, QUERY, METADATA)
- **Driver**: JDBC driver JAR file loaded dynamically at runtime
- **Connection Config**: Configuration required to establish database connection

**Database Types**:
- **SQL Databases**: MySQL, PostgreSQL, Oracle, SQL Server, etc.
- **NoSQL Databases**: Redis, MongoDB, Elasticsearch, etc.

**Plugin Versioning**:
- Plugins specify supported database version ranges
- `minDatabaseVersion`: Minimum supported version (e.g., "5.7.0")
- `maxDatabaseVersion`: Maximum supported version, empty means all future versions

### Business Logic

**Plugin Lifecycle**:
1. `initialize()` - Load and configure plugin
2. `start()` - Activate plugin capabilities
3. `stop()` - Deactivate plugin
4. `destroy()` - Clean up resources

**Connection Management**:
1. Validate configuration
2. Load JDBC driver from external JAR
3. Build JDBC URL
4. Build connection properties
5. Establish connection
6. Test/use connection
7. Close connection

## Important Constraints

### Technical Constraints
- **Java 17 minimum** - Required for modern language features
- **No bundled JDBC drivers** - All drivers loaded dynamically from external JARs
- **Bean Validation required** - Use annotations for parameter validation
- **English-only code** - All comments, docs, and messages in English

### Design Constraints
- **Follow Java design guidelines** (see `docs/java-design-guidelines.md`)
  - Use constants for error messages
  - Use Apache Commons utilities
  - No magic values
  - Proper layering (Entity, Mapper, Service, Controller)

### Security Constraints
- Sensitive data (passwords) must not be logged
- Connection configs may contain credentials - handle securely
- Driver JARs loaded from trusted sources only

### Performance Constraints
- Driver ClassLoader caching to avoid repeated loading
- Connection pooling (future requirement)
- Metadata caching (future requirement)

## External Dependencies

### Runtime Dependencies
- **MySQL JDBC Driver** - Provided by user as external JAR
- **PostgreSQL JDBC Driver** - Future support
- Other database drivers loaded dynamically

### Development Tools
- **Maven 3.8+** - Build tool
- **JUnit 5** - Testing framework
- **Docker** - For running test databases

### External Services
- None currently - fully self-contained application

## Plugin Development Guidelines

### Adding a New Database Plugin

1. Create module: `data-agent-server-plugins/[database]-plugin/`
2. Extend `AbstractDatabasePlugin`
3. Add `@PluginInfo` annotation with metadata
4. Implement capability interfaces (e.g., `ConnectionProvider`)
5. Register via SPI: `META-INF/services/edu.zsc.ai.plugin.Plugin`
6. Add to parent `data-agent-server-plugins/pom.xml`
7. Write comprehensive tests

### Capability Development Guidelines

1. Define capability interface with `@CapabilityMarker`
2. Add capability to `CapabilityEnum`
3. Document capability contract clearly
4. Provide at least one reference implementation
5. Write tests for capability

### Code Review Checklist
- [ ] All comments in English
- [ ] No magic values or string literals
- [ ] Uses Apache Commons utilities
- [ ] Bean Validation annotations where applicable
- [ ] Proper error handling with error codes
- [ ] Comprehensive tests
- [ ] Javadoc on public APIs
