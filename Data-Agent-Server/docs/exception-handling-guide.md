# 异常处理使用指南

## 📋 异常处理体系概览

Data-Agent 项目采用统一的异常处理体系,使用枚举方式管理错误码,包含以下核心组件:

### 1. 核心组件

| 组件 | 位置 | 说明 |
|------|------|------|
| `ErrorCode` | `common/ErrorCode.java` | 错误码枚举（码和消息绑定） |
| `BusinessException` | `exception/BusinessException.java` | 业务异常类 |
| `GlobalExceptionHandler` | `exception/handler/GlobalExceptionHandler.java` | 全局异常处理器 |
| `ApiResponse<T>` | `model/dto/response/ApiResponse.java` | 统一响应格式 |

### 2. 错误码规范

```
0           - 成功
40000       - 请求参数错误
40100       - 未登录
40101       - 无权限
40300       - 禁止访问
40400       - 请求数据不存在
50000       - 系统内部异常
50001       - 操作失败

具体分类:
50100-50199 - 数据库连接相关
50200-50299 - 驱动相关
50300-50399 - SQL 执行相关
50400-50499 - 插件相关
50500-50599 - 文件操作相关
50600-50699 - 数据验证相关
```

## 🚀 使用方法

### 1. 在 Service 层抛出业务异常

#### 方式一: 使用错误码枚举（推荐）

```java
import edu.zsc.ai.common.ErrorCode;
import edu.zsc.ai.exception.BusinessException;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    
    @Override
    public Connection connect(Long connectionId) {
        DbConnection dbConnection = dbConnectionMapper.selectById(connectionId);
        if (dbConnection == null) {
            // ✅ 推荐：使用错误码枚举
            throw new BusinessException(ErrorCode.DB_CONNECTION_NOT_FOUND);
        }
        
        try {
            return createConnection(dbConnection);
        } catch (SQLException e) {
            // ✅ 推荐：带原因的异常
            throw new BusinessException(
                ErrorCode.DB_CONNECTION_ERROR,
                "数据库连接失败: " + e.getMessage(),
                e
            );
        }
    }
}
```

#### 方式二: 使用错误码枚举 + 自定义消息

```java
@Service
public class DriverServiceImpl implements DriverService {
    
    @Override
    public void downloadDriver(String driverName, String version) {
        if (!isSupportedVersion(version)) {
            // 自定义详细的错误消息
            throw new BusinessException(
                ErrorCode.DRIVER_VERSION_NOT_SUPPORTED,
                String.format("驱动 %s 不支持版本 %s", driverName, version)
            );
        }
    }
}
```

#### 方式三: 直接使用错误码和消息（兼容旧代码）

```java
@Service
public class FileServiceImpl implements FileService {
    
    @Override
    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            // ⚠️ 不推荐：应该使用错误码枚举
            throw new BusinessException(50500, "文件不存在: " + filePath);
        }
    }
}
```

### 2. 在 Controller 层处理响应

Controller 层不需要手动捕获异常,`GlobalExceptionHandler` 会自动处理:

```java
@RestController
@RequestMapping("/api/connections")
public class ConnectionController {
    
    @Autowired
    private ConnectionService connectionService;
    
    /**
     * 测试数据库连接
     * 
     * @param request 连接请求
     * @return 测试结果
     */
    @PostMapping("/test")
    public ApiResponse<ConnectionTestResponse> testConnection(
            @RequestBody @Valid ConnectRequest request) {
        // Service 层抛出的异常会被 GlobalExceptionHandler 自动捕获
        ConnectionTestResponse response = connectionService.testConnection(request);
        return ApiResponse.success(response);
    }
}
```

### 3. 响应格式

#### 成功响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "name": "MySQL Connection"
  }
}
```

#### 错误响应

```json
{
  "code": 50100,
  "message": "数据库连接失败",
  "data": null
}
```

## 📝 常见场景示例

### 场景 1: 资源不存在

```java
@Service
public class ConnectionServiceImpl implements ConnectionService {
    
    @Override
    public ConnectionResponse getConnection(Long id) {
        DbConnection connection = dbConnectionMapper.selectById(id);
        if (connection == null) {
            throw new BusinessException(ErrorCode.DB_CONNECTION_NOT_FOUND);
        }
        return convertToResponse(connection);
    }
}
```

### 场景 2: 参数验证失败

```java
@Service
public class DriverServiceImpl implements DriverService {
    
    @Override
    public void uploadDriver(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_EMPTY);
        }
        
        if (file.getSize() > 100 * 1024 * 1024) {  // 100MB
            throw new BusinessException(
                ErrorCode.FILE_SIZE_EXCEEDED,
                "文件大小不能超过 100MB"
            );
        }
    }
}
```

### 场景 3: 数据库操作失败

```java
@Service
public class ConnectionServiceImpl implements ConnectionService {
    
