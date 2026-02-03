# Project Context

## Purpose

Data-Agent is a web-based database management tool similar to Navicat, DataGrip, and DBeaver. It provides a unified interface for managing multiple database types (SQL and NoSQL) through a pluggable architecture.

**Goals**:
- Support a wide variety of SQL databases (MySQL, PostgreSQL, Oracle, SQL Server, etc.)
- Support NoSQL databases (Redis, MongoDB, Elasticsearch, etc.)
- Provide a modern, beautiful web UI with best UX practices
- Enable easy extension through a plugin-based architecture
- Open source version with potential enterprise version in the future

## Tech Stack

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.6
- **Build Tool**: Maven (multi-module project)
- **Plugin System**: Java SPI (Service Provider Interface)
- **Validation**: Jakarta Bean Validation API 3.0.2 + Hibernate Validator 8.0.1
- **Utilities**: 
  - Lombok (code generation)
  - Apache Commons Lang3 (string/object utilities)
  - Apache Commons Collections4 (collection utilities)
  - Jackson 2.18.2 (JSON processing)

### Frontend
- **Framework**: Vue.js 3
- **Build Tool**: Vite
- **Language**: TypeScript
- **Testing**: Vitest

### Database Support (Plugin-based)
- MySQL 5.7.x (plugin: mysql-5.7)
- MySQL 8.0+ (plugin: mysql-8)
- Future: PostgreSQL, Oracle, SQL Server, Redis, MongoDB, etc.

## Project Conventions

### Code Style

#### Java
- **Package Naming**: `edu.zsc.ai.[module]`
- **Class Naming**: PascalCase
- **Method Naming**: camelCase
- **Constants**: UPPER_SNAKE_CASE
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: 120 characters recommended
- **Documentation**: All comments and Javadoc in English for internationalization

#### Naming Conventions
- **Entity**: `User`, `Order` (PascalCase)
- **Service Interface**: `UserService`, `OrderService`
- **Service Implementation**: `UserServiceImpl`, `OrderServiceImpl`
- **Mapper**: `UserMapper`, `OrderMapper`
- **Controller**: `UserController`, `OrderController`
- **Request DTO**: `CreateUserRequest`, `QueryUserListRequest`
- **Response DTO**: `UserResponse`, `OrderResponse`
- **Constant Class**: `ResponseConstant`, `RedisKeyConstant`
- **Enum**: `DeleteStatusEnum`, `OrderStatusEnum` (suffix: `Enum`)
- **Util Class**: `FileUtil`, `RedisUtil` (suffix: `Util`)

#### Lombok Usage
- Use `@Data` for DTOs and entities
- Use `@Builder` for configuration objects
- Use `@Slf4j` for logging
- Use `@RequiredArgsConstructor` for dependency injection

### Architecture Patterns

#### Multi-Module Maven Structure
```
data-agent-server (parent)
├── data-agent-server-plugin (Plugin API)
├── data-agent-server-plugins (Plugin implementations)
│   └── mysql-plugin
└── data-agent-server-app (Spring Boot application)
```

#### Plugin Architecture
- **SPI-based**: Java Service Provider Interface for plugin discovery
- **Annotation-driven**: Use `@PluginInfo` for plugin metadata
- **Capability-based**: Plugins declare capabilities (CONNECTION, QUERY, DDL, etc.)
- **Component-based**: Reusable components (DriverLoader, JdbcConnectionBuilder)

#### Design Patterns
- **Strategy + Registry**: For type-based handlers
- **Decorator**: For cross-cutting concerns (caching, logging)
- **Builder**: For complex object construction
- **Factory**: For plugin instantiation

#### Layered Architecture
```
Controller → Service → Mapper → Database
         ↓
    Validation
         ↓
    Exception Handling
```

#### Database Layer (Five-piece Set)
1. Entity - Database entity class
2. Mapper - Data access interface (MyBatis-Plus)
3. Mapper XML - SQL mapping file
4. Service - Business interface
5. ServiceImpl - Business implementation

### Testing Strategy

