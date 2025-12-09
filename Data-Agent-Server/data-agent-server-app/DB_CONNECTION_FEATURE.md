# 数据库连接管理功能

## 功能概述

数据库连接管理功能允许用户在系统中配置和管理多个数据库连接，支持 MySQL、PostgreSQL、Oracle、SQL Server 等多种数据库类型。

## 已实现的功能

### 1. 核心功能

- ✅ **连接管理 CRUD**
  - 创建数据库连接配置
  - 查询连接列表
  - 查询单个连接详情
  - 更新连接配置
  - 删除连接配置

- ✅ **连接测试**
  - 测试数据库连接是否可用
  - 验证连接参数的正确性

- ✅ **密码加密**
  - 使用 AES 加密存储密码
  - 自动加密/解密密码

- ✅ **权限控制**
  - 用户只能访问自己创建的连接
  - 基于 Sa-Token 的身份验证

- ✅ **数据隔离**
  - 每个用户的连接数据完全隔离
  - 连接名称全局唯一

### 2. 支持的数据库类型

- MySQL
- PostgreSQL
- Oracle
- SQL Server
- Redis（预留）

## API 接口

### 基础路径
```
/api/connections
```

### 接口列表

| 方法 | 路径 | 说明 | 需要登录 |
|------|------|------|----------|
| GET | `/api/connections` | 获取连接列表 | ✅ |
| GET | `/api/connections/{id}` | 获取连接详情 | ✅ |
| POST | `/api/connections` | 创建连接 | ✅ |
| PUT | `/api/connections/{id}` | 更新连接 | ✅ |
| DELETE | `/api/connections/{id}` | 删除连接 | ✅ |
| POST | `/api/connections/test` | 测试连接 | ✅ |
| GET | `/api/connections/types` | 获取支持的数据库类型 | ✅ |

## 请求示例

### 1. 创建连接

```bash
POST /api/connections
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "我的MySQL数据库",
  "dbType": "mysql",
  "host": "localhost",
  "port": 3306,
  "database": "test_db",
  "username": "root",
  "password": "password123",
  "driverJarPath": "/path/to/mysql-connector.jar",
  "timeout": 30,
  "properties": "{\"useSSL\":\"false\"}"
}
```

### 2. 测试连接

```bash
POST /api/connections/test
Content-Type: application/json
Authorization: Bearer {token}

{
  "dbType": "mysql",
  "host": "localhost",
  "port": 3306,
  "database": "test_db",
  "username": "root",
  "password": "password123",
  "driverJarPath": "/path/to/mysql-connector.jar",
  "timeout": 30
}
```

### 3. 获取连接列表

```bash
GET /api/connections
Authorization: Bearer {token}
```

响应：
```json
[
  {
    "id": 1,
    "name": "我的MySQL数据库",
    "dbType": "mysql",
    "host": "localhost",
    "port": 3306,
    "database": "test_db",
    "username": "root",
    "password": "******",
    "driverJarPath": "/path/to/mysql-connector.jar",
    "timeout": 30,
    "properties": "{\"useSSL\":\"false\"}",
    "userId": 123,
    "createdAt": "2025-11-25T10:00:00",
    "updatedAt": "2025-11-25T10:00:00"
  }
]
```

## 数据库表结构

```sql
CREATE TABLE db_connections (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    db_type         VARCHAR(20)  NOT NULL,
    host            VARCHAR(255) NOT NULL,
    port            INTEGER      NOT NULL,
    database        VARCHAR(100),
    username        VARCHAR(100),
    password        VARCHAR(255),  -- AES 加密存储
    driver_jar_path VARCHAR(500) NOT NULL,
    timeout         INTEGER   DEFAULT 30,
    properties      TEXT      DEFAULT '',
    user_id         BIGINT       NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_db_connections_type ON db_connections(db_type);
CREATE INDEX idx_db_connections_host_port ON db_connections(host, port);
CREATE INDEX idx_db_connections_user_id ON db_connections(user_id);
```

## 安全特性

