# 错误码使用指南

## 📋 错误码设计

项目使用**枚举方式**管理错误码,错误码和错误消息绑定在一起。

---

## 🎯 错误码规范

### 错误码范围

```
0           - 成功
40000-49999 - 客户端错误
50000-59999 - 服务端错误
```

### 具体分类

| 范围 | 分类 | 说明 |
|------|------|------|
| 0 | 成功 | 操作成功 |
| 40000-40099 | 通用客户端错误 | 参数错误、未登录、无权限等 |
| 40400-40499 | 资源错误 | 资源不存在、禁止访问等 |
| 50000-50099 | 通用服务端错误 | 系统异常、操作失败等 |
| 50100-50199 | 数据库连接 | 连接失败、超时、配置错误等 |
| 50200-50299 | 驱动管理 | 驱动加载、下载、版本问题等 |
| 50300-50399 | SQL 执行 | SQL 语法错误、执行失败、超时等 |
| 50400-50499 | 插件管理 | 插件加载、初始化、功能支持等 |
| 50500-50599 | 文件操作 | 文件读写、格式、大小等 |
| 50600-50699 | 数据验证 | 字段验证、格式检查等 |

---

## 📝 错误码列表

### 通用错误码

| 枚举名 | 错误码 | 错误消息 |
|--------|--------|---------|
| SUCCESS | 0 | ok |
| PARAMS_ERROR | 40000 | 请求参数错误 |
| NOT_LOGIN_ERROR | 40100 | 未登录 |
| NO_AUTH_ERROR | 40101 | 无权限 |
| FORBIDDEN_ERROR | 40300 | 禁止访问 |
| NOT_FOUND_ERROR | 40400 | 请求数据不存在 |
| SYSTEM_ERROR | 50000 | 系统内部异常 |
| OPERATION_ERROR | 50001 | 操作失败 |

### 数据库连接相关 (50100-50199)

| 枚举名 | 错误码 | 错误消息 |
|--------|--------|---------|
| DB_CONNECTION_ERROR | 50100 | 数据库连接失败 |
| DB_CONNECTION_TIMEOUT | 50101 | 数据库连接超时 |
| DB_CONNECTION_NOT_FOUND | 50102 | 数据库连接不存在 |
| DB_CONNECTION_ALREADY_EXISTS | 50103 | 数据库连接已存在 |
| DB_CONNECTION_CONFIG_ERROR | 50104 | 数据库连接配置错误 |

### 驱动相关 (50200-50299)

| 枚举名 | 错误码 | 错误消息 |
|--------|--------|---------|
| DRIVER_NOT_FOUND | 50200 | 驱动文件不存在 |
| DRIVER_LOAD_ERROR | 50201 | 驱动加载失败 |
| DRIVER_DOWNLOAD_ERROR | 50202 | 驱动下载失败 |
| DRIVER_VERSION_NOT_SUPPORTED | 50203 | 驱动版本不支持 |
| DRIVER_FILE_CORRUPTED | 50204 | 驱动文件损坏 |

### SQL 执行相关 (50300-50399)

| 枚举名 | 错误码 | 错误消息 |
|--------|--------|---------|
| SQL_SYNTAX_ERROR | 50300 | SQL 语法错误 |
| SQL_EXECUTION_ERROR | 50301 | SQL 执行失败 |
| SQL_TIMEOUT_ERROR | 50302 | SQL 执行超时 |
| TRANSACTION_COMMIT_ERROR | 50303 | 事务提交失败 |
| TRANSACTION_ROLLBACK_ERROR | 50304 | 事务回滚失败 |

### 插件相关 (50400-50499)

| 枚举名 | 错误码 | 错误消息 |
|--------|--------|---------|
| PLUGIN_NOT_FOUND | 50400 | 插件不存在 |
| PLUGIN_LOAD_ERROR | 50401 | 插件加载失败 |
| PLUGIN_NOT_SUPPORT | 50402 | 插件不支持该功能 |
| PLUGIN_INIT_ERROR | 50403 | 插件初始化失败 |

### 文件操作相关 (50500-50599)

| 枚举名 | 错误码 | 错误消息 |
|--------|--------|---------|
| FILE_NOT_FOUND | 50500 | 文件不存在 |
| FILE_READ_ERROR | 50501 | 文件读取失败 |
| FILE_WRITE_ERROR | 50502 | 文件写入失败 |
| FILE_DELETE_ERROR | 50503 | 文件删除失败 |
| FILE_FORMAT_NOT_SUPPORTED | 50504 | 文件格式不支持 |
| FILE_SIZE_EXCEEDED | 50505 | 文件大小超出限制 |

### 数据验证相关 (50600-50699)

| 枚举名 | 错误码 | 错误消息 |
|--------|--------|---------|
| VALIDATION_ERROR | 50600 | 数据验证失败 |
| REQUIRED_FIELD_EMPTY | 50601 | 必填字段为空 |
| FIELD_FORMAT_ERROR | 50602 | 字段格式错误 |
| FIELD_LENGTH_EXCEEDED | 50603 | 字段长度超出限制 |
| FIELD_VALUE_OUT_OF_RANGE | 50604 | 字段值超出范围 |

---

## 🚀 使用方式

### 1. 抛出异常

```java
import edu.zsc.ai.common.ErrorCode;
import edu.zsc.ai.exception.BusinessException;

// 使用默认消息
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR);

// 自定义消息
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR, "连接 MySQL 8.0 失败");

// 带异常原因
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR, "连接超时", e);
```

### 2. 返回响应

```java
import edu.zsc.ai.common.ErrorCode;
import edu.zsc.ai.model.dto.response.ApiResponse;

// 成功响应
return ApiResponse.success(data);

// 错误响应 (使用默认消息)
return ApiResponse.error(ErrorCode.PARAMS_ERROR);

// 错误响应 (自定义消息)
return ApiResponse.error(ErrorCode.PARAMS_ERROR, "用户名不能为空");
```

---

## 📊 响应格式

### 成功响应

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

### 错误响应

```json
{
  "code": 50100,
  "message": "数据库连接失败",
  "data": null
}
```

---

## 🔧 扩展错误码

需要添加新错误码时,在 `ErrorCode.java` 枚举中添加:

```java
@Getter
public enum ErrorCode {
    // ... 现有错误码
    
    // 新增错误码 (选择合适的范围)
    EXPORT_ERROR(50700, "数据导出失败"),
    IMPORT_ERROR(50701, "数据导入失败");
    
    // ... 其他代码
}
```

**注意事项:**
- 选择合适的错误码范围
- 错误码不要重复
- 错误消息要清晰明确
- 遵循命名规范 (大写下划线)

---

## 📚 参考文档

- [异常处理使用指南](./exception-handling-guide.md)
- 错误码枚举定义: `common/ErrorCode.java`