    @Override
    public void saveConnection(ConnectionCreateRequest request) {
        try {
            DbConnection connection = convertToEntity(request);
            dbConnectionMapper.insert(connection);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(
                ErrorCode.DB_CONNECTION_ALREADY_EXISTS,
                "连接名称已存在",
                e
            );
        }
    }
}
```

### 场景 4: 外部服务调用失败

```java
@Service
public class DriverServiceImpl implements DriverService {
    
    @Override
    public void downloadDriver(DownloadDriverRequest request) {
        try {
            String url = buildDownloadUrl(request);
            downloadFile(url, request.getTargetPath());
        } catch (IOException e) {
            throw new BusinessException(
                ErrorCode.DRIVER_DOWNLOAD_ERROR,
                "驱动下载失败: " + e.getMessage(),
                e
            );
        }
    }
}
```

### 场景 5: SQL 执行失败

```java
@Service
public class SqlExecutorService {
    
    @Override
    public SqlCommandResult executeCommand(SqlCommandRequest request) {
        try {
            return executor.executeCommand(request);
        } catch (SQLException e) {
            // 根据 SQL 错误码判断具体错误类型
            if (e.getErrorCode() == 1064) {  // MySQL 语法错误
                throw new BusinessException(
                    ErrorCode.SQL_SYNTAX_ERROR,
                    "SQL 语法错误: " + e.getMessage(),
                    e
                );
            } else {
                throw new BusinessException(
                    ErrorCode.SQL_EXECUTION_ERROR,
                    "SQL 执行失败: " + e.getMessage(),
                    e
                );
            }
        }
    }
}
```

## 🎯 最佳实践

### 1. 使用错误码枚举,避免魔法值

```java
// ✅ 推荐：使用错误码枚举
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR);

// ❌ 不推荐：使用魔法值
throw new BusinessException(50100, "数据库连接失败");
```

### 2. 提供详细的错误信息

```java
// ✅ 推荐：包含具体信息
throw new BusinessException(
    ErrorCode.DRIVER_NOT_FOUND,
    String.format("驱动文件不存在: %s", driverPath)
);

// ⚠️ 可以接受：使用默认消息
throw new BusinessException(ErrorCode.DRIVER_NOT_FOUND);
```

### 3. 保留原始异常信息

```java
// ✅ 推荐：保留原始异常
try {
    // ...
} catch (SQLException e) {
    throw new BusinessException(
        ErrorCode.DB_CONNECTION_ERROR,
        "数据库连接失败: " + e.getMessage(),
        e  // 保留原始异常
    );
}

// ❌ 不推荐：丢失原始异常
try {
    // ...
} catch (SQLException e) {
    throw new BusinessException(
        ErrorCode.DB_CONNECTION_ERROR,
        e.getMessage()  // 丢失了堆栈信息
    );
}
```

### 4. 在合适的层级抛出异常

```java
// ✅ 推荐：在 Service 层抛出业务异常
@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Override
    public void deleteConnection(Long id) {
        DbConnection connection = dbConnectionMapper.selectById(id);
        if (connection == null) {
            throw new BusinessException(ErrorCode.DB_CONNECTION_NOT_FOUND);
        }
        dbConnectionMapper.deleteById(id);
    }
}

// ❌ 不推荐：在 Controller 层处理业务逻辑
@RestController
public class ConnectionController {
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConnection(@PathVariable Long id) {
        DbConnection connection = dbConnectionMapper.selectById(id);
        if (connection == null) {
            return ApiResponse.error(ErrorCode.DB_CONNECTION_NOT_FOUND);
        }
        dbConnectionMapper.deleteById(id);
        return ApiResponse.success();
    }
}
```

### 5. 不要过度捕获异常

```java
// ✅ 推荐：让异常向上传播
@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Override
    public Connection connect(Long id) {
        DbConnection dbConnection = getConnection(id);
        return createConnection(dbConnection);
        // 不需要 try-catch,让 GlobalExceptionHandler 处理
    }
}

// ❌ 不推荐：过度捕获
@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Override
    public Connection connect(Long id) {
        try {
            DbConnection dbConnection = getConnection(id);
            return createConnection(dbConnection);
        } catch (Exception e) {
            // 捕获所有异常并重新包装,没有必要
            throw new BusinessException(500, e.getMessage());
        }
    }
}
```

## 🔧 扩展错误码

当需要添加新的错误码时,在 `ErrorCode` 枚举中添加:

```java
// ErrorCode.java
@Getter
public enum ErrorCode {
    // ... 现有错误码
    
    // ==================== 新模块错误码 (50700-50799) ====================
    
    /**
     * 导出失败
     */
    EXPORT_ERROR(50700, "数据导出失败"),
    
    /**
     * 导入失败
     */
    IMPORT_ERROR(50701, "数据导入失败");
    
    // ... 其他代码
}
```

**优点:**
- ✅ 只需在一个地方添加
- ✅ 错误码和消息自动绑定
- ✅ 不会出现错配问题

## 📚 参考资料

- [Java 代码设计规范](./java-design-guidelines.md)
- [Spring Boot 异常处理最佳实践](https://spring.io/guides/tutorials/rest/)
- [HTTP 状态码规范](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Status)
