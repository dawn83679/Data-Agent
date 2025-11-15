# 错误码迁移总结

## 📋 变更概述

采用**枚举方式**管理错误码,替代原有的常量类方式。

---

## 🔄 变更内容

### 删除的文件
- ❌ `constant/ErrorCodeConstant.java`
- ❌ `constant/ErrorMessageConstant.java`

### 新增的文件
- ✅ `common/ErrorCode.java`

### 修改的文件
- ✅ `exception/BusinessException.java`
- ✅ `model/dto/response/ApiResponse.java`
- ✅ `exception/handler/GlobalExceptionHandler.java`

---

## 📊 使用对比

### 迁移前
```java
throw new BusinessException(
    ErrorCodeConstant.DB_CONNECTION_FAILED,
    ErrorMessageConstant.DB_CONNECTION_FAILED
);
```

### 迁移后
```java
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR);
```

---

## ✨ 核心优势

| 特性 | 迁移前 | 迁移后 |
|------|--------|--------|
| 文件数量 | 2 个 | 1 个 |
| 使用参数 | 2 个 | 1 个 |
| 类型安全 | ❌ 可能错配 | ✅ 编译时检查 |
| 维护成本 | 高 | 低 |

---

## 🚀 使用方式

### 抛出异常
```java
// 使用默认消息
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR);

// 自定义消息
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR, "连接 MySQL 失败");

// 带异常原因
throw new BusinessException(ErrorCode.DB_CONNECTION_ERROR, "连接超时", e);
```

### 返回响应
```java
// 成功
return ApiResponse.success(data);

// 失败
return ApiResponse.error(ErrorCode.PARAMS_ERROR);

// 失败 + 自定义消息
return ApiResponse.error(ErrorCode.PARAMS_ERROR, "用户名不能为空");
```

---

## 📝 错误码变化

### 成功码
- 旧: `200` → 新: `0`

### 客户端错误
- 旧: `400-499` → 新: `40000-49999`

### 服务端错误
- 旧: `500-599, 2000-2599` → 新: `50000-59999`

### 主要映射

| 旧码 | 新枚举 | 新码 |
|------|--------|------|
| 200 | SUCCESS | 0 |
| 400 | PARAMS_ERROR | 40000 |
| 500 | SYSTEM_ERROR | 50000 |
| 2000 | DB_CONNECTION_ERROR | 50100 |
| 2100 | DRIVER_NOT_FOUND | 50200 |
| 2200 | SQL_SYNTAX_ERROR | 50300 |
| 2300 | PLUGIN_NOT_FOUND | 50400 |
| 2400 | FILE_NOT_FOUND | 50500 |
| 2500 | VALIDATION_ERROR | 50600 |

---

## ⚠️ 注意事项

### 响应格式变化
```json
// 旧格式
{"code": 200, "message": "操作成功", "data": {...}}

// 新格式
{"code": 0, "message": "ok", "data": {...}}
```

### 前端适配
```javascript
// 旧代码
if (response.code === 200) { /* 成功 */ }

// 新代码
if (response.code === 0) { /* 成功 */ }
```

### 兼容性
保留了直接传错误码的构造函数:
```java
throw new BusinessException(50100, "数据库连接失败");  // 仍可用,但不推荐
```

---

## 📚 参考文档

- [异常处理使用指南](./exception-handling-guide.md)