#### Test Types
- **Unit Tests**: JUnit 5 for business logic
- **Integration Tests**: Spring Boot Test for API endpoints
- **Plugin Tests**: Verify SPI loading and capability discovery
- **Connection Tests**: Test real database connections

#### Test Naming
- Test class: `[ClassName]Test`
- Test method: `test[MethodName]` or descriptive names like `testConnectWithDatabase`

#### Test Requirements
- All public APIs must have tests
- All plugins must have SPI loading tests
- Connection capabilities must have real connection tests
- Edge cases and error handling must be tested

#### Test Organization
- Unit tests in same package as tested class
- Integration tests in `src/test/java/[package]/integration`
- Plugin tests in plugin module's `src/test/java`

### Git Workflow

#### Branch Strategy
- `main` - Main development branch
- Feature branches: `feature/[feature-name]`
- Bug fixes: `fix/[bug-name]`

#### Commit Conventions
Follow Conventional Commits:
- `feat:` - New features
- `fix:` - Bug fixes
- `refactor:` - Code refactoring
- `docs:` - Documentation changes
- `test:` - Test additions/changes
- `chore:` - Build/dependency changes

**Format**: `<type>: <description>`

Example: `feat: Implement pluggable architecture with Java SPI and MySQL plugin support`

## Domain Context

### Database Management Domain
- **Connection Management**: Establish, test, and close database connections
- **Query Execution**: Execute SELECT, INSERT, UPDATE, DELETE statements
- **DDL Operations**: CREATE, ALTER, DROP tables/schemas
- **Metadata Management**: Retrieve table/column/index information
- **Schema Management**: Manage database schemas and migrations
- **Data Export/Import**: Export data to various formats, import from files

### Plugin Capabilities
- **CONNECTION**: Establish and manage database connections
- **QUERY**: Execute SQL queries (future)
- **DDL**: Execute DDL statements (future)
- **DML**: Execute DML statements (future)
- **METADATA**: Retrieve database metadata (future)
- **SCHEMA**: Manage database schemas (future)

## Important Constraints

### Technical Constraints
- **Java Version**: Java 17 (LTS)
- **Spring Boot Version**: 3.5.6
- **No Magic Values**: All constants must be defined in constant classes
- **No Direct String Usage**: Error messages must use constants
- **No Magic Numbers**: Use enums for status codes and flags
- **Internationalization**: All code comments and documentation in English
- **Driver Loading**: JDBC drivers must be loaded from external JAR files (not in classpath)

### Design Guidelines
- **See**: `docs/java-design-guidelines.md` for detailed Java coding standards
- **See**: `docs/db-design-guidelines.md` for database design standards
- **See**: `docs/plugin-architecture-design.md` for plugin architecture details

### Code Quality Requirements
- Follow SOLID principles
- Use Apache Commons utilities (prefer over custom implementations)
- No business logic in Controllers (Service layer only)
- All DTOs must use Builder pattern
- All exceptions must use specific error codes
- No direct Entity returns from Controllers (use Response DTOs)

### Security Constraints
- Never expose sensitive information in logs or error messages
- Use prepared statements to prevent SQL injection
- Validate all user inputs with Bean Validation
- Password fields must never be logged

## External Dependencies

### Required External Components
- **JDBC Drivers**: External JAR files for database connectivity
  - MySQL Connector/J (for MySQL plugins)
  - PostgreSQL JDBC Driver (future)
  - etc.

### Optional External Services
- None currently (future: authentication service, monitoring, etc.)

### Development Tools
- **IDE**: IntelliJ IDEA / Cursor
- **Docker**: For running test databases
- **Maven**: For dependency management and building

## OpenSpec Integration

### When to Create Proposals
- Adding new database plugin support
- Adding new plugin capabilities (QUERY, DDL, METADATA, etc.)
- Changing plugin architecture or SPI mechanism
- Breaking changes to Plugin API
- Performance optimizations that change behavior

### When to Skip Proposals
- Bug fixes in existing plugins
- Dependency updates (non-breaking)
- Documentation updates
- Test additions for existing functionality
- Code formatting and comments