### 1. 密码加密
- 使用 AES-128 加密算法
- 密码在存储前自动加密
- 密码在使用时自动解密
- API 响应中密码始终显示为 `******`

### 2. 权限控制
- 所有接口都需要登录
- 用户只能访问自己创建的连接
- 尝试访问其他用户的连接会返回 403 错误

### 3. 数据隔离
- 每个用户的连接数据完全隔离
- 连接名称全局唯一（所有用户共享同一命名空间）

## 配置说明

### application.yml 配置

```yaml
app:
  security:
    # 密码加密密钥（生产环境请使用环境变量）
    encryption-key: ${ENCRYPTION_KEY:data-agent-secret-key-for-password-encryption}
```

**重要提示**：生产环境中，请通过环境变量设置加密密钥，不要硬编码在配置文件中。

## 使用流程

1. **用户登录** → 获取访问令牌
2. **创建连接** → 配置数据库连接信息
3. **测试连接** → 验证连接是否可用
4. **保存连接** → 连接配置保存到数据库
5. **使用连接** → 在其他功能中选择并使用已保存的连接

## 后续扩展

### 建议的功能增强

1. **连接池管理**
   - 动态创建和管理数据源
   - 连接池缓存和复用
   - 连接池监控

2. **连接分组**
   - 按项目/环境分组
   - 支持标签管理

3. **连接共享**
   - 团队内共享连接配置
   - 权限管理（只读/读写）

4. **连接监控**
   - 连接状态监控
   - 使用统计
   - 最后使用时间

5. **批量操作**
   - 批量导入连接
   - 批量导出连接
   - 连接克隆

6. **审计日志**
   - 记录连接的创建/修改/删除操作
   - 记录连接的使用情况

## 文件清单

### 实体类
- `DbConnection.java` - 数据库连接实体

### DTO
- `CreateConnectionRequest.java` - 创建连接请求
- `UpdateConnectionRequest.java` - 更新连接请求
- `TestConnectionRequest.java` - 测试连接请求
- `DbConnectionResponse.java` - 连接响应

### 工具类
- `PasswordEncryptor.java` - 密码加密工具

### Mapper
- `DbConnectionMapper.java` - Mapper 接口
- `DbConnectionMapper.xml` - MyBatis XML 映射

### Service
- `DbConnectionService.java` - Service 接口
- `DbConnectionServiceImpl.java` - Service 实现

### Controller
- `DbConnectionController.java` - REST API 控制器

### 数据库
- `00001connection.sql` - 数据库表结构

## 测试建议

### 单元测试
```java
@SpringBootTest
class DbConnectionServiceTest {
    // 测试创建连接
    // 测试更新连接
    // 测试删除连接
    // 测试权限控制
    // 测试密码加密
}
```

### 集成测试
```java
@SpringBootTest
@AutoConfigureMockMvc
class DbConnectionControllerTest {
    // 测试 API 接口
    // 测试身份验证
    // 测试数据隔离
}
```

## 常见问题

### Q: 如何更改加密密钥？
A: 在 `application.yml` 中配置或通过环境变量 `ENCRYPTION_KEY` 设置。注意：更改密钥后，已加密的密码将无法解密。

### Q: 支持哪些数据库类型？
A: 目前支持 MySQL、PostgreSQL、Oracle、SQL Server。可以通过 `/api/connections/types` 接口查询。

### Q: 密码是如何存储的？
A: 密码使用 AES-128 加密后存储在数据库中，API 响应中始终显示为 `******`。

### Q: 如何测试连接？
A: 使用 `/api/connections/test` 接口，提供连接参数即可测试，无需先保存连接。

## 注意事项

1. **生产环境安全**
   - 务必使用环境变量设置加密密钥
   - 定期更换加密密钥
   - 启用 HTTPS

2. **数据库驱动**
   - 需要提供 JDBC 驱动 JAR 文件路径
   - 确保驱动文件可访问

3. **连接超时**
   - 默认超时时间为 30 秒
   - 可根据实际情况调整

4. **并发控制**
   - 连接名称唯一性由数据库约束保证
   - 支持并发创建和更新操作
